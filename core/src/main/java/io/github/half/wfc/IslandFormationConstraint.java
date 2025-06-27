package io.github.half.wfc;

// Island-specific constraints (continued)

import io.github.half.BlockType;

import java.util.EnumSet;
import java.util.Set;

class IslandFormationConstraint extends Constraint {
    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        // Islands should have coherent structure
        if (blockType == BlockType.GRASS || blockType == BlockType.DIRT) {
            // Check if we're actually on an island
            int waterCount = 0;
            int totalCount = 0;

            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Position checkPos = new Position(pos.x + dx, pos.y, pos.z + dz);
                    BlockType block = context.getBlockAt(checkPos);
                    if (block != null) {
                        totalCount++;
                        if (block == BlockType.WATER) waterCount++;
                    }
                }
            }

            // If surrounded by too much water, this shouldn't be land
            float waterRatio = totalCount > 0 ? (float) waterCount / totalCount : 0;
            return waterRatio < 0.6f; // Allow land if <60% water nearby
        }

        return true;
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        // Count nearby water to determine if this can be land
        int waterCount = 0;
        int totalCount = 0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Position checkPos = new Position(pos.x + dx, pos.y, pos.z + dz);
                BlockType block = context.getBlockAt(checkPos);
                if (block != null) {
                    totalCount++;
                    if (block == BlockType.WATER) waterCount++;
                }
            }
        }

        float waterRatio = totalCount > 0 ? (float) waterCount / totalCount : 0;

        if (waterRatio > 0.7f) {
            // Too much water - restrict land blocks
            Set<BlockType> allowed = EnumSet.allOf(BlockType.class);
            allowed.removeAll(EnumSet.of(BlockType.GRASS, BlockType.DIRT, BlockType.STONE));
            return allowed;
        }

        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 9; }
}

class ShorelineConstraint extends Constraint {
    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        if (blockType == BlockType.SAND) {
            // Sand should be near water or at beach level
            boolean nearWater = false;
            boolean correctHeight = pos.y >= 30 && pos.y <= 35;

            for (Direction dir : Direction.values()) {
                Position neighbor = pos.add(dir);
                BlockType neighborBlock = context.getBlockAt(neighbor);
                if (neighborBlock == BlockType.WATER) {
                    nearWater = true;
                    break;
                }
            }

            return nearWater || correctHeight;
        }

        return true;
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        boolean nearWater = false;
        boolean correctHeight = pos.y >= 30 && pos.y <= 35;

        for (Direction dir : Direction.values()) {
            Position neighbor = pos.add(dir);
            BlockType neighborBlock = context.getBlockAt(neighbor);
            if (neighborBlock == BlockType.WATER) {
                nearWater = true;
                break;
            }
        }

        if (!nearWater && !correctHeight) {
            Set<BlockType> allowed = EnumSet.allOf(BlockType.class);
            allowed.remove(BlockType.SAND);
            return allowed;
        }

        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 8; }
}

class ProximityConstraint extends Constraint {
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
