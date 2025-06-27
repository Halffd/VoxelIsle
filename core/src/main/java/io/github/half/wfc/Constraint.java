package io.github.half.wfc;


import io.github.half.BlockType;

import java.util.Set;

public abstract class Constraint {
    public abstract boolean isValid(Position pos, BlockType blockType, WorldContext context);

    public abstract Set<BlockType> getAllowedTypes(Position pos, WorldContext context);
    public abstract int priority(); // Higher = more important
}
