package com.mazmorraumbral.entity;

import com.mazmorraumbral.registry.ModItems;
import com.mazmorraumbral.util.CombatUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Arcanista del Ocaso: lanza un proyectil arcano (12-18, daño mágico) y una runa que ancla al objetivo si no se
 * aparta. El cristal de su bastón (visible en la mano) absorbe parte del daño; al romperse interrumpe el hechizo
 * en curso (o el siguiente) y la deja aturdida un momento. El cristal se regenera pasado un tiempo.
 */
public class ArcanistaOcaso extends UmbralMonster {
    public static final int STATE_CASTING = 1;
    public static final int STATE_STAGGER = 2;

    private static final DustParticleOptions POLVO_RUNA =
            new DustParticleOptions(new Vector3f(0.55F, 0.2F, 0.95F), 1.2F);
    private static final int PREPARACION = 25;
    private static final float VIDA_CRISTAL = 40.0F;

    private int enfriamientoHechizo = 40;
    private boolean siguienteRuna;
    private boolean runaEnCurso;
    private Vec3 posRuna = Vec3.ZERO;
    private float vidaCristal = VIDA_CRISTAL;
    private boolean cristalActivo = true;
    private int regeneracionCristal;
    private boolean interrumpirSiguiente;

    public ArcanistaOcaso(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AMETHYST_SHARD));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0D, 1.25D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
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
                new Drop(ModItems.POLVO_ARCANO, 0.80F, 1, 3),
                new Drop(ModItems.CATALIZADOR_MAGICO, 0.15F, 1, 1),
                new Drop(ModItems.PRISMA_CONVERGENCIA, 0.08F, 1, 1));
    }

    @Override
    public boolean armsRaised() {
        return getState() == STATE_CASTING;
    }

    public boolean isCristalActivo() {
        return cristalActivo;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        if (enfriamientoHechizo > 0) enfriamientoHechizo--;

        if (!cristalActivo && --regeneracionCristal <= 0) {
            cristalActivo = true;
            vidaCristal = VIDA_CRISTAL;
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AMETHYST_SHARD));
            this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        if (cristalActivo && this.level() instanceof ServerLevel sl && this.tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.1D, getZ(), 1, 0.15D, 0.1D, 0.15D, 0.0D);
        }

        LivingEntity objetivo = this.getTarget();
        switch (getState()) {
            case STATE_IDLE -> {
                if (enfriamientoHechizo <= 0 && objetivo != null && this.distanceToSqr(objetivo) < 18.0D * 18.0D
                        && this.hasLineOfSight(objetivo)) {
                    if (interrumpirSiguiente) {
                        interrumpirSiguiente = false;
                        enfriamientoHechizo = 60;
                        this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.HOSTILE, 1.0F, 1.5F);
                        return;
                    }
                    runaEnCurso = siguienteRuna;
                    siguienteRuna = !siguienteRuna;
                    posRuna = objetivo.position();
                    setState(STATE_CASTING);
                    this.getNavigation().stop();
                }
            }
            case STATE_CASTING -> {
                this.getNavigation().stop();
                if (objetivo != null) {
                    this.getLookControl().setLookAt(objetivo, 60.0F, 60.0F);
                }
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.8D, getZ(), 3, 0.3D, 0.3D, 0.3D, 0.2D);
                    if (runaEnCurso) {
                        CombatUtil.anillo(sl, posRuna, 1.6D, POLVO_RUNA, 16);
                    }
                }
                if (stateTicks >= PREPARACION) {
                    if (runaEnCurso) {
                        lanzarRuna(objetivo);
                    } else {
                        lanzarProyectil(objetivo);
                    }
                    setState(STATE_IDLE);
                    enfriamientoHechizo = runaEnCurso ? 60 : 50;
                }
            }
            case STATE_STAGGER -> {
                this.getNavigation().stop();
                if (stateTicks >= 40) {
                    setState(STATE_IDLE);
                }
            }
            default -> {
            }
        }
    }

    private void lanzarProyectil(LivingEntity objetivo) {
        if (objetivo == null) {
            return;
        }
        Vec3 origen = this.getEyePosition();
        Vec3 destino = objetivo.getEyePosition();
        Vec3 d = destino.subtract(origen);
        float dano = 12.0F + this.random.nextFloat() * 6.0F; // 12-18
        ArcaneBolt bolt = new ArcaneBolt(this.level(), this, d.x, d.y, d.z, dano);
        bolt.setPos(origen.x, origen.y - 0.2D, origen.z);
        this.level().addFreshEntity(bolt);
        this.level().playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE,
                1.0F, 1.2F);
    }

    private void lanzarRuna(LivingEntity objetivo) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WITCH, posRuna.x, posRuna.y + 0.2D, posRuna.z, 20, 0.8D, 0.1D, 0.8D, 0.05D);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE,
                1.0F, 0.8F);
        // Solo ancla a quien siga dentro de la runa: se puede esquivar apartándose durante la preparación.
        for (LivingEntity e : CombatUtil.enRadio(posRuna, 1.8D, 1.5D, this.level(), CombatUtil.SOLO_JUGADORES)) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (cristalActivo && source.getEntity() != null) {
            vidaCristal -= amount * 0.3F;
            if (vidaCristal <= 0.0F) {
                romperCristal();
            }
        }
        return super.hurt(source, amount);
    }

    private void romperCristal() {
        cristalActivo = false;
        regeneracionCristal = 400;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 0.9F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.0D, getZ(), 15, 0.3D, 0.3D, 0.3D, 0.2D);
        }
        if (getState() == STATE_CASTING) {
            setState(STATE_STAGGER);        // interrumpe el hechizo en preparación
            enfriamientoHechizo = 80;
        } else {
            interrumpirSiguiente = true;    // el siguiente hechizo se pierde
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CristalActivo", cristalActivo);
        tag.putFloat("VidaCristal", vidaCristal);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CristalActivo")) {
            cristalActivo = tag.getBoolean("CristalActivo");
            vidaCristal = tag.getFloat("VidaCristal");
            if (!cristalActivo) {
                regeneracionCristal = 200;
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            }
        }
    }
}
