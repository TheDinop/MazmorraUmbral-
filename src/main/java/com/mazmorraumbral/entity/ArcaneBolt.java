package com.mazmorraumbral.entity;

import com.mazmorraumbral.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Proyectil arcano: daño mágico, sin fuego, visible por su estela de partículas. */
public class ArcaneBolt extends AbstractHurtingProjectile {
    private float dano = 15.0F;

    public ArcaneBolt(EntityType<? extends ArcaneBolt> type, Level level) {
        super(type, level);
    }

    public ArcaneBolt(Level level, LivingEntity owner, double dx, double dy, double dz, float dano) {
        super(ModEntities.ARCANE_BOLT.get(), owner, dx, dy, dz, level);
        this.dano = dano;
    }

    @Override
    protected float getInertia() {
        return 0.88F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.WITCH;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof UmbralMonster);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity vivo) {
            Entity dueno = this.getOwner();
            vivo.hurt(this.damageSources().indirectMagic(this, dueno), dano);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
