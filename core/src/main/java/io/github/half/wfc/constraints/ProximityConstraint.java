package io.github.half.wfc.constraints;
import io.github.half.wfc.*;
import io.github.half.BlockType;
import java.util.*;

public class ProximityConstraint extends Constraint {
    private final BlockType targetType;
    private final BlockType referenceType;
    private final int maxDistance;

    public ProximityConstraint(BlockType targetType, BlockType referenceType, int maxDistance) {
        this.targetType = targetType;
        this.referenceType = referenceType;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        if (blockType != targetType) return true;

        // Check if reference type is within maxDistance
        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -maxDistance; dy <= maxDistance; dy++) {
                for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= maxDistance) {
                        Position checkPos = new Position(pos.x + dx, pos.y + dy, pos.z + dz);
                        BlockType block = context.getBlockAt(checkPos);
                        if (block == referenceType) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        boolean hasReference = false;

        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -maxDistance; dy <= maxDistance; dy++) {
                for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= maxDistance) {
                        Position checkPos = new Position(pos.x + dx, pos.y + dy, pos.z + dz);
                        BlockType block = context.getBlockAt(checkPos);
                        if (block == referenceType) {
                            hasReference = true;
                            break;
                        }
                    }
                }
                if (hasReference) break;
            }
            if (hasReference) break;
        }

        if (!hasReference) {
            Set<BlockType> allowed = EnumSet.allOf(BlockType.class);
            allowed.remove(targetType);
            return allowed;
        }

        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 5; }
}
