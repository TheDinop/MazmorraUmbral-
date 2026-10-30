package com.mazmorraumbral.encounter;

import com.mazmorraumbral.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Composición de oleadas por piso. Son propuestas iniciales, pendientes de probar y ajustar.
 * Se construyen al llamar al método (no al cargar la clase) porque los tipos de entidad se registran más tarde.
 */
public final class FloorSpec {

    /**
     * @param variante         variante de las criaturas vanilla (NORMAL para las personalizadas)
     * @param varianteCriatura 0 normal, 1 extraordinaria, 2 legendaria; solo para criaturas personalizadas
     */
    public record Entrada(EntityType<? extends Mob> tipo, int cantidad, Variante variante, int varianteCriatura) {
    }

    private FloorSpec() {
    }

    private static Entrada v(EntityType<? extends Mob> tipo, int cantidad, Variante variante) {
        return new Entrada(tipo, cantidad, variante, 0);
    }

    private static Entrada c(EntityType<? extends Mob> tipo, int cantidad, int variante) {
        return new Entrada(tipo, cantidad, Variante.NORMAL, variante);
    }

    public static List<List<Entrada>> olas(int piso) {
        switch (piso) {
            case 1:
                return List.of(
                        List.of(v(EntityType.ZOMBIE, 3, Variante.NORMAL), v(EntityType.SKELETON, 2, Variante.NORMAL)),
                        List.of(v(EntityType.ZOMBIE, 3, Variante.NORMAL), v(EntityType.SPIDER, 2, Variante.NORMAL),
                                v(EntityType.SKELETON, 2, Variante.NORMAL)),
                        List.of(v(EntityType.ZOMBIE, 4, Variante.NORMAL), v(EntityType.SKELETON, 3, Variante.NORMAL),
                                v(EntityType.SPIDER, 2, Variante.NORMAL)));
            case 2:
                return List.of(
                        List.of(v(EntityType.ZOMBIE, 3, Variante.XP), v(EntityType.SKELETON, 2, Variante.XP)),
                        List.of(v(EntityType.ZOMBIE, 3, Variante.XP), v(EntityType.SPIDER, 2, Variante.XP),
                                v(EntityType.SKELETON, 2, Variante.XP)),
                        List.of(v(EntityType.ZOMBIE, 4, Variante.XP), v(EntityType.SKELETON, 3, Variante.XP),
                                v(EntityType.SPIDER, 3, Variante.XP)));
            case 3:
                return List.of(
                        List.of(v(EntityType.ZOMBIE, 3, Variante.SUPERIOR), v(EntityType.SKELETON, 2, Variante.SUPERIOR)),
                        List.of(v(EntityType.SPIDER, 3, Variante.SUPERIOR), v(EntityType.ZOMBIE, 3, Variante.XP)),
                        List.of(v(EntityType.ZOMBIE, 2, Variante.ELITE), v(EntityType.SKELETON, 3, Variante.SUPERIOR),
                                v(EntityType.SPIDER, 2, Variante.SUPERIOR)),
                        List.of(v(EntityType.ZOMBIE, 3, Variante.ELITE), v(EntityType.SKELETON, 2, Variante.ELITE),
                                v(EntityType.SPIDER, 2, Variante.ELITE)));
            case 4:
                return List.of(
                        List.of(c(ModEntities.CABALLERO_MARCHITO.get(), 2, 0)),
                        List.of(c(ModEntities.CABALLERO_MARCHITO.get(), 1, 0), c(ModEntities.VERDUGO_CARMESI.get(), 1, 0)),
                        List.of(c(ModEntities.VERDUGO_CARMESI.get(), 2, 0), c(ModEntities.CABALLERO_MARCHITO.get(), 2, 1)));
            case 5:
                return List.of(
                        List.of(c(ModEntities.ARCANISTA_OCASO.get(), 2, 0)),
                        List.of(c(ModEntities.DRACONIDO_OCASO.get(), 1, 0), c(ModEntities.ARCANISTA_OCASO.get(), 1, 0),
                                c(ModEntities.CUSTODIO_RELIQUIAS.get(), 1, 0)),
                        List.of(c(ModEntities.DRACONIDO_OCASO.get(), 2, 0), c(ModEntities.CUSTODIO_RELIQUIAS.get(), 1, 1),
                                c(ModEntities.ARCANISTA_OCASO.get(), 2, 1)));
            case 6:
                return List.of(
                        List.of(c(ModEntities.FANTASMA_ESPECTRAL.get(), 2, 0), c(ModEntities.CABALLERO_MARCHITO.get(), 1, 0)),
                        List.of(c(ModEntities.VERDUGO_CARMESI.get(), 1, 1), c(ModEntities.ARCANISTA_OCASO.get(), 1, 0),
                                c(ModEntities.CUSTODIO_RELIQUIAS.get(), 1, 0)),
                        List.of(c(ModEntities.DRACONIDO_OCASO.get(), 1, 1), c(ModEntities.FANTASMA_ESPECTRAL.get(), 2, 1),
                                c(ModEntities.CABALLERO_MARCHITO.get(), 1, 1)),
                        List.of(c(ModEntities.VERDUGO_CARMESI.get(), 1, 2), c(ModEntities.ARCANISTA_OCASO.get(), 1, 1),
                                c(ModEntities.DRACONIDO_OCASO.get(), 1, 0), c(ModEntities.CUSTODIO_RELIQUIAS.get(), 1, 1),
                                c(ModEntities.FANTASMA_ESPECTRAL.get(), 1, 2)));
            case 7:
                return List.of(List.of(c(ModEntities.CABALLERO_NETHERITE_VACIO.get(), 1, 0)));
            default:
                return List.of();
        }
    }
}
