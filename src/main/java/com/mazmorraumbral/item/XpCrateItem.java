package com.mazmorraumbral.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Crate de XP: una sola identidad de objeto. Entrega 8-16 botellas de experiencia (propuesta, sin balancear). */
public class XpCrateItem extends Item {
    private static final int MIN_BOTELLAS = 8;
    private static final int MAX_BOTELLAS = 16;

    public XpCrateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pila = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(pila, true);
        }
        pila.shrink(1);
        int cantidad = MIN_BOTELLAS + level.getRandom().nextInt(MAX_BOTELLAS - MIN_BOTELLAS + 1);
        ItemStack premio = new ItemStack(Items.EXPERIENCE_BOTTLE, cantidad);
        if (!player.getInventory().add(premio)) {
            player.drop(premio, false);
        }
        player.displayClientMessage(Component.translatable("message.mazmorra_umbral.crate_abierta"), true);
        return InteractionResultHolder.consume(pila);
    }
}
