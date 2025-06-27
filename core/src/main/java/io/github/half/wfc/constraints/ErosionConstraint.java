package io.github.half.wfc.constraints;

import io.github.half.wfc.*;
import io.github.half.BlockType;
import java.util.*;

// Hydrological constraints for caves
public class ErosionConstraint extends Constraint {
    private final float waterFlowThreshold;

    public ErosionConstraint(float waterFlowThreshold) {
        this.waterFlowThreshold = waterFlowThreshold;
    }

    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        if (blockType != BlockType.AIR) return true;

        // Air (caves) can only exist where water flow potential is high
        float waterFlow = calculateWaterFlowPotential(pos, context);
        return waterFlow >= waterFlowThreshold;
    }

    private float calculateWaterFlowPotential(Position pos, WorldContext context) {
        // Simplified water flow calculation based on height gradients
        float potential = 0f;
        int waterLevel = 32; // Same as your WATER_LEVEL

        // Higher potential if above water table
        if (pos.y > waterLevel) {
            potential += (pos.y - waterLevel) * 0.1f;
        }

        // Check for nearby water sources or height gradients
        for (Direction dir : Direction.values()) {
            Position neighbor = pos.add(dir);
            BlockType neighborBlock = context.getBlockAt(neighbor);

            if (neighborBlock == BlockType.WATER) {
                potential += 0.8f;
            } else if (neighborBlock == BlockType.AIR && neighbor.y > pos.y) {
                potential += 0.3f; // Downward flow potential
            }
        }

        return potential;
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        float waterFlow = calculateWaterFlowPotential(pos, context);
        if (waterFlow < waterFlowThreshold) {
            Set<BlockType> allowed = EnumSet.allOf(BlockType.class);
            allowed.remove(BlockType.AIR);
            return allowed;
        }
        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 7; }
}
