package com.mazmorraumbral.registry;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.item.CrateItem;
import com.mazmorraumbral.item.CrateRarity;
import com.mazmorraumbral.item.XpCrateItem;
import com.mazmorraumbral.item.XpStorageTomeItem;
import com.mazmorraumbral.item.XpTomeItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MazmorraUmbral.MODID);

    // ---- Bloques --------------------------------------------------------------------------------------------
    public static final RegistryObject<Item> PIEDRA_DEL_UMBRAL = ITEMS.register("piedra_del_umbral",
            () -> new BlockItem(ModBlocks.PIEDRA_DEL_UMBRAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> GENERADOR_OLEADAS = ITEMS.register("generador_oleadas",
            () -> new BlockItem(ModBlocks.GENERADOR_OLEADAS.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ASCENSO_DEL_UMBRAL = ITEMS.register("ascenso_del_umbral",
            () -> new BlockItem(ModBlocks.ASCENSO_DEL_UMBRAL.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BOVEDA_DEL_UMBRAL = ITEMS.register("boveda_del_umbral",
            () -> new BlockItem(ModBlocks.BOVEDA_DEL_UMBRAL.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // ---- Acceso ---------------------------------------------------------------------------------------------
    public static final RegistryObject<Item> FRAGMENTO_DEL_UMBRAL = ITEMS.register("fragmento_del_umbral",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> LLAVE_UMBRAL = ITEMS.register("llave_umbral",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    // ---- Materiales de criaturas ----------------------------------------------------------------------------
    public static final RegistryObject<Item> FRAGMENTO_DE_ALMA = material("fragmento_de_alma", Rarity.UNCOMMON);
    public static final RegistryObject<Item> NUCLEO_DEUDA_ESPECTRAL = material("nucleo_deuda_espectral", Rarity.RARE);
    public static final RegistryObject<Item> FRAGMENTO_ACERO_RUNICO = material("fragmento_acero_runico", Rarity.UNCOMMON);
    public static final RegistryObject<Item> SELLO_ORDEN_MARCHITA = material("sello_orden_marchita", Rarity.RARE);
    public static final RegistryObject<Item> INSIGNIA_EJECUCION = material("insignia_ejecucion", Rarity.RARE);
    public static final RegistryObject<Item> TENDON_CARMESI = material("tendon_carmesi", Rarity.UNCOMMON);
    public static final RegistryObject<Item> POLVO_ARCANO = material("polvo_arcano", Rarity.UNCOMMON);
    public static final RegistryObject<Item> CATALIZADOR_MAGICO = material("catalizador_magico", Rarity.RARE);
    public static final RegistryObject<Item> PRISMA_CONVERGENCIA = material("prisma_convergencia", Rarity.RARE);
    public static final RegistryObject<Item> MEMBRANA_ESPECTRAL = material("membrana_espectral", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ESCAMAS = material("escamas", Rarity.UNCOMMON);
    public static final RegistryObject<Item> TENDON_ALA_UMBRIA = material("tendon_ala_umbria", Rarity.RARE);
    public static final RegistryObject<Item> NUCLEO_RELIQUIA = material("nucleo_reliquia", Rarity.RARE);
    public static final RegistryObject<Item> MATRIZ_PROTECCION_ANTIGUA = material("matriz_proteccion_antigua", Rarity.RARE);

    /** Material exclusivo del jefe: no fabricable y no aparece en crates. */
    public static final RegistryObject<Item> CORAZON_DORADO_UMBRAL = ITEMS.register("corazon_dorado_umbral",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // ---- Consumibles de experiencia -------------------------------------------------------------------------
    public static final RegistryObject<Item> TOMO_EXPERIENCIA = ITEMS.register("tomo_experiencia",
            () -> new XpTomeItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TOMO_ALMACENAMIENTO_XP = ITEMS.register("tomo_almacenamiento_xp",
            () -> new XpStorageTomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** Efectos propuestos: Visión nocturna 5 min, Absorción I 2 min, Prisa II 3 min, Saturación 3 min. */
    public static final RegistryObject<Item> ZANAHORIA_DORADA_UMBRAL = ITEMS.register("zanahoria_dorada_umbral",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).food(new FoodProperties.Builder()
                    .nutrition(8).saturationMod(1.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.SATURATION, 3600, 0), 1.0F)
                    .build())));

    // ---- Crates ---------------------------------------------------------------------------------------------
    public static final RegistryObject<Item> CRATE_COMUN = crate("crate_comun", CrateRarity.COMUN, Rarity.COMMON);
    public static final RegistryObject<Item> CRATE_RARA = crate("crate_rara", CrateRarity.RARA, Rarity.UNCOMMON);
    public static final RegistryObject<Item> CRATE_EPICA = crate("crate_epica", CrateRarity.EPICA, Rarity.RARE);
    public static final RegistryObject<Item> CRATE_LEGENDARIA = crate("crate_legendaria", CrateRarity.LEGENDARIA, Rarity.EPIC);
    public static final RegistryObject<Item> CRATE_MITICA = crate("crate_mitica", CrateRarity.MITICA, Rarity.EPIC);
    public static final RegistryObject<Item> CRATE_XP = ITEMS.register("crate_xp",
            () -> new XpCrateItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // ---- Huevos de aparición (solo para pruebas: los enemigos así creados NO dan recompensas de oleada) -------
    public static final RegistryObject<Item> HUEVO_FANTASMA = ITEMS.register("huevo_fantasma_espectral",
            () -> new ForgeSpawnEggItem(ModEntities.FANTASMA_ESPECTRAL, 0x9FB4D6, 0x3E3A6E, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_CABALLERO = ITEMS.register("huevo_caballero_marchito",
            () -> new ForgeSpawnEggItem(ModEntities.CABALLERO_MARCHITO, 0x5B5B4A, 0x8A7A3A, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_VERDUGO = ITEMS.register("huevo_verdugo_carmesi",
            () -> new ForgeSpawnEggItem(ModEntities.VERDUGO_CARMESI, 0x7A0F14, 0x1B1B1B, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_ARCANISTA = ITEMS.register("huevo_arcanista_ocaso",
            () -> new ForgeSpawnEggItem(ModEntities.ARCANISTA_OCASO, 0x4B2A7A, 0xC9A0FF, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_DRACONIDO = ITEMS.register("huevo_draconido_ocaso",
            () -> new ForgeSpawnEggItem(ModEntities.DRACONIDO_OCASO, 0x2B1F3F, 0x6FE0D0, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_CUSTODIO = ITEMS.register("huevo_custodio_reliquias",
            () -> new ForgeSpawnEggItem(ModEntities.CUSTODIO_RELIQUIAS, 0x8C6B2A, 0xE9D27A, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_JEFE = ITEMS.register("huevo_caballero_netherite_vacio",
            () -> new ForgeSpawnEggItem(ModEntities.CABALLERO_NETHERITE_VACIO, 0x1A1A22, 0x7A3CE0, new Item.Properties()));

    private static RegistryObject<Item> material(String nombre, Rarity rareza) {
        return ITEMS.register(nombre, () -> new Item(new Item.Properties().rarity(rareza)));
    }

    private static RegistryObject<Item> crate(String nombre, CrateRarity categoria, Rarity rareza) {
        Supplier<Item> s = () -> new CrateItem(new Item.Properties().stacksTo(16).rarity(rareza), categoria);
        return ITEMS.register(nombre, s);
    }

    private ModItems() {
    }
}
