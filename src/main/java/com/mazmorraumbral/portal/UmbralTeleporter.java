package com.mazmorraumbral.portal;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.registry.ModDimensions;
import com.mazmorraumbral.state.UmbralSavedData;
import com.mazmorraumbral.world.FloorBuilder;
import com.mazmorraumbral.world.UmbralLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Entrada, salida y cambio de piso. La muerte, el cierre del juego o un fallo no tocan el registro del jugador:
 * solo se escribe al entrar (posición de retorno y portal de origen) y al subir de piso.
 */
public final class UmbralTeleporter {

    private UmbralTeleporter() {
    }

    /** Se difiere al final del tick para no cambiar de dimensión en mitad de las colisiones. */
    public static void programar(ServerPlayer player) {
        player.setPortalCooldown();
        player.server.execute(() -> {
            if (player.isRemoved() || player.hasDisconnected()) {
                return;
            }
            ejecutar(player);
        });
    }

    private static void ejecutar(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel origen = player.serverLevel();
        UmbralSavedData data = UmbralSavedData.get(server);
        UmbralSavedData.Registro registro = data.registro(player.getUUID());

        if (origen.dimension().equals(ModDimensions.UMBRAL)) {
            salir(player, server, data, registro);
        } else {
            entrar(player, server, origen, data, registro);
        }
    }

    private static void entrar(ServerPlayer player, MinecraftServer server, ServerLevel origen, UmbralSavedData data,
                               UmbralSavedData.Registro registro) {
        ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL);
        if (umbral == null) {
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.sin_dimension"), false);
            MazmorraUmbral.LOGGER.error("La dimensión {} no está cargada; revisa el datapack del mod.",
                    ModDimensions.UMBRAL.location());
            return;
        }
        int piso = Math.max(1, Math.min(UmbralLayout.MAX_PISO, registro.pisoActual));
        FloorBuilder.asegurar(umbral, data, piso);

        String dimensionOrigen = origen.dimension().location().toString();
        registro.guardarRetorno(dimensionOrigen, player.blockPosition());
        registro.guardarPortal(dimensionOrigen, player.blockPosition());
        registro.expedicionIniciada = true;
        data.setDirty();

        mover(player, umbral, UmbralLayout.llegada(piso), 0.0F);
        anunciarPiso(player, piso);
        if (UmbralConfig.LOG_DEPURACION.get()) {
            MazmorraUmbral.LOGGER.info("{} entra al Umbral (piso {})", player.getGameProfile().getName(), piso);
        }
    }

    private static void salir(ServerPlayer player, MinecraftServer server, UmbralSavedData data,
                              UmbralSavedData.Registro registro) {
        ServerLevel destino = server.overworld();
        BlockPos pos = destino.getSharedSpawnPos();
        if (registro.retornoDimension != null) {
            ResourceLocation loc = ResourceLocation.tryParse(registro.retornoDimension);
            ServerLevel guardado = loc == null ? null
                    : server.getLevel(ResourceKey.create(Registries.DIMENSION, loc));
            if (guardado != null) {
                destino = guardado;
                pos = registro.posicionRetorno();
            }
        }
        mover(player, destino, Vec3.atBottomCenterOf(pos), player.getYRot());
        if (UmbralConfig.LOG_DEPURACION.get()) {
            MazmorraUmbral.LOGGER.info("{} sale del Umbral hacia {}", player.getGameProfile().getName(),
                    destino.dimension().location());
        }
        if (registro.vaultReclamado) {
            colapsarPortal(server, registro);
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.umbral_del_vacio"), false);
        }
    }

    /** Cambia de piso dentro de la dimensión: solo el bloque de ascenso (que solo existe tras limpiar el piso). */
    public static void subirDePiso(ServerPlayer player, BlockPos bloque) {
        ServerLevel nivel = player.serverLevel();
        if (!nivel.dimension().equals(ModDimensions.UMBRAL)) {
            return;
        }
        int piso = UmbralLayout.pisoDe(bloque);
        if (piso < 1 || piso >= UmbralLayout.MAX_PISO) {
            return;
        }
        int siguiente = piso + 1;
        UmbralSavedData data = UmbralSavedData.get(player.server);
        FloorBuilder.asegurar(nivel, data, siguiente);

        UmbralSavedData.Registro registro = data.registro(player.getUUID());
        if (siguiente > registro.pisoActual) {
            registro.pisoActual = siguiente;
            data.setDirty();
        }
        Vec3 llegada = UmbralLayout.llegada(siguiente);
        player.teleportTo(nivel, llegada.x, llegada.y, llegada.z, 0.0F, 0.0F);
        player.setPortalCooldown();
        anunciarPiso(player, siguiente);
    }

    private static void anunciarPiso(ServerPlayer player, int piso) {
        player.displayClientMessage(Component.translatable("message.mazmorra_umbral.entras_piso", piso,
                Component.translatable("piso.mazmorra_umbral." + piso)), true);
        if (piso == 1) {
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.tutorial"), false);
        }
    }

    private static void mover(ServerPlayer player, ServerLevel destino, Vec3 posicion, float yRot) {
        player.changeDimension(destino, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                            Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(posicion, Vec3.ZERO, yRot, entity.getXRot());
            }
        });
        player.setPortalCooldown();
    }

    /**
     * Umbral del Vacío: la energía del portal por el que el jugador entró colapsa (los bloques de portal se
     * eliminan); el marco de Piedra del Umbral permanece. Para volver a entrar hace falta otro Fragmento.
     */
    private static void colapsarPortal(MinecraftServer server, UmbralSavedData.Registro registro) {
        if (registro.portalDimension == null) {
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(registro.portalDimension);
        ServerLevel nivel = loc == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, loc));
        if (nivel == null) {
            return;
        }
        BlockPos base = BlockPos.of(registro.portalPosicion);
        Set<BlockPos> visitados = new HashSet<>();
        Deque<BlockPos> cola = new ArrayDeque<>();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(2, 2, 2))) {
            if (nivel.getBlockState(p).is(ModBlocks.PORTAL_DEL_UMBRAL.get())) {
                BlockPos fijo = p.immutable();
                if (visitados.add(fijo)) {
                    cola.add(fijo);
                }
            }
        }
        int quitados = 0;
        while (!cola.isEmpty() && quitados < 600) {
            BlockPos p = cola.poll();
            nivel.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            quitados++;
            for (Direction d : Direction.values()) {
                BlockPos vecino = p.relative(d);
                if (!visitados.contains(vecino) && nivel.getBlockState(vecino).is(ModBlocks.PORTAL_DEL_UMBRAL.get())) {
                    visitados.add(vecino);
                    cola.add(vecino);
                }
            }
        }
    }
}
