package com.mazmorraumbral.client;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registro de renderers (solo cliente). */
@Mod.EventBusSubscriber(modid = MazmorraUmbral.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registrarRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FANTASMA_ESPECTRAL.get(),
                ctx -> new UmbralHumanoidRenderer<>(ctx, "fantasma_espectral", 0.4F, 1.0F));
        event.registerEntityRenderer(ModEntities.CABALLERO_MARCHITO.get(),
                ctx -> new UmbralHumanoidRenderer<>(ctx, "caballero_marchito", 0.6F, 1.1F));
        event.registerEntityRenderer(ModEntities.VERDUGO_CARMESI.get(),
                ctx -> new UmbralHumanoidRenderer<>(ctx, "verdugo_carmesi", 0.7F, 1.2F));
        event.registerEntityRenderer(ModEntities.ARCANISTA_OCASO.get(),
                ctx -> new UmbralHumanoidRenderer<>(ctx, "arcanista_ocaso", 0.4F, 1.0F));
        event.registerEntityRenderer(ModEntities.CUSTODIO_RELIQUIAS.get(),
                ctx -> new UmbralHumanoidRenderer<>(ctx, "custodio_reliquias", 0.7F, 1.15F));
        event.registerEntityRenderer(ModEntities.CABALLERO_NETHERITE_VACIO.get(),
                ctx -> new UmbralHumanoidRenderer<>(ctx, "caballero_netherite_vacio", 0.9F, 1.35F));
        event.registerEntityRenderer(ModEntities.DRACONIDO_OCASO.get(), DraconidoRenderer::new);
        // El proyectil arcano no tiene modelo: se ve por sus partículas.
        event.registerEntityRenderer(ModEntities.ARCANE_BOLT.get(), NoopRenderer::new);
    }
}
