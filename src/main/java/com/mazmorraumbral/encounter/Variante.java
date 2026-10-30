package com.mazmorraumbral.encounter;

import com.mazmorraumbral.util.UmbralTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Variantes de criaturas vanilla (valores iniciales de diseño, sin balancear en el juego).
 * El multiplicador de XP solo se aplica a mobs marcados por un generador y muertos por un jugador.
 * Las criaturas personalizadas usan Variante.NORMAL (solo se marcan) y su propia variante interna.
 */
public enum Variante {
    NORMAL("normal", 0.0D, 0.0D, 1),
    XP("xp", 40.0D, 7.5D, 2),
    SUPERIOR("superior", 80.0D, 10.0D, 3),
    ELITE("elite", 120.0D, 14.0D, 4);

    private final String id;
    private final double vida;
    private final double dano;
    private final int multiplicadorXp;

    Variante(String id, double vida, double dano, int multiplicadorXp) {
        this.id = id;
        this.vida = vida;
        this.dano = dano;
        this.multiplicadorXp = multiplicadorXp;
    }

    public void aplicar(Mob mob, BlockPos spawner, long expedicion) {
        UmbralTags.marcar(mob, spawner, expedicion);
        mob.getPersistentData().putInt(UmbralTags.MULT_XP, multiplicadorXp);

        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        // El equipo aleatorio no se suelta: evita farmear armaduras en los generadores.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }
        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false);
        }
        AttributeInstance refuerzos = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (refuerzos != null) {
            refuerzos.setBaseValue(0.0D);
        }

        if (this == NORMAL) {
            return;
        }

        AttributeInstance vidaMax = mob.getAttribute(Attributes.MAX_HEALTH);
        if (vidaMax != null) {
            vidaMax.setBaseValue(vida);
        }
        mob.setHealth(mob.getMaxHealth());

        AttributeInstance danoAtq = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (danoAtq != null) {
            danoAtq.setBaseValue(dano);
        }

        mob.setCustomName(Component.translatable("variante.mazmorra_umbral." + id, mob.getType().getDescription()));
        if (this == ELITE) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 60, 0, false, false));
        }
    }
}
