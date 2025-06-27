package io.github.half.wfc.constraints;

import io.github.half.wfc.*;
import io.github.half.BlockType;
import java.util.*;

// Geological formation constraints
public class GeologicalConstraint extends Constraint {
    private final BlockType oreType;
    private final BlockType hostRock;
    private final float densityThreshold;

    public GeologicalConstraint(BlockType oreType, BlockType hostRock, float densityThreshold) {
        this.oreType = oreType;
        this.hostRock = hostRock;
        this.densityThreshold = densityThreshold;
    }

    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        if (blockType != oreType) return true;

        // Ore can only form in host rock with sufficient density
        float hostRockDensity = calculateHostRockDensity(pos, context);
        return hostRockDensity >= densityThreshold;
    }

    private float calculateHostRockDensity(Position pos, WorldContext context) {
        int hostRockCount = 0;
        int totalCount = 0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Position checkPos = new Position(pos.x + dx, pos.y + dy, pos.z + dz);
                    BlockType block = context.getBlockAt(checkPos);
                    if (block != null) {
                        totalCount++;
                        if (block == hostRock) hostRockCount++;
                    }
                }
            }
        }

        return totalCount > 0 ? (float) hostRockCount / totalCount : 0f;
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        float hostRockDensity = calculateHostRockDensity(pos, context);
        if (hostRockDensity < densityThreshold) {
            Set<BlockType> allowed = EnumSet.allOf(BlockType.class);
            allowed.remove(oreType);
            return allowed;
        }
        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 6; }
}
