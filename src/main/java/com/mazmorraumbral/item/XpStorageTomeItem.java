package com.mazmorraumbral.item;

import com.mazmorraumbral.util.XpUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Tomo de Almacenamiento de XP: guarda hasta 10.000 puntos reales. Clic derecho = depositar tu XP; agachado +
 * clic derecho = retirar todo. El saldo vive en el NBT de ESTE objeto y solo cambia en el servidor. En cada
 * transacción se actualiza primero el origen y después el destino, en el mismo tick, así que no hay ventana de
 * duplicación. Se apila de 1 en 1.
 */
public class XpStorageTomeItem extends Item {
    public static final int CAPACIDAD = 10_000;
    private static final String CLAVE = "XpAlmacenada";

    public XpStorageTomeItem(Properties properties) {
        super(properties);
    }

    public static int almacenada(ItemStack pila) {
        return pila.hasTag() ? Math.max(0, pila.getTag().getInt(CLAVE)) : 0;
    }

    private static void fijar(ItemStack pila, int puntos) {
        pila.getOrCreateTag().putInt(CLAVE, Math.max(0, Math.min(CAPACIDAD, puntos)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pila = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(pila, true);
        }
        int guardada = almacenada(pila);
        if (player.isShiftKeyDown()) {
            if (guardada <= 0) {
                player.displayClientMessage(Component.translatable("message.mazmorra_umbral.tomo_vacio"), true);
                return InteractionResultHolder.fail(pila);
            }
            fijar(pila, 0);
            XpUtil.dar(player, guardada);
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.tomo_retirado", guardada), true);
        } else {
            int libre = CAPACIDAD - guardada;
            int mover = Math.min(XpUtil.total(player), libre);
            if (mover <= 0) {
                player.displayClientMessage(Component.translatable(libre <= 0
                        ? "message.mazmorra_umbral.tomo_lleno" : "message.mazmorra_umbral.tomo_sin_xp"), true);
                return InteractionResultHolder.fail(pila);
            }
            fijar(pila, guardada + mover);
            XpUtil.quitar(player, mover);
            player.displayClientMessage(Component.translatable("message.mazmorra_umbral.tomo_depositado", mover), true);
        }
        return InteractionResultHolder.consume(pila);
    }

    @Override
    public void appendHoverText(ItemStack pila, Level level, List<Component> lineas, TooltipFlag bandera) {
        lineas.add(Component.translatable("tooltip.mazmorra_umbral.tomo_almacenamiento", almacenada(pila), CAPACIDAD));
    }
}
