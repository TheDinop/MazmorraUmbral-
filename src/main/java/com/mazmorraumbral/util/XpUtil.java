package com.mazmorraumbral.util;

import net.minecraft.world.entity.player.Player;

/** Operaciones con puntos REALES de experiencia (no niveles aproximados). */
public final class XpUtil {
    private XpUtil() {
    }

    public static int puntosParaNivel(int nivel) {
        if (nivel <= 16) {
            return nivel * nivel + 6 * nivel;
        }
        if (nivel <= 31) {
            return (int) (2.5D * nivel * nivel - 40.5D * nivel + 360.0D);
        }
        return (int) (4.5D * nivel * nivel - 162.5D * nivel + 2220.0D);
    }

    /** Total de puntos de XP del jugador (nivel + progreso de la barra). */
    public static int total(Player p) {
        return puntosParaNivel(p.experienceLevel)
                + Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
    }

    /** Fija el total exacto de puntos reiniciando nivel y barra. Solo debe llamarse en el servidor. */
    public static void fijarTotal(Player p, int puntos) {
        p.experienceLevel = 0;
        p.experienceProgress = 0.0F;
        p.totalExperience = 0;
        if (puntos > 0) {
            p.giveExperiencePoints(puntos);
        } else {
            // Forzar que el cliente reciba el estado reiniciado.
            p.giveExperiencePoints(0);
        }
    }

    public static void quitar(Player p, int puntos) {
        if (puntos <= 0) {
            return;
        }
        fijarTotal(p, Math.max(0, total(p) - puntos));
    }

    public static void dar(Player p, int puntos) {
        if (puntos > 0) {
            p.giveExperiencePoints(puntos);
        }
    }
}
