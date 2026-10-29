package com.mazmorraumbral.state;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estado persistente del mod, guardado en el mundo (datos del Overworld).
 * Solo el servidor lo lee y lo escribe; el cliente nunca decide recompensas ni progreso.
 */
public class UmbralSavedData extends SavedData {
    private static final String NOMBRE = "mazmorra_umbral_estado";

    private boolean recetaBloqueada;
    private UUID primerActivador;
    private final Map<UUID, Registro> jugadores = new HashMap<>();

    public static UmbralSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(UmbralSavedData::cargar, UmbralSavedData::new, NOMBRE);
    }

    public boolean isRecetaBloqueada() {
        return recetaBloqueada;
    }

    /** Marca la primera activación exitosa y bloquea la receta inicial. Idempotente. */
    public void registrarActivacionExitosa(UUID jugador) {
        if (!recetaBloqueada) {
            recetaBloqueada = true;
            primerActivador = jugador;
            setDirty();
        }
    }

    public Registro registro(UUID jugador) {
        return jugadores.computeIfAbsent(jugador, k -> new Registro());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("recetaBloqueada", recetaBloqueada);
        if (primerActivador != null) {
            tag.putUUID("primerActivador", primerActivador);
        }
        ListTag lista = new ListTag();
        for (Map.Entry<UUID, Registro> e : jugadores.entrySet()) {
            CompoundTag t = e.getValue().guardar();
            t.putUUID("uuid", e.getKey());
            lista.add(t);
        }
        tag.put("jugadores", lista);
        return tag;
    }

    public static UmbralSavedData cargar(CompoundTag tag) {
        UmbralSavedData d = new UmbralSavedData();
        d.recetaBloqueada = tag.getBoolean("recetaBloqueada");
        if (tag.hasUUID("primerActivador")) {
            d.primerActivador = tag.getUUID("primerActivador");
        }
        ListTag lista = tag.getList("jugadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            d.jugadores.put(t.getUUID("uuid"), Registro.cargar(t));
        }
        return d;
    }

    /** Progreso de expedición de un jugador. Los campos de pisos/jefe se usarán en fases posteriores. */
    public static class Registro {
        public boolean expedicionIniciada;
        public int pisoActual = 1;
        public boolean victoriaRegistrada;
        public boolean recompensaFinalEntregada;
        public String retornoDimension;
        public long retornoPosicion;

        public void guardarRetorno(String dimension, BlockPos pos) {
            this.retornoDimension = dimension;
            this.retornoPosicion = pos.asLong();
        }

        public BlockPos posicionRetorno() {
            return BlockPos.of(retornoPosicion);
        }

        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putBoolean("expedicionIniciada", expedicionIniciada);
            t.putInt("pisoActual", pisoActual);
            t.putBoolean("victoriaRegistrada", victoriaRegistrada);
            t.putBoolean("recompensaFinalEntregada", recompensaFinalEntregada);
            if (retornoDimension != null) {
                t.putString("retornoDimension", retornoDimension);
                t.putLong("retornoPosicion", retornoPosicion);
            }
            return t;
        }

        static Registro cargar(CompoundTag t) {
            Registro r = new Registro();
            r.expedicionIniciada = t.getBoolean("expedicionIniciada");
            r.pisoActual = Math.max(1, t.getInt("pisoActual"));
            r.victoriaRegistrada = t.getBoolean("victoriaRegistrada");
            r.recompensaFinalEntregada = t.getBoolean("recompensaFinalEntregada");
            if (t.contains("retornoDimension", Tag.TAG_STRING)) {
                r.retornoDimension = t.getString("retornoDimension");
                r.retornoPosicion = t.getLong("retornoPosicion");
            }
            return r;
        }
    }
}
