package com.mazmorraumbral.item;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.config.UmbralConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Crate: cinco tiradas independientes. Cada tirada elige primero una categoría de rareza (matriz de CrateRarity)
 * y después un objeto de la tabla de esa categoría. La crate se consume ANTES de entregar nada: un fallo a mitad
 * pierde el premio, nunca lo duplica. Todo ocurre en el servidor.
 */
public class CrateItem extends Item {
    public static final int TIRADAS = 5;

    private final CrateRarity rareza;

    public CrateItem(Properties properties, CrateRarity rareza) {
        super(properties);
        this.rareza = rareza;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pila = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel servidor)) {
            return InteractionResultHolder.sidedSuccess(pila, level.isClientSide);
        }
        pila.shrink(1);

        for (int i = 0; i < TIRADAS; i++) {
            CrateRarity categoria = rareza.elegirCategoria(servidor.getRandom());
            ResourceLocation id = new ResourceLocation(MazmorraUmbral.MODID, "crates/" + categoria.id());
            LootTable tabla = servidor.getServer().getLootData().getLootTable(id);
            LootParams parametros = new LootParams.Builder(servidor)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .create(LootContextParamSets.CHEST);
            for (ItemStack premio : tabla.getRandomItems(parametros)) {
                if (UmbralConfig.LOG_DEPURACION.get()) {
                    MazmorraUmbral.LOGGER.info("Crate {} tirada {} -> {} x{} ({})", rareza.id(), i + 1,
                            premio.getItem(), premio.getCount(), categoria.id());
                }
                if (!player.getInventory().add(premio)) {
                    player.drop(premio, false);
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.mazmorra_umbral.crate_abierta"), true);
        return InteractionResultHolder.consume(pila);
    }
}
