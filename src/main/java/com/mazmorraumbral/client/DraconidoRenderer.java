package com.mazmorraumbral.client;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.entity.DraconidoOcaso;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;

/**
 * El Draconido reutiliza la geometria del Phantom vanilla,
 * con textura propia y escala aumentada.
 */
public class DraconidoRenderer extends MobRenderer<DraconidoOcaso, DraconidoRenderer.DraconidoModel> {

    private static final ResourceLocation TEXTURA =
            new ResourceLocation(MazmorraUmbral.MODID, "textures/entity/draconido_ocaso.png");

    public DraconidoRenderer(EntityRendererProvider.Context contexto) {
        super(
                contexto,
                new DraconidoModel(
                        new PhantomModel<Phantom>(
                                contexto.bakeLayer(ModelLayers.PHANTOM)
                        ).root()
                ),
                0.75F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(DraconidoOcaso entidad) {
        return TEXTURA;
    }

    @Override
    protected void scale(DraconidoOcaso entidad, PoseStack pose, float parcial) {
        pose.scale(1.6F, 1.6F, 1.6F);
    }

    /**
     * Adaptador para utilizar la geometria del Phantom
     * con la entidad DraconidoOcaso.
     */
    public static class DraconidoModel extends HierarchicalModel<DraconidoOcaso> {

        private final ModelPart raiz;

        public DraconidoModel(ModelPart raiz) {
            this.raiz = raiz;
        }

        @Override
        public ModelPart root() {
            return raiz;
        }

        @Override
        public void setupAnim(
                DraconidoOcaso entidad,
                float movimiento,
                float intensidad,
                float tiempo,
                float giroCabeza,
                float inclinacionCabeza
        ) {
            // La geometria del Phantom se mantiene sin animacion propia.
        }
    }
}
