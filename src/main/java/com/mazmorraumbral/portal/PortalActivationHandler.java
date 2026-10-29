package com.mazmorraumbral.portal;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.registry.ModDimensions;
import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.state.UmbralSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class PortalActivationHandler {

    /**
     * Clic derecho con un Fragmento del Umbral sobre un bloque de Piedra del Umbral.
     * El fragmento se consume solo si el marco es válido y el portal se enciende.
     * En creativo no se consume ni se bloquea la receta (decisión de prueba, ver ESTADO.md).
     */
    @SubscribeEvent
    public void alHacerClicDerechoEnBloque(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();

        if (!stack.is(ModItems.FRAGMENTO_DEL_UMBRAL.get())) {
            return;
        }
        if (!level.getBlockState(event.getPos()).is(ModBlocks.PIEDRA_DEL_UMBRAL.get())) {
            return;
        }

        // Interceptamos en ambos lados para que el cliente no intente otra acción; solo el servidor actúa.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (level.dimension().equals(ModDimensions.UMBRAL)) {
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.portal_no_aqui"), true);
            return;
        }

        Optional<PortalFrame> frame = PortalFrame.find(level, event.getPos());
        if (frame.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.portal_invalido"), true);
            return;
        }

        frame.get().activate(level);
        level.playSound(null, event.getPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.2F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            UmbralSavedData.get(player.server).registrarActivacionExitosa(player.getUUID());
        }

        if (UmbralConfig.LOG_DEPURACION.get()) {
            MazmorraUmbral.LOGGER.info("Portal del Umbral activado por {} en {}", player.getGameProfile().getName(),
                    event.getPos());
        }
    }
}
