package com.mazmorraumbral.item;

import net.minecraft.util.RandomSource;

/**
 * Cinco rarezas de crate y su matriz de probabilidad de categoría por tirada (propuesta de la especificación,
 * cada fila suma 100). El objeto concreto se elige después en la tabla de la categoría (data/.../loot_tables/crates).
 */
public enum CrateRarity {
    COMUN("comun", new int[]{70, 23, 6, 1, 0}),
    RARA("rara", new int[]{45, 35, 16, 4, 0}),
    EPICA("epica", new int[]{25, 35, 28, 10, 2}),
    LEGENDARIA("legendaria", new int[]{12, 23, 35, 25, 5}),
    MITICA("mitica", new int[]{5, 12, 28, 35, 20});

    private final String id;
    private final int[] pesos;

    CrateRarity(String id, int[] pesos) {
        this.id = id;
        this.pesos = pesos;
    }

    public String id() {
        return id;
    }

    /** Elige la categoría de una tirada según la fila de esta crate. Siempre devuelve una categoría válida. */
    public CrateRarity elegirCategoria(RandomSource azar) {
        int tirada = azar.nextInt(100);
        int acumulado = 0;
        CrateRarity[] todas = values();
        for (int i = 0; i < pesos.length; i++) {
            acumulado += pesos[i];
            if (tirada < acumulado) {
                return todas[i];
            }
        }
        return COMUN;
    }
}
