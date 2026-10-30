package com.mazmorraumbral;

import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.event.UmbralEvents;
import com.mazmorraumbral.portal.PortalActivationHandler;
import com.mazmorraumbral.registry.ModBlockEntities;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.registry.ModEntities;
import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.registry.ModRecipes;
import com.mazmorraumbral.registry.ModTabs;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MazmorraUmbral.MODID)
public class MazmorraUmbral {
    public static final String MODID = "mazmorra_umbral";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MazmorraUmbral() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModTabs.TABS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UmbralConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(new PortalActivationHandler());
        MinecraftForge.EVENT_BUS.register(new UmbralEvents());
    }
}
