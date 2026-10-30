package com.mazmorraumbral.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public final class CombatUtil {
    public static final Predicate<LivingEntity> SOLO_JUGADORES = e -> e instanceof Player;

    private CombatUtil() {
    }

    public static boolean esJugadorValido(Entity e) {
        return e instanceof Player p && p.isAlive() && !p.isSpectator();
    }

    /** Dirección horizontal normalizada hacia la que mira la entidad. */
    public static Vec3 mirada(LivingEntity e) {
        Vec3 look = e.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0.0D, look.z);
        if (dir.lengthSqr() < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return dir.normalize();
    }

    /** Coseno del ángulo (horizontal) entre hacia dónde mira src y la dirección hacia other. */
    public static double cosHacia(LivingEntity src, Entity other) {
        Vec3 d = other.position().subtract(src.position());
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        if (dist < 0.01D) {
            return 1.0D;
        }
        Vec3 dh = new Vec3(d.x / dist, 0.0D, d.z / dist);
        return dh.dot(mirada(src));
    }

    /** Entidades dentro de un cono horizontal delante de src. */
    public static List<LivingEntity> enCono(LivingEntity src, double alcance, double semiAnguloGrados,
                                            Predicate<LivingEntity> filtro) {
        double cos = Math.cos(Math.toRadians(semiAnguloGrados));
        AABB caja = src.getBoundingBox().inflate(alcance, 2.0D, alcance);
        return src.level().getEntitiesOfClass(LivingEntity.class, caja, e -> {
            if (e == src || !filtro.test(e)) {
                return false;
            }
            Vec3 d = e.position().subtract(src.position());
            double dist = Math.sqrt(d.x * d.x + d.z * d.z);
            if (dist > alcance + e.getBbWidth() / 2.0D) {
                return false;
            }
            return cosHacia(src, e) >= cos;
        });
    }

    /** Entidades dentro de un radio horizontal (con margen vertical). */
    public static List<LivingEntity> enRadio(Vec3 centro, double radio, double margenY,
                                             net.minecraft.world.level.Level level, Predicate<LivingEntity> filtro) {
        AABB caja = new AABB(centro.x - radio, centro.y - margenY, centro.z - radio,
                centro.x + radio, centro.y + margenY + 1.0D, centro.z + radio);
        return level.getEntitiesOfClass(LivingEntity.class, caja, e -> {
            if (!filtro.test(e)) {
                return false;
            }
            double dx = e.getX() - centro.x;
            double dz = e.getZ() - centro.z;
            return dx * dx + dz * dz <= radio * radio;
        });
    }

    public static <T extends ParticleOptions> void anillo(ServerLevel level, Vec3 centro, double radio,
                                                          T particula, int puntos) {
        for (int i = 0; i < puntos; i++) {
            double a = (Math.PI * 2.0D) * i / puntos;
            level.sendParticles(particula, centro.x + Math.cos(a) * radio, centro.y + 0.1D,
                    centro.z + Math.sin(a) * radio, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Línea de partículas entre dos puntos (vínculos visibles). */
    public static <T extends ParticleOptions> void linea(ServerLevel level, Vec3 a, Vec3 b, T particula, int puntos) {
        for (int i = 0; i <= puntos; i++) {
            double t = i / (double) puntos;
            level.sendParticles(particula, a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t,
                    a.z + (b.z - a.z) * t, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
