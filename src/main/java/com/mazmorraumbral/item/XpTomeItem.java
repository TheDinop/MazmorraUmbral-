package com.mazmorraumbral.item;

import com.mazmorraumbral.util.XpUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Tomo de Experiencia: consumible que otorga 500 puntos reales de XP. */
public class XpTomeItem extends Item {
    public static final int PUNTOS = 500;

    public XpTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pila = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(pila, true);
        }
        pila.shrink(1);
        XpUtil.dar(player, PUNTOS);
        return InteractionResultHolder.consume(pila);
    }
}
