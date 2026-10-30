package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.block.FloorExitBlock;
import com.mazmorraumbral.block.VaultBlock;
import com.mazmorraumbral.block.WaveSpawnerBlock;
import com.mazmorraumbral.portal.UmbralPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MazmorraUmbral.MODID);

    public static final RegistryObject<Block> PIEDRA_DEL_UMBRAL = BLOCKS.register("piedra_del_umbral",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final RegistryObject<Block> PORTAL_DEL_UMBRAL = BLOCKS.register("portal_del_umbral",
            () -> new UmbralPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F)
                    .lightLevel(state -> 11)
                    .sound(SoundType.GLASS)
                    .noLootTable()));

    public static final RegistryObject<Block> GENERADOR_OLEADAS = BLOCKS.register("generador_oleadas",
            () -> new WaveSpawnerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 9)
                    .sound(SoundType.AMETHYST)
                    .noLootTable()));

    public static final RegistryObject<Block> ASCENSO_DEL_UMBRAL = BLOCKS.register("ascenso_del_umbral",
            () -> new FloorExitBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 12)
                    .sound(SoundType.AMETHYST)
                    .noLootTable()));

    public static final RegistryObject<Block> BOVEDA_DEL_UMBRAL = BLOCKS.register("boveda_del_umbral",
            () -> new VaultBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 12)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noLootTable()));

    private ModBlocks() {
    }
}
