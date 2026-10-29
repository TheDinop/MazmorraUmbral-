package com.mazmorraumbral.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UmbralConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TAMANO_MAXIMO_MARCO;
    public static final ForgeConfigSpec.BooleanValue LOG_DEPURACION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("portal");
        TAMANO_MAXIMO_MARCO = b
                .comment("Tamaño máximo (ancho o alto del interior) del marco de Piedra del Umbral.")
                .defineInRange("tamanoMaximoMarco", 21, 4, 21);
        b.pop();

        b.push("depuracion");
        LOG_DEPURACION = b
                .comment("Escribe en el log los eventos de activación, entrada y salida del portal.")
                .define("logDepuracion", false);
        b.pop();

        SPEC = b.build();
    }

    private UmbralConfig() {
    }
}
