package com.mazmorraumbral.event;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.registry.ModDimensions;
import com.mazmorraumbral.state.UmbralSavedData;
import com.mazmorraumbral.util.UmbralTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

/** Eventos de juego (bus de Forge). */
public class UmbralEvents {

    /**
     * Multiplica la XP solo si el mob fue creado por un generador (lleva la marca de mazmorra) y lo mató un jugador.
     * Mobs invocados por comandos, huevos u otros mods no llevan la marca y no cambian.
     */
    @SubscribeEvent
    public void alCalcularExperiencia(LivingExperienceDropEvent event) {
        LivingEntity entidad = event.getEntity();
        if (!UmbralTags.esDeMazmorra(entidad) || event.getAttackingPlayer() == null) {
            return;
        }
        CompoundTag datos = entidad.getPersistentData();
        if (!datos.contains(UmbralTags.MULT_XP)) {
            return;
        }
        int multiplicador = Math.max(1, datos.getInt(UmbralTags.MULT_XP));
        event.setDroppedExperience(event.getDroppedExperience() * multiplicador);
    }

    /**
     * Reinicio controlado de la mazmorra: solo cuando alguien ya reclamó la bóveda, la dimensión está vacía y nadie
     * conectado está a mitad de expedición. Nunca se reconstruye con jugadores dentro. Los pisos se reconstruyen
     * perezosamente al volver a entrar (FloorBuilder), así que aquí solo se cambia el identificador de expedición.
     */
    @SubscribeEvent
    public void alTickDelServidor(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 100 != 0) {
            return;
        }
        UmbralSavedData datos = UmbralSavedData.get(server);
        if (!datos.isReinicioPendiente()) {
            return;
        }
        ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL);
        if (umbral != null && !umbral.players().isEmpty()) {
            return;
        }
        List<UUID> conectados = server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList();
        if (datos.hayProgresoActivo(conectados)) {
            return;
        }
        datos.nuevaExpedicion();
        MazmorraUmbral.LOGGER.info("Mazmorra del Umbral reiniciada: expedición {}", datos.getExpedicionId());
    }
}
