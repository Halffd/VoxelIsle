package io.github.half.wfc.constraints;

import io.github.half.wfc.*;
import io.github.half.BlockType;
import java.util.*;

public class AdjacencyConstraint extends Constraint {
    private final BlockType sourceType;
    private final Direction direction;
    private final Set<BlockType> allowedNeighbors;

    public AdjacencyConstraint(BlockType sourceType, Direction direction, BlockType... allowedNeighbors) {
        this.sourceType = sourceType;
        this.direction = direction;
        this.allowedNeighbors = new HashSet<>(Arrays.asList(allowedNeighbors));
    }

    @Override
    public boolean isValid(Position pos, BlockType blockType, WorldContext context) {
        if (blockType != sourceType) return true;

        Position neighborPos = pos.add(direction);
        BlockType neighbor = context.getBlockAt(neighborPos);

        return neighbor == null || allowedNeighbors.contains(neighbor);
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position pos, WorldContext context) {
        Position sourcePos = pos.add(direction.opposite());
        BlockType sourceBlock = context.getBlockAt(sourcePos);

        if (sourceBlock == sourceType) {
            return new HashSet<>(allowedNeighbors);
        }

        return EnumSet.allOf(BlockType.class);
    }

    @Override
    public int priority() { return 10; }
}
