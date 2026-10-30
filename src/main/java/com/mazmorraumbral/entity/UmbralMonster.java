package com.mazmorraumbral.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base de las criaturas personalizadas: variante (Normal / Extraordinaria / Legendaria), estado de habilidad
 * sincronizado con el cliente (para animaciones y señales visuales) y drops calculados en el servidor.
 *
 * Los drops NO usan Looting (el parámetro se ignora a propósito) para evitar multiplicar botín único.
 */
public abstract class UmbralMonster extends Monster {
    public static final int STATE_IDLE = 0;

    protected static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(UmbralMonster.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(UmbralMonster.class, EntityDataSerializers.INT);

    /** Ticks transcurridos en el estado actual (solo servidor). */
    protected int stateTicks;

    protected record Drop(Supplier<? extends Item> item, float chance, int min, int max) {
    }

    protected UmbralMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
        this.setPersistenceRequired();
    }

    /** Vida por variante: {normal, extraordinaria, legendaria}. */
    protected abstract int[] tablaVida();

    protected abstract List<Drop> drops();

    /** true cuando el modelo debe mostrar los brazos levantados (preparación de un ataque). */
    public boolean armsRaised() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, 0);
        this.entityData.define(DATA_STATE, STATE_IDLE);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    /** Aplica una variante (0 normal, 1 extraordinaria, 2 legendaria) con su vida correspondiente. */
    public void setVariant(int variant) {
        int v = Math.max(0, Math.min(2, variant));
        this.entityData.set(DATA_VARIANT, v);
        int[] tabla = tablaVida();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tabla[v]);
        this.setHealth(this.getMaxHealth());
        if (v > 0) {
            this.setCustomName(Component.translatable("variant.mazmorra_umbral.v" + v, this.getType().getDescription()));
            this.setCustomNameVisible(true);
        }
    }

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    protected void setState(int state) {
        if (this.entityData.get(DATA_STATE) != state) {
            this.entityData.set(DATA_STATE, state);
        }
        this.stateTicks = 0;
    }

    /** Multiplicador RELATIVO de probabilidad de materiales elegibles: +25 % / +50 %. */
    protected float multiplicadorBotin() {
        return switch (getVariant()) {
            case 1 -> 1.25F;
            case 2 -> 1.5F;
            default -> 1.0F;
        };
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.level().isClientSide || !recentlyHit) {
            return;
        }
        for (Drop d : drops()) {
            float probabilidad = Math.min(0.95F, d.chance() * multiplicadorBotin());
            if (this.random.nextFloat() < probabilidad) {
                int n = d.min() + this.random.nextInt(d.max() - d.min() + 1);
                this.spawnAtLocation(new ItemStack(d.item().get(), n));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variante", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_VARIANT, Math.max(0, Math.min(2, tag.getInt("Variante"))));
    }
}
