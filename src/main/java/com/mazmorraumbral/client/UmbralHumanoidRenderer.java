package com.mazmorraumbral.client;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.entity.UmbralMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer de criaturas humanoides: modelo vanilla de zombi con una textura propia por criatura. */
public class UmbralHumanoidRenderer<T extends UmbralMonster> extends HumanoidMobRenderer<T, UmbralHumanoidModel<T>> {
    private final ResourceLocation textura;
    private final float escala;

    public UmbralHumanoidRenderer(EntityRendererProvider.Context contexto, String nombre, float sombra, float escala) {
        super(contexto, new UmbralHumanoidModel<>(contexto.bakeLayer(ModelLayers.ZOMBIE)), sombra);
        this.textura = new ResourceLocation(MazmorraUmbral.MODID, "textures/entity/" + nombre + ".png");
        this.escala = escala;
    }

    @Override
    public ResourceLocation getTextureLocation(T entidad) {
        return textura;
    }

    @Override
    protected void scale(T entidad, PoseStack pose, float parcial) {
        pose.scale(escala, escala, escala);
    }
}
