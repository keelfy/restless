package dev.keelfy.restless.ward;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a floor cell is shut in by salt: a flood fill that has to close within a cell budget and has to touch salt.
 * Worn salt is a gap: it stops nothing until it is laid again.
 */
public final class Enclosure {
    public enum Cell {
        OPEN,
        SALT,
        WORN,
        WALL
    }

    @FunctionalInterface
    public interface Floor {
        Cell at(int x, int z);
    }

    private Enclosure() {}

    /**
     * The start cell counts as open even when it holds salt. Walls alone do not shelter: a plain room is not warded.
     */
    public static boolean sealed(int x, int z, Floor floor, int maxCells) {
        List<int[]> salt = flood(x, z, floor, maxCells);
        return salt != null && !salt.isEmpty();
    }

    /** The salt cells that shut the start cell in, empty when it is not sealed. These are the cells a stop wears. */
    public static List<int[]> ring(int x, int z, Floor floor, int maxCells) {
        List<int[]> salt = flood(x, z, floor, maxCells);
        return salt == null ? List.of() : salt;
    }

    /** Whether a ring that has stopped {@code stops} events is worn out, at a limit of {@code wearStops}. */
    public static boolean wornOut(int stops, int wearStops) {
        return stops >= wearStops;
    }

    /** Whether the cell is shut in by walls and salt together, salt or not. */
    public static boolean closed(int x, int z, Floor floor, int maxCells) {
        return flood(x, z, floor, maxCells) != null;
    }

    /** The salt cells touched, or null when the fill escapes the budget. */
    private static List<int[]> flood(int x, int z, Floor floor, int maxCells) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {x, z});
        seen.add(key(x, z));
        List<int[]> salt = new ArrayList<>();
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = at[0] + step[0];
                int nz = at[1] + step[1];
                if (!seen.add(key(nx, nz))) {
                    continue;
                }
                switch (floor.at(nx, nz)) {
                    case SALT -> salt.add(new int[] {nx, nz});
                    case WALL -> {}
                    case OPEN, WORN -> {
                        if (seen.size() > maxCells) {
                            return null;
                        }
                        queue.add(new int[] {nx, nz});
                    }
                }
            }
        }
        return salt;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
