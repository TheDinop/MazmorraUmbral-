package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.blockentity.WaveSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MazmorraUmbral.MODID);

    public static final RegistryObject<BlockEntityType<WaveSpawnerBlockEntity>> GENERADOR_OLEADAS =
            BLOCK_ENTITIES.register("generador_oleadas",
                    () -> BlockEntityType.Builder.of(WaveSpawnerBlockEntity::new,
                            ModBlocks.GENERADOR_OLEADAS.get()).build(null));

    private ModBlockEntities() {
    }
}
