package com.mazmorraumbral.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UmbralConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue TAMANO_MAXIMO_MARCO;
    public static final ForgeConfigSpec.BooleanValue LOG_DEPURACION;

    public static final ForgeConfigSpec.IntValue SPAWNER_RADIO;
    public static final ForgeConfigSpec.IntValue MAX_SIMULTANEOS;
    public static final ForgeConfigSpec.BooleanValue SOLO_SUPERVIVENCIA;
    public static final ForgeConfigSpec.DoubleValue MULT_MOBS_POR_JUGADOR;

    public static final ForgeConfigSpec.IntValue FANTASMA_LIMITE_XP;
    public static final ForgeConfigSpec.IntValue FANTASMA_BASE_XP;

    public static final ForgeConfigSpec.IntValue JEFE_XP;
    public static final ForgeConfigSpec.DoubleValue CORAZON_PROBABILIDAD;
    public static final ForgeConfigSpec.IntValue JEFE_VIDA_BASE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("portal");
        TAMANO_MAXIMO_MARCO = b
                .comment("Tamaño máximo (ancho o alto del interior) del marco de Piedra del Umbral.")
                .defineInRange("tamanoMaximoMarco", 21, 4, 21);
        b.pop();

        b.push("oleadas");
        SPAWNER_RADIO = b
                .comment("Radio (en bloques) en el que un jugador válido activa un generador de oleadas.")
                .defineInRange("spawnerRadio", 16, 6, 48);
        MAX_SIMULTANEOS = b
                .comment("Tope global de enemigos simultáneos por generador (el piso puede usar menos).")
                .defineInRange("maxSimultaneos", 12, 2, 40);
        SOLO_SUPERVIVENCIA = b
                .comment("Si es true, solo los jugadores en supervivencia/aventura activan oleadas y reciben recompensas.",
                        "Ponlo en false para probar en creativo.")
                .define("soloSupervivencia", true);
        MULT_MOBS_POR_JUGADOR = b
                .comment("Cantidad extra de enemigos por cada jugador adicional dentro del radio (0.5 = +50 %).")
                .defineInRange("multMobsPorJugador", 0.5D, 0.0D, 2.0D);
        b.pop();

        b.push("fantasma");
        FANTASMA_LIMITE_XP = b
                .comment("Puntos reales de XP máximos que un Fantasma Espectral puede almacenar.")
                .defineInRange("limiteXp", 300, 20, 5000);
        FANTASMA_BASE_XP = b
                .comment("Recompensa base de XP que se suma al doble de lo robado cuando muere.")
                .defineInRange("baseXp", 30, 0, 1000);
        b.pop();

        b.push("jefe");
        JEFE_XP = b
                .comment("Puntos reales de XP por victoria válida sobre el jefe (una vez por jugador y expedición).")
                .defineInRange("xpVictoria", 1000, 0, 100000);
        CORAZON_PROBABILIDAD = b
                .comment("Probabilidad (0-1) de recibir el Corazón Dorado del Umbral al derrotar al jefe.",
                        "VALOR PROVISIONAL: la especificación no define cantidad ni probabilidad.")
                .defineInRange("corazonDoradoProbabilidad", 1.0D, 0.0D, 1.0D);
        JEFE_VIDA_BASE = b
                .comment("Vida base del jefe para 1 jugador (se escala 170 % / 225 % / 275 % para 2 / 3 / 4+).")
                .defineInRange("vidaBase", 2000, 200, 100000);
        b.pop();

        b.push("depuracion");
        LOG_DEPURACION = b
                .comment("Escribe en el log los eventos de activación, entrada, oleadas y salida.")
                .define("logDepuracion", false);
        b.pop();

        SPEC = b.build();
    }

    private UmbralConfig() {
    }
}
