package io.github.half.wfc.constraints;

import io.github.half.BlockType;
import io.github.half.wfc.Constraint;
import io.github.half.wfc.Position;
import io.github.half.wfc.WorldContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

public class StructureConstraint extends Constraint {

    private final int priority;
    private final Random random;
    private final float structureDensity = 0.001f; // 0.1% chance for a structure

    public StructureConstraint(int priority) {
        this.priority = priority;
        this.random = new Random();
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position position, WorldContext context) {
        // This constraint doesn't restrict allowed types, but rather places structures.
        // It will return all possible types, and then isValid will check for placement.
        return new HashSet<>(Arrays.asList(BlockType.values()));
    }

    @Override
    public boolean isValid(Position position, BlockType blockType, WorldContext context) {
        // Only attempt to place structures at a certain height and if the block below is solid
        if (position.y > 32 && position.y < 60 && blockType == BlockType.AIR) {
            // Check if the block below is solid
            BlockType blockBelow = context.getBlockAt(position.down());
            if (blockBelow != null && blockBelow.isSolid()) {
                // Randomly decide to place a structure
                if (random.nextFloat() < structureDensity) {
                    placeStructure(position, context);
                    return false; // Prevent other blocks from being placed here
                }
            }
        }
        return true;
    }

    private void placeStructure(Position position, WorldContext context) {
        // Simple structure: a 1x1x2 pillar of wood with a leaf on top
        // This is a placeholder and can be expanded to more complex structures
        context.setBlockAt(position, BlockType.WOOD);
        context.setBlockAt(position.up(), BlockType.WOOD);
        context.setBlockAt(position.up().up(), BlockType.LEAVES);
    }

    @Override
    public int priority() {
        return priority;
    }
}
