package com.mazmorraumbral.portal;

import com.mazmorraumbral.config.UmbralConfig;
import com.mazmorraumbral.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Detecta un marco vertical rectangular de Piedra del Umbral (interior mínimo 2x3, todo aire).
 * Las esquinas del marco no son obligatorias, igual que en el portal del Nether.
 */
public final class PortalFrame {
    public final Direction.Axis axis;
    public final List<BlockPos> interior;

    private PortalFrame(Direction.Axis axis, List<BlockPos> interior) {
        this.axis = axis;
        this.interior = interior;
    }

    public static Optional<PortalFrame> find(Level level, BlockPos clicked) {
        int max = UmbralConfig.TAMANO_MAXIMO_MARCO.get();
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != axis && dir.getAxis() != Direction.Axis.Y) {
                    continue;
                }
                BlockPos start = clicked.relative(dir);
                if (!level.getBlockState(start).isAir()) {
                    continue;
                }
                PortalFrame frame = tryBuild(level, start, axis, max);
                if (frame != null) {
                    return Optional.of(frame);
                }
            }
        }
        return Optional.empty();
    }

    private static PortalFrame tryBuild(Level level, BlockPos start, Direction.Axis axis, int max) {
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction[] dirs = {along, along.getOpposite(), Direction.UP, Direction.DOWN};

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        int cap = max * max;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction d : dirs) {
                BlockPos n = pos.relative(d);
                if (visited.contains(n)) {
                    continue;
                }
                if (level.isOutsideBuildHeight(n)) {
                    return null;
                }
                BlockState state = level.getBlockState(n);
                if (state.isAir()) {
                    visited.add(n);
                    if (visited.size() > cap) {
                        return null;
                    }
                    queue.add(n);
                } else if (!state.is(ModBlocks.PIEDRA_DEL_UMBRAL.get())) {
                    return null;
                }
            }
        }

        int minA = Integer.MAX_VALUE, maxA = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos p : visited) {
            int a = axis == Direction.Axis.X ? p.getX() : p.getZ();
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        int w = maxA - minA + 1;
        int h = maxY - minY + 1;
        if (w < 2 || h < 3 || w > max || h > max || visited.size() != w * h) {
            return null;
        }
        return new PortalFrame(axis, new ArrayList<>(visited));
    }

    public void activate(Level level) {
        BlockState portal = ModBlocks.PORTAL_DEL_UMBRAL.get().defaultBlockState()
                .setValue(UmbralPortalBlock.AXIS, axis);
        for (BlockPos pos : interior) {
            // Flags 18 = actualizar cliente (2) + no recalcular formas vecinas (16)
            level.setBlock(pos, portal, 18);
        }
    }
}
