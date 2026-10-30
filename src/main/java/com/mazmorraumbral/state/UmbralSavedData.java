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
 *
 * Una "expedición" es un ciclo completo de la mazmorra compartida. Cuando alguien reclama la bóveda y sale,
 * se solicita un reinicio; el reinicio solo se aplica cuando no queda nadie dentro de la dimensión.
 */
public class UmbralSavedData extends SavedData {
    private static final String NOMBRE = "mazmorra_umbral_estado";

    private boolean recetaBloqueada;
    private UUID primerActivador;
    private long expedicionId = 1L;
    private boolean reinicioPendiente;
    private int pisosConstruidos;
    private int pisosCompletados;
    private final Map<UUID, Registro> jugadores = new HashMap<>();

    public static UmbralSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(UmbralSavedData::cargar, UmbralSavedData::new, NOMBRE);
    }

    // ---- Receta inicial -------------------------------------------------------------------------------------

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

    // ---- Expedición y pisos ---------------------------------------------------------------------------------

    public long getExpedicionId() {
        return expedicionId;
    }

    public boolean isReinicioPendiente() {
        return reinicioPendiente;
    }

    public void solicitarReinicio() {
        if (!reinicioPendiente) {
            reinicioPendiente = true;
            setDirty();
        }
    }

    /** Inicia una expedición nueva: los pisos se reconstruyen y los generadores se reinician al activarse. */
    public void nuevaExpedicion() {
        expedicionId++;
        pisosConstruidos = 0;
        pisosCompletados = 0;
        reinicioPendiente = false;
        setDirty();
    }

    public boolean isPisoConstruido(int piso) {
        return (pisosConstruidos & (1 << piso)) != 0;
    }

    public void marcarPisoConstruido(int piso) {
        pisosConstruidos |= (1 << piso);
        setDirty();
    }

    public boolean isPisoCompletado(int piso) {
        return (pisosCompletados & (1 << piso)) != 0;
    }

    public void marcarPisoCompletado(int piso) {
        pisosCompletados |= (1 << piso);
        setDirty();
    }

    // ---- Jugadores ------------------------------------------------------------------------------------------

    /**
     * true si alguno de los jugadores conectados está a mitad de expedición (entró y aún no reclamó la bóveda).
     * Se usa para no reiniciar la mazmorra bajo los pies de alguien que murió o salió a mitad de camino.
     * Limitación: el progreso de jugadores desconectados no bloquea el reinicio.
     */
    public boolean hayProgresoActivo(java.util.Collection<UUID> conectados) {
        for (UUID uuid : conectados) {
            Registro r = registro(uuid);
            if (r.expedicionIniciada && !r.vaultReclamado) {
                return true;
            }
        }
        return false;
    }

    public Registro registro(UUID jugador) {
        Registro r = jugadores.computeIfAbsent(jugador, k -> new Registro());
        r.sincronizar(expedicionId);
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("recetaBloqueada", recetaBloqueada);
        if (primerActivador != null) {
            tag.putUUID("primerActivador", primerActivador);
        }
        tag.putLong("expedicionId", expedicionId);
        tag.putBoolean("reinicioPendiente", reinicioPendiente);
        tag.putInt("pisosConstruidos", pisosConstruidos);
        tag.putInt("pisosCompletados", pisosCompletados);
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
        d.expedicionId = tag.contains("expedicionId") ? tag.getLong("expedicionId") : 1L;
        d.reinicioPendiente = tag.getBoolean("reinicioPendiente");
        d.pisosConstruidos = tag.getInt("pisosConstruidos");
        d.pisosCompletados = tag.getInt("pisosCompletados");
        ListTag lista = tag.getList("jugadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            d.jugadores.put(t.getUUID("uuid"), Registro.cargar(t));
        }
        return d;
    }

    /** Progreso de expedición de un jugador. */
    public static class Registro {
        public long expedicionJugador;
        public boolean expedicionIniciada;
        public int pisoActual = 1;
        public boolean vaultReclamado;
        public boolean jefeRecompensado;
        public int victorias;
        public String retornoDimension;
        public long retornoPosicion;
        public String portalDimension;
        public long portalPosicion;

        /** Si la expedición del mundo cambió, el progreso por jugador de la anterior se descarta. */
        public void sincronizar(long idMundo) {
            if (expedicionJugador != idMundo) {
                expedicionJugador = idMundo;
                pisoActual = 1;
                vaultReclamado = false;
                jefeRecompensado = false;
                expedicionIniciada = false;
            }
        }

        public void guardarRetorno(String dimension, BlockPos pos) {
            this.retornoDimension = dimension;
            this.retornoPosicion = pos.asLong();
        }

        public BlockPos posicionRetorno() {
            return BlockPos.of(retornoPosicion);
        }

        public void guardarPortal(String dimension, BlockPos pos) {
            this.portalDimension = dimension;
            this.portalPosicion = pos.asLong();
        }

        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putLong("expedicionJugador", expedicionJugador);
            t.putBoolean("expedicionIniciada", expedicionIniciada);
            t.putInt("pisoActual", pisoActual);
            t.putBoolean("vaultReclamado", vaultReclamado);
            t.putBoolean("jefeRecompensado", jefeRecompensado);
            t.putInt("victorias", victorias);
            if (retornoDimension != null) {
                t.putString("retornoDimension", retornoDimension);
                t.putLong("retornoPosicion", retornoPosicion);
            }
            if (portalDimension != null) {
                t.putString("portalDimension", portalDimension);
                t.putLong("portalPosicion", portalPosicion);
            }
            return t;
        }

        static Registro cargar(CompoundTag t) {
            Registro r = new Registro();
            r.expedicionJugador = t.getLong("expedicionJugador");
            r.expedicionIniciada = t.getBoolean("expedicionIniciada");
            r.pisoActual = Math.max(1, t.getInt("pisoActual"));
            r.vaultReclamado = t.getBoolean("vaultReclamado");
            r.jefeRecompensado = t.getBoolean("jefeRecompensado");
            r.victorias = t.getInt("victorias");
            if (t.contains("retornoDimension", Tag.TAG_STRING)) {
                r.retornoDimension = t.getString("retornoDimension");
                r.retornoPosicion = t.getLong("retornoPosicion");
            }
            if (t.contains("portalDimension", Tag.TAG_STRING)) {
                r.portalDimension = t.getString("portalDimension");
                r.portalPosicion = t.getLong("portalPosicion");
            }
            return r;
        }
    }
}
