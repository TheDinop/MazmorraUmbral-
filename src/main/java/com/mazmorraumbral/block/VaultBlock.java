package com.mazmorraumbral.block;

import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.state.UmbralSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bóveda del Umbral. Se abre una sola vez por jugador y expedición: exige haber recibido la recompensa del jefe y
 * consume una Llave del Umbral. El estado se guarda ANTES de entregar nada, así que un fallo o una desconexión a
 * mitad nunca duplica el premio (como mucho, lo pierde).
 *
 * Contenido de la recompensa: PROPUESTA mía, la especificación solo dice "crates, llaves y recompensas finales
 * según progreso". Está sin balancear.
 */
public class VaultBlock extends Block {

    public VaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer jugador)) {
            return InteractionResult.PASS;
        }
        UmbralSavedData datos = UmbralSavedData.get(jugador.server);
        UmbralSavedData.Registro registro = datos.registro(jugador.getUUID());

        if (registro.vaultReclamado) {
            jugador.displayClientMessage(Component.translatable("message.mazmorra_umbral.boveda_ya_reclamada"), true);
            return InteractionResult.CONSUME;
        }
        if (!registro.jefeRecompensado) {
            jugador.displayClientMessage(Component.translatable("message.mazmorra_umbral.boveda_sin_jefe"), true);
            return InteractionResult.CONSUME;
        }
        if (!consumirLlave(jugador)) {
            jugador.displayClientMessage(Component.translatable("message.mazmorra_umbral.boveda_sin_llave"), true);
            return InteractionResult.CONSUME;
        }

        // Estado primero, premios después.
        registro.vaultReclamado = true;
        registro.victorias++;
        datos.solicitarReinicio();
        datos.setDirty();

        entregar(jugador, new ItemStack(ModItems.CRATE_EPICA.get(), 1));
        entregar(jugador, new ItemStack(ModItems.CRATE_RARA.get(), 2));
        entregar(jugador, new ItemStack(ModItems.CRATE_XP.get(), 1));
        if (level.getRandom().nextFloat() < 0.35F) {
            entregar(jugador, new ItemStack(ModItems.CRATE_LEGENDARIA.get(), 1));
        }
        if (level.getRandom().nextFloat() < 0.05F) {
            entregar(jugador, new ItemStack(ModItems.CRATE_MITICA.get(), 1));
        }
        // Fuente de fragmentos posteriores: principalmente la mazmorra.
        entregar(jugador, new ItemStack(ModItems.FRAGMENTO_DEL_UMBRAL.get(), 1));

        jugador.displayClientMessage(Component.translatable("message.mazmorra_umbral.boveda_abierta"), false);
        if (UmbralConfig.LOG_DEPURACION.get()) {
            MazmorraUmbral.LOGGER.info("{} reclamó la bóveda (victoria {})", jugador.getGameProfile().getName(),
                    registro.victorias);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean consumirLlave(ServerPlayer jugador) {
        Inventory inv = jugador.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack pila = inv.getItem(i);
            if (pila.is(ModItems.LLAVE_UMBRAL.get())) {
                pila.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void entregar(ServerPlayer jugador, ItemStack pila) {
        if (!jugador.getInventory().add(pila)) {
            jugador.drop(pila, false);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
