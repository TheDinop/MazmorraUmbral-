package com.mazmorraumbral;

import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.portal.PortalActivationHandler;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.registry.ModRecipes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(MazmorraUmbral.MODID)
public class MazmorraUmbral {
    public static final String MODID = "mazmorra_umbral";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MazmorraUmbral() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);

        modBus.addListener(this::agregarAPestanasCreativas);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UmbralConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(new PortalActivationHandler());
    }

    private void agregarAPestanasCreativas(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.PIEDRA_DEL_UMBRAL);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.FRAGMENTO_DEL_UMBRAL);
        }
    }
}
