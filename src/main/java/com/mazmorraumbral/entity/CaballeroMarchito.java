package com.mazmorraumbral.entity;

import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.util.CombatUtil;
import com.mazmorraumbral.util.UmbralTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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

import java.util.List;

/**
 * Caballero de la Orden Marchita: tanque. Bloqueo frontal con contraataque; vulnerable por los flancos y la
 * espalda (daño x1,5); protege temporalmente a otros enemigos con Resistencia.
 */
public class CaballeroMarchito extends UmbralMonster {
    public static final int STATE_BLOCK = 1;

    private static final int DURACION_BLOQUEO = 50;
    private int enfriamientoBloqueo = 60;
    private int enfriamientoContra;
    private int enfriamientoProteccion = 100;

    public CaballeroMarchito(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)   // objetivo 10-16
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GatedMeleeAttackGoal(this, 1.0D, false, () -> true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int[] tablaVida() {
        return new int[]{160, 300, 500};
    }

    @Override
    protected List<Drop> drops() {
        return List.of(
                new Drop(ModItems.FRAGMENTO_ACERO_RUNICO, 0.80F, 1, 3),
                new Drop(ModItems.SELLO_ORDEN_MARCHITA, 0.15F, 1, 1));
    }

    @Override
    public boolean armsRaised() {
        return false;
    }

    public boolean estaBloqueando() {
        return getState() == STATE_BLOCK;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        if (enfriamientoBloqueo > 0) enfriamientoBloqueo--;
        if (enfriamientoContra > 0) enfriamientoContra--;
        if (enfriamientoProteccion > 0) enfriamientoProteccion--;

        LivingEntity objetivo = this.getTarget();
        if (getState() == STATE_BLOCK) {
            if (stateTicks >= DURACION_BLOQUEO) {
                setState(STATE_IDLE);
                enfriamientoBloqueo = 100;
            }
        } else if (enfriamientoBloqueo <= 0 && objetivo != null && this.distanceToSqr(objetivo) < 64.0D) {
            setState(STATE_BLOCK);
            this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE,
                    0.8F, 0.7F);
        }

        if (enfriamientoProteccion <= 0 && this.tickCount % 20 == 0) {
            proteger();
        }
    }

    /** Resistencia temporal a un aliado cercano que todavía no la tenga. */
    private void proteger() {
        List<LivingEntity> aliados = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(10.0D),
                e -> e != this && e instanceof Monster && (e instanceof UmbralMonster || UmbralTags.esDeMazmorra(e))
                        && !e.hasEffect(MobEffects.DAMAGE_RESISTANCE));
        if (aliados.isEmpty()) {
            return;
        }
        LivingEntity aliado = aliados.get(this.random.nextInt(aliados.size()));
        aliado.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
        if (this.level() instanceof ServerLevel sl) {
            CombatUtil.linea(sl, this.position().add(0, 1.2, 0), aliado.position().add(0, 1.0, 0),
                    ParticleTypes.ENCHANT, 8);
        }
        enfriamientoProteccion = 300;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity atacante = source.getEntity();
        if (atacante != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            double cos = CombatUtil.cosHacia(this, atacante);
            if (estaBloqueando() && cos > 0.34D && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE,
                        1.0F, 1.0F);
                if (!this.level().isClientSide && enfriamientoContra <= 0 && atacante instanceof LivingEntity vivo
                        && this.distanceToSqr(vivo) < 16.0D) {
                    vivo.hurt(this.damageSources().mobAttack(this), 12.0F);
                    vivo.knockback(0.6D, this.getX() - vivo.getX(), this.getZ() - vivo.getZ());
                    enfriamientoContra = 20;
                }
                return false;
            }
            if (cos < 0.0D) {
                amount *= 1.5F; // flanco o espalda
            }
        }
        return super.hurt(source, amount);
    }
}
