package com.mazmorraumbral.event;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.entity.ArcanistaOcaso;
import com.mazmorraumbral.entity.CaballeroMarchito;
import com.mazmorraumbral.entity.CaballeroNetheriteVacio;
import com.mazmorraumbral.entity.CustodioReliquias;
import com.mazmorraumbral.entity.DraconidoOcaso;
import com.mazmorraumbral.entity.FantasmaEspectral;
import com.mazmorraumbral.entity.VerdugoCarmesi;
import com.mazmorraumbral.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del bus del mod (registro de atributos). */
@Mod.EventBusSubscriber(modid = MazmorraUmbral.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {
    }

    @SubscribeEvent
    public static void registrarAtributos(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FANTASMA_ESPECTRAL.get(), FantasmaEspectral.createAttributes().build());
        event.put(ModEntities.CABALLERO_MARCHITO.get(), CaballeroMarchito.createAttributes().build());
        event.put(ModEntities.VERDUGO_CARMESI.get(), VerdugoCarmesi.createAttributes().build());
        event.put(ModEntities.ARCANISTA_OCASO.get(), ArcanistaOcaso.createAttributes().build());
        event.put(ModEntities.DRACONIDO_OCASO.get(), DraconidoOcaso.createAttributes().build());
        event.put(ModEntities.CUSTODIO_RELIQUIAS.get(), CustodioReliquias.createAttributes().build());
        event.put(ModEntities.CABALLERO_NETHERITE_VACIO.get(), CaballeroNetheriteVacio.createAttributes().build());
    }
}
