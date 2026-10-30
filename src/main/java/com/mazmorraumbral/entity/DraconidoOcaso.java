package com.mazmorraumbral.entity;

import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.util.CombatUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dracónido del Ocaso: atacante aéreo. Orbita sobre el objetivo, lanza un aliento espectral (cono) y un picado
 * (16-24 de daño). Si el picado falla, cae aturdido y vulnerable (x1,5) un momento.
 */
public class DraconidoOcaso extends UmbralMonster {
    public static final int STATE_BREATH_WINDUP = 1;
    public static final int STATE_BREATH = 2;
    public static final int STATE_DIVE_WINDUP = 3;
    public static final int STATE_DIVING = 4;
    public static final int STATE_STUNNED = 5;

    private int enfriamientoAliento = 80;
    private int enfriamientoPicado = 120;
    private Vec3 destinoPicado = Vec3.ZERO;
    private boolean picadoAcerto;

    public DraconidoOcaso(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int[] tablaVida() {
        return new int[]{120, 230, 400};
    }

    @Override
    protected List<Drop> drops() {
        return List.of(
                new Drop(ModItems.MEMBRANA_ESPECTRAL, 0.60F, 1, 2),
                new Drop(ModItems.ESCAMAS, 0.75F, 1, 3),
                new Drop(ModItems.TENDON_ALA_UMBRIA, 0.20F, 1, 1));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        if (enfriamientoAliento > 0) enfriamientoAliento--;
        if (enfriamientoPicado > 0) enfriamientoPicado--;

        LivingEntity objetivo = this.getTarget();
        int estado = getState();

        if (estado == STATE_STUNNED) {
            if (stateTicks >= 60) {
                this.setNoGravity(true);
                setState(STATE_IDLE);
                enfriamientoPicado = 200;
            }
            return;
        }
        if (objetivo == null) {
            return;
        }

        switch (estado) {
            case STATE_IDLE -> {
                // Orbitar sobre el objetivo.
                if (this.tickCount % 10 == 0) {
                    double a = this.tickCount * 0.06D;
                    this.getMoveControl().setWantedPosition(objetivo.getX() + Math.cos(a) * 7.0D,
                            objetivo.getY() + 5.0D, objetivo.getZ() + Math.sin(a) * 7.0D, 1.0D);
                }
                double dist2 = this.distanceToSqr(objetivo);
                if (enfriamientoPicado <= 0 && dist2 < 20.0D * 20.0D && this.hasLineOfSight(objetivo)) {
                    setState(STATE_DIVE_WINDUP);
                } else if (enfriamientoAliento <= 0 && dist2 < 10.0D * 10.0D && this.hasLineOfSight(objetivo)) {
                    setState(STATE_BREATH_WINDUP);
                }
            }
            case STATE_BREATH_WINDUP -> {
                this.getLookControl().setLookAt(objetivo, 60.0F, 60.0F);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
                if (this.level() instanceof ServerLevel sl) {
                    Vec3 boca = this.getEyePosition().add(CombatUtil.mirada(this).scale(1.2D));
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boca.x, boca.y, boca.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                }
                if (stateTicks >= 20) {
                    setState(STATE_BREATH);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE,
                            1.0F, 0.6F);
                }
            }
            case STATE_BREATH -> {
                this.getLookControl().setLookAt(objetivo, 30.0F, 30.0F);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
                Vec3 boca = this.getEyePosition();
                Vec3 dir = objetivo.getEyePosition().subtract(boca).normalize();
                if (this.level() instanceof ServerLevel sl) {
                    for (int i = 1; i <= 6; i++) {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boca.x + dir.x * i, boca.y + dir.y * i,
                                boca.z + dir.z * i, 2, 0.25D, 0.25D, 0.25D, 0.01D);
                    }
                }
                if (stateTicks % 8 == 0) {
                    for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class,
                            this.getBoundingBox().inflate(9.0D), CombatUtil.SOLO_JUGADORES::test)) {
                        Vec3 a = e.getEyePosition().subtract(boca);
                        double largo = a.length();
                        if (largo <= 8.0D && largo > 0.1D && a.normalize().dot(dir) > 0.9D) {
                            e.hurt(this.damageSources().indirectMagic(this, this), 3.5F);
                        }
                    }
                }
                if (stateTicks >= 40) {
                    setState(STATE_IDLE);
                    enfriamientoAliento = 140;
                }
            }
            case STATE_DIVE_WINDUP -> {
                this.getLookControl().setLookAt(objetivo, 60.0F, 60.0F);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.7D));
                if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
                    sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5D, getZ(), 6, 0.5D, 0.3D, 0.5D, 0.3D);
                }
                if (stateTicks >= 25) {
                    destinoPicado = objetivo.position().add(0.0D, 0.5D, 0.0D);
                    picadoAcerto = false;
                    setState(STATE_DIVING);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE,
                            1.2F, 0.7F);
                }
            }
            case STATE_DIVING -> {
                Vec3 hacia = destinoPicado.subtract(this.position());
                if (hacia.lengthSqr() > 0.01D) {
                    this.setDeltaMovement(hacia.normalize().scale(1.2D));
                    this.getLookControl().setLookAt(destinoPicado.x, destinoPicado.y, destinoPicado.z);
                }
                this.hasImpulse = true;
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class,
                        this.getBoundingBox().inflate(0.6D), CombatUtil.SOLO_JUGADORES::test)) {
                    float dano = 16.0F + this.random.nextFloat() * 8.0F; // 16-24
                    if (e.hurt(this.damageSources().mobAttack(this), dano)) {
                        e.knockback(0.9D, this.getX() - e.getX(), this.getZ() - e.getZ());
                        picadoAcerto = true;
                    }
                }
                boolean llego = this.position().distanceToSqr(destinoPicado) < 2.0D || this.horizontalCollision
                        || this.verticalCollision || stateTicks > 30;
                if (picadoAcerto) {
                    setState(STATE_IDLE);
                    enfriamientoPicado = 160;
                    this.setDeltaMovement(0.0D, 0.6D, 0.0D);
                } else if (llego) {
                    // Falló: cae aturdido y queda vulnerable.
                    this.setNoGravity(false);
                    setState(STATE_STUNNED);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.PHANTOM_DEATH, SoundSource.HOSTILE,
                            1.0F, 1.4F);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getState() == STATE_STUNNED) {
            amount *= 1.5F;
        }
        return super.hurt(source, amount);
    }
}
