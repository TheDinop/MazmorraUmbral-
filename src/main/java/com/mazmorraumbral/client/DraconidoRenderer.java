package com.mazmorraumbral.client;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.entity.DraconidoOcaso;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** El Dracónido reutiliza el modelo del Fantasma (Phantom) vanilla, más grande y con textura propia. */
public class DraconidoRenderer extends MobRenderer<DraconidoOcaso, PhantomModel<DraconidoOcaso>> {
    private static final ResourceLocation TEXTURA =
            new ResourceLocation(MazmorraUmbral.MODID, "textures/entity/draconido_ocaso.png");

    public DraconidoRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new PhantomModel<>(contexto.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
    }

    @Override
    public ResourceLocation getTextureLocation(DraconidoOcaso entidad) {
        return TEXTURA;
    }

    @Override
    protected void scale(DraconidoOcaso entidad, PoseStack pose, float parcial) {
        pose.scale(1.6F, 1.6F, 1.6F);
    }
}
