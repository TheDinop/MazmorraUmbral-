package com.mazmorraumbral.entity;

import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.util.CombatUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * Verdugo Carmesí: marca al objetivo, prepara un ataque pesado telegrafiado (18-26 de daño) y, si falla, queda
 * expuesto (recibe x1,5 de daño y no puede moverse) durante una ventana de recuperación.
 */
public class VerdugoCarmesi extends UmbralMonster {
    public static final int STATE_WINDUP = 1;
    public static final int STATE_RECOVERY = 2;

    private static final DustParticleOptions POLVO_CARMESI =
            new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.05F), 1.3F);
    private static final int DURACION_PREPARACION = 30;
    private int enfriamientoPesado = 60;
    private boolean fallo;

    public VerdugoCarmesi(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 130.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GatedMeleeAttackGoal(this, 1.1D, false,
                () -> getState() == STATE_IDLE));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int[] tablaVida() {
        return new int[]{130, 250, 420};
    }

    @Override
    protected List<Drop> drops() {
        return List.of(
                new Drop(ModItems.INSIGNIA_EJECUCION, 0.25F, 1, 1),
                new Drop(ModItems.TENDON_CARMESI, 0.65F, 1, 2));
    }

    @Override
    public boolean armsRaised() {
        return getState() == STATE_WINDUP;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        if (enfriamientoPesado > 0) enfriamientoPesado--;

        LivingEntity objetivo = this.getTarget();
        switch (getState()) {
            case STATE_IDLE -> {
                if (enfriamientoPesado <= 0 && objetivo != null && this.distanceToSqr(objetivo) <= 12.0D
                        && this.hasLineOfSight(objetivo)) {
                    setState(STATE_WINDUP);
                    this.getNavigation().stop();
                    this.level().playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR,
                            SoundSource.HOSTILE, 1.0F, 0.8F);
                    if (this.level() instanceof ServerLevel sl) {
                        // Marca visible sobre el objetivo.
                        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, objetivo.getX(), objetivo.getY() + 2.3D,
                                objetivo.getZ(), 4, 0.2D, 0.1D, 0.2D, 0.0D);
                    }
                }
            }
            case STATE_WINDUP -> {
                this.getNavigation().stop();
                if (objetivo != null) {
                    this.getLookControl().setLookAt(objetivo, 60.0F, 60.0F);
                }
                if (this.level() instanceof ServerLevel sl) {
                    double progreso = Math.min(1.0D, stateTicks / (double) DURACION_PREPARACION);
                    CombatUtil.anillo(sl, this.position(), 1.0D + 2.8D * progreso, POLVO_CARMESI, 14);
                    if (objetivo != null) {
                        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, objetivo.getX(), objetivo.getY() + 2.3D,
                                objetivo.getZ(), 1, 0.1D, 0.0D, 0.1D, 0.0D);
                    }
                }
                if (stateTicks >= DURACION_PREPARACION) {
                    golpePesado();
                }
            }
            case STATE_RECOVERY -> {
                this.getNavigation().stop();
                if (stateTicks >= (fallo ? 70 : 30)) {
                    setState(STATE_IDLE);
                    enfriamientoPesado = 100;
                }
            }
            default -> {
            }
        }
    }

    private void golpePesado() {
        boolean acierto = false;
        float dano = 18.0F + this.random.nextFloat() * 8.0F; // 18-26
        for (LivingEntity e : CombatUtil.enCono(this, 3.8D, 55.0D, CombatUtil.SOLO_JUGADORES)) {
            if (e.hurt(this.damageSources().mobAttack(this), dano)) {
                e.knockback(1.0D, this.getX() - e.getX(), this.getZ() - e.getZ());
                acierto = true;
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                0.6F, 1.4F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + CombatUtil.mirada(this).x * 2.0D, getY() + 1.0D,
                    getZ() + CombatUtil.mirada(this).z * 2.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        fallo = !acierto;
        setState(STATE_RECOVERY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getState() == STATE_RECOVERY) {
            amount *= 1.5F; // expuesto
        }
        return super.hurt(source, amount);
    }
}
