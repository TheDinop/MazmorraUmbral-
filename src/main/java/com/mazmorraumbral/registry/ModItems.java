package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MazmorraUmbral.MODID);

    public static final RegistryObject<Item> PIEDRA_DEL_UMBRAL = ITEMS.register("piedra_del_umbral",
            () -> new BlockItem(ModBlocks.PIEDRA_DEL_UMBRAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> FRAGMENTO_DEL_UMBRAL = ITEMS.register("fragmento_del_umbral",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    private ModItems() {
    }
}
