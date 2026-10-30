package com.mazmorraumbral.entity;

import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.util.CombatUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Caballero de Netherite del Vacío Umbral. Tres fases, ataques telegrafiados y ventanas de recuperación.
 *
 *  - Fase 1 «Caballero del Vacío»: tajo rápido (16-24) y golpe pesado telegrafiado (28-40).
 *  - Fase 2 «Ruptura Umbral»: añade zonas de peligro del Vacío (marcadas en el suelo antes de detonar).
 *  - Fase 3 «Último Juramento»: más presión (tajo doble, menos preparación, zonas más frecuentes) pero siempre
 *    con señal previa y ventana de recuperación tras el golpe pesado.
 *
 * Reglas fijas: sin invulnerabilidad permanente, sin curación (se bloquea heal() ), sin cadenas inevitables
 * (las zonas solo aparecen con el jefe libre y nunca durante la preparación de un golpe).
 */
public class CaballeroNetheriteVacio extends UmbralMonster {
    public static final int STATE_WINDUP_LIGHT = 1;
    public static final int STATE_WINDUP_HEAVY = 2;
    public static final int STATE_RECOVERY = 3;
    public static final int STATE_TRANSITION = 4;

    protected static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(CaballeroNetheriteVacio.class, EntityDataSerializers.INT);

    private static final DustParticleOptions POLVO_VACIO =
            new DustParticleOptions(new Vector3f(0.45F, 0.1F, 0.85F), 1.4F);
    private static final float[] ESCALA_VIDA = {1.0F, 1.7F, 2.25F, 2.75F};

    private static final class Zona {
        final Vec3 centro;
        int ticks;

        Zona(Vec3 centro, int ticks) {
            this.centro = centro;
            this.ticks = ticks;
        }
    }

    private final ServerBossEvent barra = new ServerBossEvent(
            Component.translatable("entity.mazmorra_umbral.caballero_netherite_vacio"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private final List<Zona> zonas = new ArrayList<>();
    private int enfriamientoAtaque = 30;
    private int enfriamientoPesado = 80;
    private int enfriamientoZonas = 160;
    private int golpesPendientes;
    private boolean fase2Anunciada;
    private boolean fase3Anunciada;

    public CaballeroNetheriteVacio(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 1);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GatedMeleeAttackGoal(this, 1.1D, true, () -> getState() == STATE_IDLE));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int[] tablaVida() {
        int base = UmbralConfig.JEFE_VIDA_BASE.get();
        return new int[]{base, base, base};
    }

    @Override
    protected List<Drop> drops() {
        return List.of(); // las recompensas del jefe las entrega el generador al completarse, una sola vez
    }

    @Override
    public boolean armsRaised() {
        int s = getState();
        return s == STATE_WINDUP_LIGHT || s == STATE_WINDUP_HEAVY;
    }

    /** Escala la vida según jugadores válidos (1 = 100 %, 2 = 170 %, 3 = 225 %, 4+ = 275 %). No escala el daño. */
    public void escalarParaJugadores(int jugadores) {
        int idx = Math.max(1, Math.min(4, jugadores)) - 1;
        double vida = UmbralConfig.JEFE_VIDA_BASE.get() * ESCALA_VIDA[idx];
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(vida);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    /** Sin curación de ningún tipo. */
    @Override
    public void heal(float amount) {
    }

    // ---- Barra de jefe ---------------------------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer jugador) {
        super.startSeenByPlayer(jugador);
        barra.addPlayer(jugador);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer jugador) {
        super.stopSeenByPlayer(jugador);
        barra.removePlayer(jugador);
    }

    // ---- IA --------------------------------------------------------------------------------------------------

    @Override
    public boolean doHurtTarget(Entity objetivo) {
        // El melé del objetivo solo inicia un tajo telegrafiado; el daño lo aplica el golpe al terminar la preparación.
        if (getState() == STATE_IDLE && enfriamientoAtaque <= 0) {
            iniciarTajo();
        }
        return false;
    }

    private int preparacionTajo() {
        return getPhase() >= 3 ? 9 : 12;
    }

    private int preparacionPesado() {
        return switch (getPhase()) {
            case 1 -> 36;
            case 2 -> 31;
            default -> 27;
        };
    }

    private void iniciarTajo() {
        setState(STATE_WINDUP_LIGHT);
        golpesPendientes = getPhase() >= 3 ? 2 : 1;
        this.getNavigation().stop();
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                1.0F, 0.7F);
    }

    private void iniciarPesado(LivingEntity objetivo) {
        setState(STATE_WINDUP_HEAVY);
        this.getNavigation().stop();
        this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5F, 1.5F);
        if (this.level() instanceof ServerLevel sl && objetivo != null) {
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, objetivo.getX(), objetivo.getY() + 2.3D, objetivo.getZ(),
                    4, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        barra.setProgress(this.getHealth() / this.getMaxHealth());
        if (enfriamientoAtaque > 0) enfriamientoAtaque--;
        if (enfriamientoPesado > 0) enfriamientoPesado--;
        if (enfriamientoZonas > 0) enfriamientoZonas--;

        actualizarFase();
        procesarZonas();

        LivingEntity objetivo = this.getTarget();
        switch (getState()) {
            case STATE_IDLE -> {
                if (objetivo != null) {
                    double d2 = this.distanceToSqr(objetivo);
                    if (enfriamientoPesado <= 0 && d2 < 6.0D * 6.0D && this.hasLineOfSight(objetivo)) {
                        iniciarPesado(objetivo);
                    } else if (getPhase() >= 2 && enfriamientoZonas <= 0 && zonas.isEmpty()) {
                        crearZonas();
                    }
                }
            }
            case STATE_WINDUP_LIGHT -> {
                this.getNavigation().stop();
                if (objetivo != null) {
                    this.getLookControl().setLookAt(objetivo, 60.0F, 60.0F);
                }
                if (stateTicks >= preparacionTajo()) {
                    tajo();
                    golpesPendientes--;
                    if (golpesPendientes > 0) {
                        setState(STATE_WINDUP_LIGHT);
                    } else {
                        setState(STATE_IDLE);
                        enfriamientoAtaque = getPhase() >= 3 ? 25 : 35;
                    }
                }
            }
            case STATE_WINDUP_HEAVY -> {
                this.getNavigation().stop();
                if (objetivo != null) {
                    this.getLookControl().setLookAt(objetivo, 40.0F, 40.0F);
                }
                if (this.level() instanceof ServerLevel sl) {
                    double progreso = Math.min(1.0D, stateTicks / (double) preparacionPesado());
                    CombatUtil.anillo(sl, this.position(), 1.5D + 3.5D * progreso, POLVO_VACIO, 18);
                    sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.5D, getZ(), 5, 0.4D, 0.6D, 0.4D, 0.3D);
                }
                if (stateTicks >= preparacionPesado()) {
                    golpePesado();
                    setState(STATE_RECOVERY);
                    enfriamientoPesado = getPhase() >= 3 ? 100 : 140;
                }
            }
            case STATE_RECOVERY -> {
                this.getNavigation().stop();
                if (stateTicks >= 40) {
                    setState(STATE_IDLE);
                    enfriamientoAtaque = 20;
                }
            }
            case STATE_TRANSITION -> {
                this.getNavigation().stop();
                if (this.level() instanceof ServerLevel sl && this.tickCount % 3 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0D, getZ(), 6, 0.6D, 0.8D, 0.6D, 0.05D);
                }
                if (stateTicks >= 60) {
                    setState(STATE_IDLE);
                    enfriamientoPesado = 60;
                    enfriamientoZonas = 120;
                }
            }
            default -> {
            }
        }
    }

    private void tajo() {
        float dano = 16.0F + this.random.nextFloat() * 8.0F; // 16-24
        for (LivingEntity e : CombatUtil.enCono(this, 4.0D, 60.0D, CombatUtil.SOLO_JUGADORES)) {
            if (e.hurt(this.damageSources().mobAttack(this), dano)) {
                e.knockback(0.5D, this.getX() - e.getX(), this.getZ() - e.getZ());
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                1.0F, 0.6F);
        if (this.level() instanceof ServerLevel sl) {
            Vec3 f = CombatUtil.mirada(this);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + f.x * 2.0D, getY() + 1.2D, getZ() + f.z * 2.0D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void golpePesado() {
        float dano = 28.0F + this.random.nextFloat() * 12.0F; // 28-40
        for (LivingEntity e : CombatUtil.enCono(this, 5.2D, 70.0D, CombatUtil.SOLO_JUGADORES)) {
            if (e.hurt(this.damageSources().mobAttack(this), dano)) {
                e.knockback(1.1D, this.getX() - e.getX(), this.getZ() - e.getZ());
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.7F);
        if (this.level() instanceof ServerLevel sl) {
            Vec3 f = CombatUtil.mirada(this);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX() + f.x * 2.5D, getY() + 0.5D, getZ() + f.z * 2.5D,
                    2, 0.5D, 0.1D, 0.5D, 0.0D);
        }
    }

    // ---- Fases -----------------------------------------------------------------------------------------------

    private void actualizarFase() {
        float ratio = this.getHealth() / this.getMaxHealth();
        int fase = ratio <= 0.33F ? 3 : ratio <= 0.66F ? 2 : 1;
        if (fase > getPhase()) {
            this.entityData.set(DATA_PHASE, fase);
            zonas.clear();
            setState(STATE_TRANSITION); // ventana de recuperación al cambiar de fase
            this.level().playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE,
                    1.0F, 0.6F);
            String clave = fase == 2 ? "message.mazmorra_umbral.jefe_fase2" : "message.mazmorra_umbral.jefe_fase3";
            if (this.level() instanceof ServerLevel sl) {
                for (ServerPlayer p : sl.getPlayers(pl -> pl.distanceToSqr(this) < 60.0D * 60.0D)) {
                    p.sendSystemMessage(Component.translatable(clave));
                }
            }
            if (fase == 2) fase2Anunciada = true;
            if (fase == 3) fase3Anunciada = true;
        }
    }

    // ---- Zonas del Vacío (fases 2 y 3) ------------------------------------------------------------------------

    private void crearZonas() {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        List<ServerPlayer> jugadores = sl.getPlayers(p -> CombatUtil.esJugadorValido(p)
                && !p.isCreative() && p.distanceToSqr(this) < 40.0D * 40.0D);
        int retraso = getPhase() >= 3 ? 45 : 50;
        int creadas = 0;
        for (ServerPlayer p : jugadores) {
            zonas.add(new Zona(p.position(), retraso));
            if (getPhase() >= 3) {
                // Segunda zona desplazada, siempre con el mismo aviso: obliga a moverse sin ser inevitable.
                double a = this.random.nextDouble() * Math.PI * 2.0D;
                zonas.add(new Zona(p.position().add(Math.cos(a) * 4.5D, 0.0D, Math.sin(a) * 4.5D), retraso + 10));
            }
            if (++creadas >= 3) {
                break;
            }
        }
        enfriamientoZonas = getPhase() >= 3 ? 120 : 180;
        if (!zonas.isEmpty()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE,
                    0.6F, 1.6F);
        }
    }

    private void procesarZonas() {
        if (zonas.isEmpty() || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        Iterator<Zona> it = zonas.iterator();
        while (it.hasNext()) {
            Zona z = it.next();
            z.ticks--;
            if (z.ticks % 2 == 0) {
                CombatUtil.anillo(sl, z.centro, 3.2D, POLVO_VACIO, 20);
                sl.sendParticles(ParticleTypes.PORTAL, z.centro.x, z.centro.y + 0.3D, z.centro.z, 4, 0.8D, 0.1D, 0.8D, 0.2D);
            }
            if (z.ticks <= 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, z.centro.x, z.centro.y + 0.5D, z.centro.z,
                        30, 1.2D, 1.0D, 1.2D, 0.1D);
                sl.playSound(null, z.centro.x, z.centro.y, z.centro.z, SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, 0.8F, 1.3F);
                for (LivingEntity e : CombatUtil.enRadio(z.centro, 3.2D, 2.0D, sl, CombatUtil.SOLO_JUGADORES)) {
                    e.hurt(this.damageSources().indirectMagic(this, this), 20.0F);
                }
                it.remove();
            }
        }
    }

    // ---- Daño ------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        int s = getState();
        if (s == STATE_RECOVERY || s == STATE_TRANSITION) {
            amount *= 1.5F; // ventanas claras para contraatacar
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_PHASE, Math.max(1, tag.getInt("Fase")));
        barra.setProgress(this.getHealth() / this.getMaxHealth());
    }
}
