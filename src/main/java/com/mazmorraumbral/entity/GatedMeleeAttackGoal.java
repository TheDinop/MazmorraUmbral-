package com.mazmorraumbral.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import java.util.function.BooleanSupplier;

/** Ataque cuerpo a cuerpo que solo se ejecuta mientras la criatura está libre (no en medio de una habilidad). */
public class GatedMeleeAttackGoal extends MeleeAttackGoal {
    private final BooleanSupplier gate;

    public GatedMeleeAttackGoal(PathfinderMob mob, double speed, boolean followUnseen, BooleanSupplier gate) {
        super(mob, speed, followUnseen);
        this.gate = gate;
    }

    @Override
    public boolean canUse() {
        return gate.getAsBoolean() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return gate.getAsBoolean() && super.canContinueToUse();
    }
}
