package com.mazmorraumbral.world;

import com.mazmorraumbral.MazmorraUmbral;
import com.mazmorraumbral.blockentity.WaveSpawnerBlockEntity;
import com.mazmorraumbral.portal.UmbralPortalBlock;
import com.mazmorraumbral.registry.ModBlocks;
import com.mazmorraumbral.state.UmbralSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Construye los pisos por código dentro de la dimensión del Umbral. Los pisos 1-7 son arenas amplias (sin laberintos:
 * zona de entrada segura, divisoria con un arco ancho y arena con un generador de oleadas central); el piso 8 es la
 * bóveda. Se construyen al primer uso de cada expedición y se reconstruyen tras un reinicio (solo con la dimensión
 * vacía, ver UmbralEvents). Solo escribe dentro de la franja de cada piso de la dimensión propia del mod.
 */
public final class FloorBuilder {
    private static final int ALTO = 12;
    /** 2 = enviar al cliente; 16 = no recalcular formas vecinas (evita cadenas de actualización y drops). */
    private static final int FLAGS = 18;

    private record Paleta(BlockState pared, BlockState suelo, BlockState techo, BlockState pilar) {
    }

    private FloorBuilder() {
    }

    private static Paleta paleta(int piso) {
        switch (piso) {
            case 4:
                return new Paleta(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
                        Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
            case 5:
                return new Paleta(Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                        Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.PURPUR_PILLAR.defaultBlockState());
            case 6:
                return new Paleta(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                        Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
                        Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            case 7:
                return new Paleta(Blocks.RED_NETHER_BRICKS.defaultBlockState(), Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                        Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.CHISELED_NETHER_BRICKS.defaultBlockState());
            case 8:
                return new Paleta(Blocks.QUARTZ_BRICKS.defaultBlockState(), Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                        Blocks.QUARTZ_BRICKS.defaultBlockState(), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
            default:
                return new Paleta(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                        Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        }
    }

    /** Construye el piso si la expedición actual todavía no lo tiene. */
    public static void asegurar(ServerLevel nivel, UmbralSavedData datos, int piso) {
        if (piso < 1 || piso > UmbralLayout.MAX_PISO || datos.isPisoConstruido(piso)) {
            return;
        }
        BlockPos o = UmbralLayout.origen(piso);
        if (!nivel.getBlockState(o.offset(0, -1, 10)).isAir()) {
            limpiar(nivel, o);
        }
        if (piso == UmbralLayout.MAX_PISO) {
            construirBoveda(nivel, o, piso);
        } else {
            construirArena(nivel, o, piso);
        }
        datos.marcarPisoConstruido(piso);
        MazmorraUmbral.LOGGER.info("Piso {} del Umbral construido (expedición {})", piso, datos.getExpedicionId());
    }

    private static void limpiar(ServerLevel nivel, BlockPos o) {
        AABB caja = new AABB(o.getX() - 19, o.getY() - 2, o.getZ() - 5, o.getX() + 20, o.getY() + 15, o.getZ() + 48);
        for (Entity e : nivel.getEntitiesOfClass(Entity.class, caja, en -> !(en instanceof Player))) {
            e.discard();
        }
        BlockState aire = Blocks.AIR.defaultBlockState();
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -3; dz <= 45; dz++) {
                for (int dy = -1; dy <= ALTO + 1; dy++) {
                    set(nivel, o, dx, dy, dz, aire);
                }
            }
        }
    }

    private static void set(ServerLevel n, BlockPos o, int dx, int dy, int dz, BlockState s) {
        n.setBlock(o.offset(dx, dy, dz), s, FLAGS);
    }

    /** Caja hueca: suelo, techo, paredes, luces de techo y portal de regreso en la pared sur. */
    private static void caja(ServerLevel n, BlockPos o, Paleta p, int medioAncho, int zMin, int zMax) {
        for (int dx = -medioAncho - 1; dx <= medioAncho + 1; dx++) {
            for (int dz = zMin - 1; dz <= zMax + 1; dz++) {
                boolean borde = Math.abs(dx) == medioAncho + 1 || dz == zMin - 1 || dz == zMax + 1;
                set(n, o, dx, -1, dz, p.suelo());
                set(n, o, dx, ALTO, dz, p.techo());
                if (borde) {
                    for (int dy = 0; dy < ALTO; dy++) {
                        set(n, o, dx, dy, dz, p.pared());
                    }
                }
            }
        }
        BlockState luz = Blocks.SEA_LANTERN.defaultBlockState();
        for (int dx = -medioAncho + 4; dx <= medioAncho - 4; dx += 8) {
            for (int dz = zMin + 6; dz <= zMax - 4; dz += 8) {
                set(n, o, dx, ALTO, dz, luz);
            }
        }
        portalRetorno(n, o, zMin - 1);
    }

    private static void portalRetorno(ServerLevel n, BlockPos o, int zPared) {
        BlockState piedra = ModBlocks.PIEDRA_DEL_UMBRAL.get().defaultBlockState();
        BlockState portal = ModBlocks.PORTAL_DEL_UMBRAL.get().defaultBlockState()
                .setValue(UmbralPortalBlock.AXIS, Direction.Axis.X);
        for (int dy = -1; dy <= 3; dy++) {
            set(n, o, -2, dy, zPared, piedra);
            set(n, o, 1, dy, zPared, piedra);
        }
        for (int dx = -1; dx <= 0; dx++) {
            set(n, o, dx, -1, zPared, piedra);
            set(n, o, dx, 3, zPared, piedra);
            for (int dy = 0; dy <= 2; dy++) {
                set(n, o, dx, dy, zPared, portal);
            }
        }
    }

    private static void construirArena(ServerLevel n, BlockPos o, int piso) {
        Paleta p = paleta(piso);
        caja(n, o, p, 16, -2, 43);

        // Divisoria entre la zona de entrada (segura) y la arena, con un arco ancho.
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = 0; dy < ALTO; dy++) {
                if (Math.abs(dx) <= 3 && dy <= 5) {
                    continue;
                }
                set(n, o, dx, dy, 15, p.pared());
            }
        }

        // Pilares de cobertura.
        int[][] pilares = {{-8, 24}, {8, 24}, {-8, 36}, {8, 36}};
        for (int[] pil : pilares) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int dy = 0; dy < ALTO; dy++) {
                        set(n, o, pil[0] + dx, dy, pil[1] + dz, p.pilar());
                    }
                }
            }
        }

        BlockPos generador = o.offset(0, 0, 30);
        BlockPos centroSalida = o.offset(0, 0, 41);
        n.setBlock(generador, ModBlocks.GENERADOR_OLEADAS.get().defaultBlockState(), FLAGS);
        BlockState sello = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        for (BlockPos celda : UmbralLayout.celdasSalida(centroSalida)) {
            n.setBlock(celda, sello, FLAGS);
        }
        BlockEntity be = n.getBlockEntity(generador);
        if (be instanceof WaveSpawnerBlockEntity spawner) {
            spawner.configurar(piso, centroSalida);
        } else {
            MazmorraUmbral.LOGGER.error("El generador del piso {} no creó su entidad de bloque", piso);
        }

        // Cofres ambientales: dos en la zona de entrada y dos al fondo de la arena.
        cofre(n, o.offset(-14, 0, -1), Direction.SOUTH, piso);
        cofre(n, o.offset(14, 0, -1), Direction.SOUTH, piso);
        cofre(n, o.offset(-15, 0, 42), Direction.NORTH, piso);
        cofre(n, o.offset(15, 0, 42), Direction.NORTH, piso);
    }

    private static void construirBoveda(ServerLevel n, BlockPos o, int piso) {
        Paleta p = paleta(piso);
        caja(n, o, p, 12, -2, 24);
        for (int[] esquina : new int[][]{{-10, 6}, {10, 6}, {-10, 18}, {10, 18}}) {
            for (int dy = 0; dy < ALTO; dy++) {
                set(n, o, esquina[0], dy, esquina[1], p.pilar());
            }
        }
        // Plinto de la bóveda.
        for (int dx = -1; dx <= 1; dx++) {
            set(n, o, dx, 0, 17, Blocks.GOLD_BLOCK.defaultBlockState());
        }
        n.setBlock(o.offset(0, 1, 17), ModBlocks.BOVEDA_DEL_UMBRAL.get().defaultBlockState(), FLAGS);
    }

    private static void cofre(ServerLevel n, BlockPos pos, Direction frente, int piso) {
        BlockState estado = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, frente);
        n.setBlock(pos, estado, FLAGS);
        BlockEntity be = n.getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity contenedor) {
            contenedor.setLootTable(new ResourceLocation(MazmorraUmbral.MODID, "chests/piso_" + piso),
                    n.getRandom().nextLong());
        }
    }
}
