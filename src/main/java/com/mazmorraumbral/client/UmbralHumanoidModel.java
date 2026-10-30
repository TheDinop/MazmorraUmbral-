package com.mazmorraumbral.client;

import com.mazmorraumbral.entity.UmbralMonster;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Modelo humanoide vanilla; levanta los brazos mientras la criatura prepara un ataque. */
public class UmbralHumanoidModel<T extends UmbralMonster> extends HumanoidModel<T> {

    public UmbralHumanoidModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T entidad, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        super.setupAnim(entidad, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entidad.armsRaised()) {
            AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks);
        }
    }
}
