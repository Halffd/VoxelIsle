package io.github.half.wfc.constraints;

import io.github.half.wfc.*;
import io.github.half.BlockType;
import java.util.*;

// Height-based constraints
public class HeightConstraint extends Constraint {
    private final Set<BlockType> blockTypes;
    private final int minHeight, maxHeight;

    public HeightConstraint(BlockType blockType, int minHeight, int maxHeight) {
        this.blockTypes = EnumSet.of(blockType);
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        if (!blockTypes.contains(blockType)) return true;
        return pos.y >= minHeight && pos.y <= maxHeight;
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        if (pos.y < minHeight || pos.y > maxHeight) {
            Set<BlockType> allowed = EnumSet.allOf(BlockType.class);
            allowed.removeAll(blockTypes);
            return allowed;
        }
        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 8; }
}
