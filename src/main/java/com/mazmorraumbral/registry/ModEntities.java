package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.entity.ArcaneBolt;
import com.mazmorraumbral.entity.ArcanistaOcaso;
import com.mazmorraumbral.entity.CaballeroMarchito;
import com.mazmorraumbral.entity.CaballeroNetheriteVacio;
import com.mazmorraumbral.entity.CustodioReliquias;
import com.mazmorraumbral.entity.DraconidoOcaso;
import com.mazmorraumbral.entity.FantasmaEspectral;
import com.mazmorraumbral.entity.VerdugoCarmesi;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MazmorraUmbral.MODID);

    public static final RegistryObject<EntityType<FantasmaEspectral>> FANTASMA_ESPECTRAL = ENTITIES.register(
            "fantasma_espectral", () -> EntityType.Builder.of(FantasmaEspectral::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(MazmorraUmbral.MODID + ":fantasma_espectral"));

    public static final RegistryObject<EntityType<CaballeroMarchito>> CABALLERO_MARCHITO = ENTITIES.register(
            "caballero_marchito", () -> EntityType.Builder.of(CaballeroMarchito::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F).clientTrackingRange(10)
                    .build(MazmorraUmbral.MODID + ":caballero_marchito"));

    public static final RegistryObject<EntityType<VerdugoCarmesi>> VERDUGO_CARMESI = ENTITIES.register(
            "verdugo_carmesi", () -> EntityType.Builder.of(VerdugoCarmesi::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.2F).clientTrackingRange(10)
                    .build(MazmorraUmbral.MODID + ":verdugo_carmesi"));

    public static final RegistryObject<EntityType<ArcanistaOcaso>> ARCANISTA_OCASO = ENTITIES.register(
            "arcanista_ocaso", () -> EntityType.Builder.of(ArcanistaOcaso::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(MazmorraUmbral.MODID + ":arcanista_ocaso"));

    public static final RegistryObject<EntityType<DraconidoOcaso>> DRACONIDO_OCASO = ENTITIES.register(
            "draconido_ocaso", () -> EntityType.Builder.of(DraconidoOcaso::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F).clientTrackingRange(10)
                    .build(MazmorraUmbral.MODID + ":draconido_ocaso"));

    public static final RegistryObject<EntityType<CustodioReliquias>> CUSTODIO_RELIQUIAS = ENTITIES.register(
            "custodio_reliquias", () -> EntityType.Builder.of(CustodioReliquias::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.1F).clientTrackingRange(10)
                    .build(MazmorraUmbral.MODID + ":custodio_reliquias"));

    public static final RegistryObject<EntityType<CaballeroNetheriteVacio>> CABALLERO_NETHERITE_VACIO =
            ENTITIES.register("caballero_netherite_vacio",
                    () -> EntityType.Builder.of(CaballeroNetheriteVacio::new, MobCategory.MONSTER)
                            .sized(1.0F, 2.8F).fireImmune().clientTrackingRange(12)
                            .build(MazmorraUmbral.MODID + ":caballero_netherite_vacio"));

    public static final RegistryObject<EntityType<ArcaneBolt>> ARCANE_BOLT = ENTITIES.register(
            "arcane_bolt", () -> EntityType.Builder.<ArcaneBolt>of(ArcaneBolt::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10)
                    .build(MazmorraUmbral.MODID + ":arcane_bolt"));

    private ModEntities() {
    }
}
