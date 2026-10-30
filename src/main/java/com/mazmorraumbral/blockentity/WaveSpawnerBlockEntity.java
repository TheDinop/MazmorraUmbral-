package com.mazmorraumbral.blockentity;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.encounter.FloorSpec;
import com.mazmorraumbral.registry.ModBlockEntities;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.entity.CaballeroNetheriteVacio;
import com.mazmorraumbral.entity.UmbralMonster;
import com.mazmorraumbral.state.UmbralSavedData;
import com.mazmorraumbral.util.UmbralTags;
import com.mazmorraumbral.util.XpUtil;
import com.mazmorraumbral.world.UmbralLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Generador de oleadas finitas. Todo el estado vive aquí y se guarda con el chunk:
 *  - estado (inactivo / activo / completo), número de oleada, si la oleada actual ya se lanzó y la expedición a la
 *    que pertenece (si la expedición del mundo cambia, el generador se rearma);
 *  - la pertenencia de un mob al encuentro se guarda en el propio mob (UmbralTags en su NBT), así que sobrevive a
 *    cerrar el juego. La oleada termina cuando no queda ningún mob vivo con esa marca dentro del recinto.
 * El generador solo avanza mientras haya un jugador válido cerca (así los chunks están cargados).
 * La oleada se cuenta como superada y se persiste ANTES de soltar el botín: un fallo entre ambos pasos pierde el
 * premio, nunca lo duplica. Los mobs sin marca (comandos, huevos, otros mods) no cuentan ni dan premios de oleada.
 */
public class WaveSpawnerBlockEntity extends BlockEntity {
    public static final int INACTIVO = 0;
    public static final int ACTIVO = 1;
    public static final int COMPLETO = 2;

    private static final int INTERVALO_TICKS = 10;
    private static final int GRACIA_TRAS_LANZAR = 40;
    private static final int ESPERA_ENTRE_OLEADAS = 60;
    private static final int PISO_JEFE = 7;

    private int piso = 1;
    private int estado = INACTIVO;
    private int oleada = 0;
    private boolean oleadaLanzada = false;
    private int espera = 0;
    private long ultimoLanzamiento = 0L;
    private long expedicion = 0L;
    private BlockPos salida;

    public WaveSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERADOR_OLEADAS.get(), pos, state);
    }

    public void configurar(int piso, BlockPos salida) {
        this.piso = piso;
        this.salida = salida;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("piso", piso);
        tag.putInt("estado", estado);
        tag.putInt("oleada", oleada);
        tag.putBoolean("oleadaLanzada", oleadaLanzada);
        tag.putInt("espera", espera);
        tag.putLong("ultimoLanzamiento", ultimoLanzamiento);
        tag.putLong("expedicion", expedicion);
        if (salida != null) {
            tag.putLong("salida", salida.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        piso = Math.max(1, tag.getInt("piso"));
        estado = tag.getInt("estado");
        oleada = tag.getInt("oleada");
        oleadaLanzada = tag.getBoolean("oleadaLanzada");
        espera = tag.getInt("espera");
        ultimoLanzamiento = tag.getLong("ultimoLanzamiento");
        expedicion = tag.getLong("expedicion");
        salida = tag.contains("salida") ? BlockPos.of(tag.getLong("salida")) : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaveSpawnerBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (server.getGameTime() % INTERVALO_TICKS != 0) {
            return;
        }
        UmbralSavedData datos = UmbralSavedData.get(server.getServer());
        be.sincronizarExpedicion(datos);
        if (be.estado == COMPLETO) {
            return;
        }
        if (be.jugadoresValidos(server).isEmpty()) {
            return;
        }
        be.avanzar(server, datos);
    }

    private void sincronizarExpedicion(UmbralSavedData datos) {
        long actual = datos.getExpedicionId();
        if (expedicion != actual) {
            expedicion = actual;
            estado = INACTIVO;
            oleada = 0;
            oleadaLanzada = false;
            espera = 0;
            setChanged();
        }
    }

    private void avanzar(ServerLevel level, UmbralSavedData datos) {
        List<List<FloorSpec.Entrada>> olas = FloorSpec.olas(piso);
        if (olas.isEmpty()) {
            return;
        }

        if (estado == INACTIVO) {
            estado = ACTIVO;
            oleada = 0;
            oleadaLanzada = false;
            espera = 20;
            setChanged();
        }

        if (!oleadaLanzada) {
            if (espera > 0) {
                espera -= INTERVALO_TICKS;
                setChanged();
                return;
            }
            lanzarOleada(level, olas.get(oleada));
            return;
        }

        if (level.getGameTime() - ultimoLanzamiento < GRACIA_TRAS_LANZAR) {
            return;
        }
        if (contarRestantes(level) > 0) {
            return;
        }

        // Oleada superada: se persiste el avance antes de entregar nada.
        oleada++;
        oleadaLanzada = false;
        espera = ESPERA_ENTRE_OLEADAS;
        boolean ultima = oleada >= olas.size();
        if (ultima) {
            estado = COMPLETO;
            datos.marcarPisoCompletado(piso);
        }
        setChanged();

        entregarRecompensa(level);
        avisar(level, Component.translatable("message.mazmorra_umbral.oleada_superada", oleada, olas.size()));

        if (ultima) {
            if (piso == PISO_JEFE) {
                recompensarJefe(level, datos);
            }
            abrirSalida(level);
        }
    }

    private List<ServerPlayer> jugadoresValidos(ServerLevel level) {
        double radio = UmbralConfig.SPAWNER_RADIO.get();
        Vec3 centro = Vec3.atCenterOf(worldPosition);
        List<ServerPlayer> validos = new ArrayList<>();
        for (ServerPlayer jugador : level.players()) {
            if (!jugador.isAlive() || jugador.isSpectator()) {
                continue;
            }
            if (jugador.isCreative() && UmbralConfig.SOLO_SUPERVIVENCIA.get()) {
                continue;
            }
            if (jugador.distanceToSqr(centro) <= radio * radio) {
                validos.add(jugador);
            }
        }
        return validos;
    }

    private int contarRestantes(ServerLevel level) {
        AABB zona = new AABB(worldPosition).inflate(40.0D, 14.0D, 40.0D);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, zona, mob -> mob.isAlive()
                && UmbralTags.tieneSpawner(mob)
                && UmbralTags.spawnerDe(mob).equals(worldPosition)
                && UmbralTags.expedicionDe(mob) == expedicion);
        return mobs.size();
    }

    private void lanzarOleada(ServerLevel level, List<FloorSpec.Entrada> ola) {
        int jugadores = Math.max(1, jugadoresValidos(level).size());
        double extra = 1.0D + UmbralConfig.MULT_MOBS_POR_JUGADOR.get() * (jugadores - 1);
        int tope = UmbralConfig.MAX_SIMULTANEOS.get();
        int lanzados = 0;
        for (FloorSpec.Entrada entrada : ola) {
            int cantidad = (int) Math.max(1L, Math.round(entrada.cantidad() * extra));
            for (int i = 0; i < cantidad && lanzados < tope; i++) {
                if (lanzarUno(level, entrada, jugadores)) {
                    lanzados++;
                }
            }
        }
        if (lanzados == 0) {
            // Sin sitio válido: se reintenta en el siguiente ciclo y NO se da por lanzada la oleada.
            MazmorraUmbral.LOGGER.warn("Generador {} no encontró sitio para spawnear la oleada {}", worldPosition, oleada);
            return;
        }
        oleadaLanzada = true;
        ultimoLanzamiento = level.getGameTime();
        setChanged();
    }

    private boolean lanzarUno(ServerLevel level, FloorSpec.Entrada entrada, int jugadores) {
        RandomSource azar = level.getRandom();
        for (int intento = 0; intento < 25; intento++) {
            double angulo = azar.nextDouble() * Math.PI * 2.0D;
            double distancia = 5.0D + azar.nextDouble() * 9.0D;
            int x = worldPosition.getX() + (int) Math.round(Math.cos(angulo) * distancia);
            int z = worldPosition.getZ() + (int) Math.round(Math.sin(angulo) * distancia);
            BlockPos pos = new BlockPos(x, worldPosition.getY(), z);

            BlockPos suelo = pos.below();
            if (!level.getBlockState(suelo).isFaceSturdy(level, suelo, Direction.UP)) {
                continue;
            }
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }

            Entity entidad = entrada.tipo().create(level);
            if (!(entidad instanceof Mob mob)) {
                continue;
            }
            mob.moveTo(x + 0.5D, pos.getY(), z + 0.5D, azar.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (mob instanceof UmbralMonster criatura) {
                criatura.setVariant(entrada.varianteCriatura());
            }
            if (mob instanceof CaballeroNetheriteVacio jefe) {
                jefe.escalarParaJugadores(jugadores);
            }
            entrada.variante().aplicar(mob, worldPosition, expedicion);
            level.addFreshEntity(mob);
            return true;
        }
        return false;
    }

    private void entregarRecompensa(ServerLevel level) {
        ResourceLocation id = new ResourceLocation(MazmorraUmbral.MODID, "spawner/piso_" + piso);
        LootTable tabla = level.getServer().getLootData().getLootTable(id);
        LootParams parametros = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .create(LootContextParamSets.CHEST);
        for (ItemStack pila : tabla.getRandomItems(parametros)) {
            Block.popResource(level, worldPosition.above(), pila);
        }
    }

    /**
     * Recompensa única del jefe por jugador y expedición: XP real, Llave del Umbral y Corazón Dorado (probabilidad
     * configurable, provisional). La marca jefeRecompensado se guarda antes de entregar nada.
     */
    private void recompensarJefe(ServerLevel level, UmbralSavedData datos) {
        for (ServerPlayer jugador : level.players()) {
            if (!jugador.isAlive() || jugador.isSpectator() || UmbralLayout.pisoDe(jugador.blockPosition()) != PISO_JEFE) {
                continue;
            }
            UmbralSavedData.Registro registro = datos.registro(jugador.getUUID());
            if (registro.jefeRecompensado) {
                continue;
            }
            registro.jefeRecompensado = true;
            datos.setDirty();

            XpUtil.dar(jugador, UmbralConfig.JEFE_XP.get());
            entregar(jugador, new ItemStack(ModItems.LLAVE_UMBRAL.get()));
            if (level.getRandom().nextDouble() < UmbralConfig.CORAZON_PROBABILIDAD.get()) {
                entregar(jugador, new ItemStack(ModItems.CORAZON_DORADO_UMBRAL.get()));
            }
            jugador.displayClientMessage(Component.translatable("message.mazmorra_umbral.jefe_recompensa"), false);
        }
    }

    private static void entregar(ServerPlayer jugador, ItemStack pila) {
        if (!jugador.getInventory().add(pila)) {
            jugador.drop(pila, false);
        }
    }

    private void abrirSalida(ServerLevel level) {
        if (piso < UmbralLayout.MAX_PISO && salida != null) {
            for (BlockPos celda : UmbralLayout.celdasSalida(salida)) {
                level.setBlock(celda, ModBlocks.ASCENSO_DEL_UMBRAL.get().defaultBlockState(), 3);
            }
        }
        level.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
        avisar(level, Component.translatable("message.mazmorra_umbral.piso_superado"));
    }

    private void avisar(ServerLevel level, Component mensaje) {
        double radio = UmbralConfig.SPAWNER_RADIO.get() + 10.0D;
        Vec3 centro = Vec3.atCenterOf(worldPosition);
        for (ServerPlayer jugador : level.players()) {
            if (jugador.distanceToSqr(centro) <= radio * radio) {
                jugador.displayClientMessage(mensaje, true);
            }
        }
    }
}
