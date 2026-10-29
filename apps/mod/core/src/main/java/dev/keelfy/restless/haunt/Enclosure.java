package dev.keelfy.restless.haunt;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Whether a floor cell is shut in by salt: a flood fill that has to close within a cell budget and has to touch salt. */
public final class Enclosure {
    public enum Cell { OPEN, SALT, WALL }

    @FunctionalInterface
    public interface Floor {
        Cell at(int x, int z);
    }

    private Enclosure() {}

    /** The start cell counts as open even when it holds salt. Walls alone do not shelter: a plain room is not warded. */
    public static boolean sealed(int x, int z, Floor floor, int maxCells) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {x, z});
        seen.add(key(x, z));
        boolean touchedSalt = false;
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = at[0] + step[0];
                int nz = at[1] + step[1];
                if (!seen.add(key(nx, nz))) {
                    continue;
                }
                switch (floor.at(nx, nz)) {
                    case SALT -> touchedSalt = true;
                    case WALL -> { }
                    case OPEN -> {
                        if (seen.size() > maxCells) {
                            return false;
                        }
                        queue.add(new int[] {nx, nz});
                    }
                }
            }
        }
        return touchedSalt;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
