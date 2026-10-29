package com.mazmorraumbral.recipe;

import com.google.gson.JsonObject;
import com.mazmorraumbral.registry.ModRecipes;
import com.mazmorraumbral.state.UmbralSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Receta con forma que deja de coincidir cuando el mundo ya registró la primera activación exitosa
 * del portal. Alcance actual: SOLO MUNDO. El alcance por jugador no es posible desde matches(),
 * porque la receta no recibe al jugador; queda pendiente (ver ESTADO.md).
 */
public class RecetaBloqueableShaped extends ShapedRecipe {

    public RecetaBloqueableShaped(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        if (level.isClientSide) {
            // El servidor decide el resultado real; el cliente solo predice.
            return true;
        }
        MinecraftServer server = level.getServer();
        return server == null || !UmbralSavedData.get(server).isRecetaBloqueada();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHAPED_BLOQUEABLE.get();
    }

    public static class Serializer implements RecipeSerializer<RecetaBloqueableShaped> {
        private final ShapedRecipe.Serializer base = new ShapedRecipe.Serializer();

        @Override
        public RecetaBloqueableShaped fromJson(ResourceLocation id, JsonObject json) {
            return new RecetaBloqueableShaped(base.fromJson(id, json));
        }

        @Override
        public RecetaBloqueableShaped fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new RecetaBloqueableShaped(base.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RecetaBloqueableShaped recipe) {
            base.toNetwork(buf, recipe);
        }
    }
}
