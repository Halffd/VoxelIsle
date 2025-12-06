package io.github.half.wfc.constraints;

import io.github.half.BlockType;
import io.github.half.wfc.Constraint;
import io.github.half.wfc.Position;
import io.github.half.wfc.WorldContext;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enforces a simple surface band based on local height from the WorldContext.
 * - Below the surface: disallow AIR; prefer solid materials (STONE/DIRT/SAND)
 * - At the surface: allow GRASS or SAND (plus DIRT as fallback)
 * - Between surface and sea level: allow WATER
 * - Above surface: allow AIR (and optionally leaves/wood for structures)
 */
public class SurfaceConstraint extends Constraint {
    private final int priority;
    private final int seaLevel;
    private final int surfaceThickness;

    public SurfaceConstraint(int priority, int seaLevel, int surfaceThickness) {
        this.priority = priority;
        this.seaLevel = seaLevel;
        this.surfaceThickness = Math.max(1, surfaceThickness);
    }

    @Override
    public boolean isValid(Position position, BlockType blockType, WorldContext context) {
        // Validation mirrors allowed types logic; rely primarily on getAllowedTypes
        return getAllowedTypes(position, context).contains(blockType);
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position position, WorldContext context) {
        float heightF = context.getHeightAt(position.x, position.z);
        int height = Math.round(heightF);

        // Surface band (height and height-1 ... height-(surfaceThickness-1))
        boolean inSurfaceBand = position.y <= height && position.y >= height - (surfaceThickness - 1);

        if (inSurfaceBand) {
            // On the surface: allow grass/sand with dirt fallback
            Set<BlockType> allowed = EnumSet.of(BlockType.GRASS, BlockType.SAND, BlockType.DIRT);
            return allowed;
        }

        if (position.y < height) {
            // Below surface: solid blocks only, no AIR or WATER
            return EnumSet.of(BlockType.STONE, BlockType.DIRT, BlockType.SAND, BlockType.SANDSTONE, BlockType.COAL_ORE, BlockType.IRON_ORE);
        }

        // position.y > height
        if (position.y <= seaLevel) {
            // Between surface and sea: allow WATER to fill up to sea level
            return EnumSet.of(BlockType.WATER, BlockType.AIR);
        }

        // Above both surface and sea: air, plus leaves/wood for structures
        return EnumSet.of(BlockType.AIR, BlockType.LEAVES, BlockType.WOOD);
    }

    @Override
    public int priority() {
        return priority;
    }
}
