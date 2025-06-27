package io.github.half.wfc;

import java.util.Set;
import java.util.HashSet;

public class Position {
    public final int x, y, z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position add(Direction dir) {
        return new Position(x + dir.dx, y + dir.dy, z + dir.dz);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position)) return false;
        Position p = (Position) obj;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() {
        return x * 31 * 31 + y * 31 + z;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}

