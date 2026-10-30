package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MazmorraUmbral.MODID);

    public static final RegistryObject<CreativeModeTab> PRINCIPAL = TABS.register("principal",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mazmorra_umbral"))
                    .icon(() -> new ItemStack(ModItems.FRAGMENTO_DEL_UMBRAL.get()))
                    .displayItems((params, output) -> {
                        for (RegistryObject<net.minecraft.world.item.Item> item : ModItems.ITEMS.getEntries()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private ModTabs() {
    }
}
