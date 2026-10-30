package com.mazmorraumbral.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Disposición fija de la dimensión del Umbral: cada piso ocupa una franja propia en el eje Z.
 * El número de piso se deduce siempre de una posición, sin guardar nada extra.
 */
public final class UmbralLayout {
    public static final int MAX_PISO = 8;
    public static final int Y = 64;

    private static final int Z_BASE = 1000;
    private static final int ALTO_PISO = 128;

    private UmbralLayout() {
    }

    /** Centro de la pared sur del piso, a la altura del suelo transitable. */
    public static BlockPos origen(int piso) {
        return new BlockPos(0, Y, Z_BASE + (piso - 1) * ALTO_PISO);
    }

    public static int pisoDe(BlockPos pos) {
        return Math.floorDiv(pos.getZ() - Z_BASE + ALTO_PISO / 2, ALTO_PISO) + 1;
    }

    /** true si la posición cae dentro de las franjas de los pisos (y no en la plataforma antigua de la fase 1). */
    public static boolean estaEnPisos(BlockPos pos) {
        int piso = pisoDe(pos);
        return piso >= 1 && piso <= MAX_PISO && pos.getZ() >= Z_BASE - 10;
    }

    public static Vec3 llegada(int piso) {
        BlockPos o = origen(piso);
        return new Vec3(o.getX() + 0.5D, o.getY(), o.getZ() + 2.5D);
    }

    /** Celdas de 3 de ancho x 2 de alto que ocupan el sello y, después, la plataforma de ascenso. */
    public static List<BlockPos> celdasSalida(BlockPos centro) {
        List<BlockPos> celdas = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                celdas.add(centro.offset(dx, dy, 0));
            }
        }
        return celdas;
    }
}
