package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * La dimensión se define por datapack (data/mazmorra_umbral/dimension/umbral.json y
 * dimension_type/umbral.json); aquí solo se guarda su clave.
 */
public final class ModDimensions {
    public static final ResourceKey<Level> UMBRAL =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MazmorraUmbral.MODID, "umbral"));

    private ModDimensions() {
    }
}
