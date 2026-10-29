package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.recipe.RecetaBloqueableShaped;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MazmorraUmbral.MODID);

    public static final RegistryObject<RecipeSerializer<RecetaBloqueableShaped>> SHAPED_BLOQUEABLE =
            SERIALIZERS.register("shaped_bloqueable", RecetaBloqueableShaped.Serializer::new);

    private ModRecipes() {
    }
}
