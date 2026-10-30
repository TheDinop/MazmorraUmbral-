package com.mazmorraumbral.entity;

import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.util.XpUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Fantasma Espectral: roba XP real al golpear, la almacena (con tope) y al morir devuelve el doble de lo robado
 * más una recompensa base. Si el jugador no tiene XP entra en Fase de Vacío.
 *
 * Interpretación de la Fase de Vacío (el documento no la detalla): invisible; los 3 primeros impactos válidos de
 * una entidad lo atraviesan sin daño; durante la fase su daño sube un 50 %; al agotarse los 3 impactos reaparece
 * y entra en enfriamiento.
 */
public class FantasmaEspectral extends UmbralMonster {
    private static final UUID MOD_FASE_VACIO = UUID.fromString("3b0a3f64-2c1e-4a43-9c0e-5d1f7b8a9c01");
    private static final int ROBO_POR_GOLPE = 25;

    private int robado;
    private UUID victima;
    private int impactosVacio;
    private int enfriamientoVacio;

    public FantasmaEspectral(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0; // la recompensa de XP se entrega en die() para poder sumar lo robado
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GatedMeleeAttackGoal(this, 1.05D, false, () -> true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int[] tablaVida() {
        return new int[]{100, 200, 350};
    }

    @Override
    protected List<Drop> drops() {
        return List.of(
                new Drop(ModItems.FRAGMENTO_DE_ALMA, 0.80F, 1, 3),
                new Drop(ModItems.NUCLEO_DEUDA_ESPECTRAL, 0.12F, 1, 1));
    }

    public int getXpAlmacenado() {
        return robado;
    }

    // ---- Robo de XP ------------------------------------------------------------------------------------------

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean golpe = super.doHurtTarget(target);
        if (golpe && !this.level().isClientSide && target instanceof ServerPlayer jugador) {
            robarXp(jugador);
        }
        return golpe;
    }

    private void robarXp(ServerPlayer jugador) {
        int disponible = XpUtil.total(jugador);
        int limite = UmbralConfig.FANTASMA_LIMITE_XP.get();
        if (disponible > 0 && robado < limite) {
            int cantidad = Math.min(Math.min(disponible, ROBO_POR_GOLPE), limite - robado);
            XpUtil.quitar(jugador, cantidad);
            robado += cantidad;
            victima = jugador.getUUID();
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL, jugador.getX(), jugador.getY() + 1.0D, jugador.getZ(),
                        8, 0.3D, 0.4D, 0.3D, 0.05D);
            }
            this.level().playSound(null, jugador.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE,
                    1.0F, 0.8F);
        }
    }

    // ---- Fase de Vacío ---------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (enfriamientoVacio > 0) {
            enfriamientoVacio--;
        }
        if (this.tickCount % 10 == 0 && impactosVacio <= 0 && enfriamientoVacio <= 0) {
            LivingEntity objetivo = this.getTarget();
            if (objetivo instanceof Player p && XpUtil.total(p) <= 0 && this.distanceToSqr(p) < 100.0D) {
                iniciarFaseVacio();
            }
        }
        if (impactosVacio > 0 && this.level() instanceof ServerLevel sl && this.tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0D, getZ(), 2, 0.3D, 0.5D, 0.3D, 0.01D);
        }
    }

    private void iniciarFaseVacio() {
        impactosVacio = 3;
        aplicarFase(true);
    }

    private void terminarFaseVacio() {
        impactosVacio = 0;
        enfriamientoVacio = 200;
        aplicarFase(false);
    }

    private void aplicarFase(boolean activa) {
        this.setInvisible(activa);
        var dano = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dano != null) {
            dano.removeModifier(MOD_FASE_VACIO);
            if (activa) {
                dano.addTransientModifier(new AttributeModifier(MOD_FASE_VACIO, "fase_vacio", 0.5D,
                        AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
        this.level().playSound(null, this.blockPosition(), activa ? SoundEvents.ENDERMAN_TELEPORT
                : SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (impactosVacio > 0 && source.getEntity() != null
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            impactosVacio--;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 6, 0.3D, 0.5D, 0.3D, 0.02D);
            }
            if (impactosVacio <= 0) {
                terminarFaseVacio();
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    // ---- Devolución de XP ------------------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && !this.dead) {
            int total = robado * 2 + UmbralConfig.FANTASMA_BASE_XP.get();
            robado = 0;
            if (total > 0) {
                ExperienceOrb.award(sl, this.position(), total);
            }
        }
        super.die(source);
    }

    /** Si el fantasma desaparece sin morir (vacío, descarte), lo robado vuelve a la víctima: no se pierde XP. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide && reason == Entity.RemovalReason.DISCARDED && robado > 0) {
            int devolver = robado;
            robado = 0;
            ServerPlayer jugador = victima == null || this.level().getServer() == null ? null
                    : this.level().getServer().getPlayerList().getPlayer(victima);
            if (jugador != null) {
                XpUtil.dar(jugador, devolver);
            } else if (this.level() instanceof ServerLevel sl) {
                ExperienceOrb.award(sl, this.position(), devolver);
            }
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("XpRobada", robado);
        tag.putInt("ImpactosVacio", impactosVacio);
        tag.putInt("EnfriamientoVacio", enfriamientoVacio);
        if (victima != null) {
            tag.putUUID("Victima", victima);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        robado = tag.getInt("XpRobada");
        impactosVacio = tag.getInt("ImpactosVacio");
        enfriamientoVacio = tag.getInt("EnfriamientoVacio");
        if (tag.hasUUID("Victima")) {
            victima = tag.getUUID("Victima");
        }
        if (impactosVacio > 0) {
            aplicarFase(true);
        }
    }
}
