package com.mazmorraumbral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Marcas persistentes (datos NBT de Forge) que identifican a un enemigo como "válido" para un generador.
 * Los enemigos invocados por comandos, huevos o mods externos no llevan estas marcas y no dan recompensas.
 */
public final class UmbralTags {
    public static final String SPAWNER = "umbral_spawner";
    public static final String EXPEDICION = "umbral_exp";
    public static final String MULT_XP = "umbral_xp_mult";
    public static final String DANO_FLECHA = "umbral_arrow_dmg";

    private UmbralTags() {
    }

    public static void marcar(Entity e, BlockPos spawner, long expedicion) {
        CompoundTag d = e.getPersistentData();
        d.putLong(SPAWNER, spawner.asLong());
        d.putLong(EXPEDICION, expedicion);
    }

    public static boolean esDeMazmorra(Entity e) {
        return e.getPersistentData().contains(EXPEDICION);
    }

    public static long expedicionDe(Entity e) {
        return e.getPersistentData().getLong(EXPEDICION);
    }

    public static boolean tieneSpawner(Entity e) {
        return e.getPersistentData().contains(SPAWNER);
    }

    public static BlockPos spawnerDe(Entity e) {
        return BlockPos.of(e.getPersistentData().getLong(SPAWNER));
    }
}
