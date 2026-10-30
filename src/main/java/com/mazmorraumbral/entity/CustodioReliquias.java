package com.mazmorraumbral.entity;

import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.util.CombatUtil;
import com.mazmorraumbral.util.UmbralTags;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * Custodio de Reliquias: soporte defensivo. Mantiene UN solo vínculo visible que da Resistencia a un aliado.
 * Cada cierto tiempo sus runas se apagan (estado EXPUESTO, textura distinta): recibe x1,5 de daño y el primer
 * golpe recibido en esa ventana rompe el vínculo. También tiene un pulso de reliquia (12-18) telegrafiado.
 */
public class CustodioReliquias extends UmbralMonster {
    public static final int STATE_PULSE_WINDUP = 1;
    public static final int STATE_EXPOSED = 2;

    private static final DustParticleOptions POLVO_DORADO =
            new DustParticleOptions(new Vector3f(1.0F, 0.8F, 0.2F), 1.2F);

    private UUID aliado;
    private int enfriamientoVinculo = 40;
    private int enfriamientoPulso = 80;
    private int enfriamientoVentana = 300;

    public CustodioReliquias(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GatedMeleeAttackGoal(this, 1.0D, false,
                () -> getState() != STATE_PULSE_WINDUP));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int[] tablaVida() {
        return new int[]{140, 260, 450};
    }

    @Override
    protected List<Drop> drops() {
        return List.of(
                new Drop(ModItems.NUCLEO_RELIQUIA, 0.35F, 1, 1),
                new Drop(ModItems.MATRIZ_PROTECCION_ANTIGUA, 0.18F, 1, 1));
    }

    @Override
    public boolean armsRaised() {
        return getState() == STATE_PULSE_WINDUP;
    }

    public boolean estaExpuesto() {
        return getState() == STATE_EXPOSED;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        if (enfriamientoVinculo > 0) enfriamientoVinculo--;
        if (enfriamientoPulso > 0) enfriamientoPulso--;
        if (enfriamientoVentana > 0) enfriamientoVentana--;

        gestionarVinculo();

        LivingEntity objetivo = this.getTarget();
        switch (getState()) {
            case STATE_IDLE -> {
                if (enfriamientoVentana <= 0) {
                    setState(STATE_EXPOSED);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                            SoundSource.HOSTILE, 1.0F, 1.2F);
                } else if (enfriamientoPulso <= 0 && objetivo != null && this.distanceToSqr(objetivo) < 25.0D) {
                    setState(STATE_PULSE_WINDUP);
                    this.getNavigation().stop();
                }
            }
            case STATE_PULSE_WINDUP -> {
                this.getNavigation().stop();
                if (this.level() instanceof ServerLevel sl) {
                    double progreso = Math.min(1.0D, stateTicks / 25.0D);
                    CombatUtil.anillo(sl, this.position(), 4.5D * progreso, POLVO_DORADO, 18);
                }
                if (stateTicks >= 25) {
                    float dano = 12.0F + this.random.nextFloat() * 6.0F; // 12-18
                    for (LivingEntity e : CombatUtil.enRadio(this.position(), 4.5D, 1.5D, this.level(),
                            CombatUtil.SOLO_JUGADORES)) {
                        if (e.hurt(this.damageSources().mobAttack(this), dano)) {
                            e.knockback(0.9D, this.getX() - e.getX(), this.getZ() - e.getZ());
                        }
                    }
                    this.level().playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                            SoundSource.HOSTILE, 0.5F, 1.6F);
                    setState(STATE_IDLE);
                    enfriamientoPulso = 160;
                }
            }
            case STATE_EXPOSED -> {
                if (stateTicks >= 80) {
                    setState(STATE_IDLE);
                    enfriamientoVentana = 300;
                }
            }
            default -> {
            }
        }
    }

    private LivingEntity buscarAliado() {
        if (aliado == null || !(this.level() instanceof ServerLevel sl)) {
            return null;
        }
        Entity e = sl.getEntity(aliado);
        return e instanceof LivingEntity vivo && vivo.isAlive() ? vivo : null;
    }

    private void gestionarVinculo() {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity actual = buscarAliado();
        if (aliado != null && (actual == null || this.distanceToSqr(actual) > 24.0D * 24.0D)) {
            romperVinculo(false);
            actual = null;
        }
        if (actual == null) {
            if (enfriamientoVinculo <= 0 && this.tickCount % 20 == 0 && getState() != STATE_EXPOSED) {
                List<LivingEntity> candidatos = sl.getEntitiesOfClass(LivingEntity.class,
                        this.getBoundingBox().inflate(16.0D),
                        e -> e != this && e instanceof Monster && !(e instanceof CustodioReliquias)
                                && (e instanceof UmbralMonster || UmbralTags.esDeMazmorra(e)));
                if (!candidatos.isEmpty()) {
                    LivingEntity elegido = candidatos.get(0);
                    double mejor = this.distanceToSqr(elegido);
                    for (LivingEntity c : candidatos) {
                        double d = this.distanceToSqr(c);
                        if (d < mejor) {
                            mejor = d;
                            elegido = c;
                        }
                    }
                    aliado = elegido.getUUID();
                    this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                            SoundSource.HOSTILE, 0.8F, 1.4F);
                }
            }
            return;
        }
        // Vínculo activo: línea visible y resistencia para el aliado.
        if (this.tickCount % 4 == 0 && getState() != STATE_EXPOSED) {
            CombatUtil.linea(sl, this.position().add(0.0D, 1.3D, 0.0D), actual.position().add(0.0D, 1.0D, 0.0D),
                    ParticleTypes.END_ROD, 10);
        }
        if (this.tickCount % 20 == 0 && getState() != STATE_EXPOSED) {
            actual.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, true, true));
        }
    }

    private void romperVinculo(boolean porGolpe) {
        if (aliado == null) {
            return;
        }
        LivingEntity actual = buscarAliado();
        if (actual != null) {
            actual.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
        aliado = null;
        enfriamientoVinculo = 200;
        if (porGolpe) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE,
                    1.0F, 0.6F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.3D, getZ(), 12, 0.4D, 0.4D, 0.4D, 0.2D);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide) {
            romperVinculo(false);
        }
        super.die(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getState() == STATE_EXPOSED && source.getEntity() != null) {
            amount *= 1.5F;
            if (aliado != null) {
                romperVinculo(true);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (aliado != null) {
            tag.putUUID("Aliado", aliado);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Aliado")) {
            aliado = tag.getUUID("Aliado");
        }
    }
}
