package com.mazmorraumbral.portal;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.registry.ModDimensions;
import com.mazmorraumbral.state.UmbralSavedData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Entrada y salida de la dimensión del Umbral. La muerte, el cierre del juego o un fallo no tocan el
 * registro del jugador: solo se escribe al entrar (posición de retorno).
 */
public final class UmbralTeleporter {
    private static final BlockPos PLATAFORMA = new BlockPos(0, 64, 0);
    private static final Vec3 LLEGADA = new Vec3(0.5D, 65.0D, 2.5D);

    private UmbralTeleporter() {
    }

    /** Se difiere al final del tick para no cambiar de dimensión en mitad de las colisiones. */
    public static void programar(ServerPlayer player) {
        player.setPortalCooldown();
        MinecraftServer server = player.server;
        server.execute(() -> {
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
        } else {
            ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL);
            if (umbral == null) {
                player.displayClientMessage(Component.translatable("message.mazmorra_umbral.sin_dimension"), false);
                MazmorraUmbral.LOGGER.error("La dimensión {} no está cargada; revisa el datapack del mod.",
                        ModDimensions.UMBRAL.location());
                return;
            }
            asegurarPlataforma(umbral);
            registro.guardarRetorno(origen.dimension().location().toString(), player.blockPosition());
            registro.expedicionIniciada = true;
            data.setDirty();
            mover(player, umbral, LLEGADA, 180.0F);
            if (UmbralConfig.LOG_DEPURACION.get()) {
                MazmorraUmbral.LOGGER.info("{} entra al Umbral", player.getGameProfile().getName());
            }
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
     * Plataforma 9x9 con el portal de regreso. Es temporal: los ocho pisos reales llegarán en fases
     * posteriores como estructuras. Solo escribe dentro de la dimensión propia del mod.
     */
    private static void asegurarPlataforma(ServerLevel umbral) {
        if (umbral.getBlockState(PLATAFORMA).is(Blocks.STONE_BRICKS)) {
            return;
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                umbral.setBlock(new BlockPos(x, 64, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
        BlockState piedra = ModBlocks.PIEDRA_DEL_UMBRAL.get().defaultBlockState();
        int z = -3;
        for (int y = 64; y <= 68; y++) {
            umbral.setBlock(new BlockPos(-2, y, z), piedra, 3);
            umbral.setBlock(new BlockPos(1, y, z), piedra, 3);
        }
        for (int x = -1; x <= 0; x++) {
            umbral.setBlock(new BlockPos(x, 64, z), piedra, 3);
            umbral.setBlock(new BlockPos(x, 68, z), piedra, 3);
        }
        BlockState portal = ModBlocks.PORTAL_DEL_UMBRAL.get().defaultBlockState()
                .setValue(UmbralPortalBlock.AXIS, Direction.Axis.X);
        for (int x = -1; x <= 0; x++) {
            for (int y = 65; y <= 67; y++) {
                umbral.setBlock(new BlockPos(x, y, z), portal, 18);
            }
        }
    }
}
