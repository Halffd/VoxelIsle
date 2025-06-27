package io.github.half.wfc;

import io.github.half.BlockType;
import java.util.*;

public class WFCCell {
    private Set<BlockType> possibleTypes;
    private BlockType collapsedType;
    private final Position position;
    private float entropy;

    public WFCCell(Position position) {
        this.position = position;
        this.possibleTypes = EnumSet.allOf(BlockType.class);
        this.collapsedType = null;
        this.entropy = possibleTypes.size();
    }

    public boolean isCollapsed() {
        return collapsedType != null;
    }

    public BlockType getCollapsedType() {
        return collapsedType;
    }

    public Set<BlockType> getPossibleTypes() {
        return new HashSet<>(possibleTypes);
    }

    public float getEntropy() {
        return entropy;
    }

    public boolean canBe(BlockType blockType) {
        return possibleTypes.contains(blockType);
    }

    public void collapse(BlockType blockType) {
        if (!possibleTypes.contains(blockType)) {
            throw new IllegalStateException("Cannot collapse to " + blockType + " at " + position);
        }
        this.collapsedType = blockType;
        this.possibleTypes = EnumSet.of(blockType);
        this.entropy = 0;
    }

    public boolean constrain(Set<BlockType> allowedTypes) {
        Set<BlockType> newPossible = EnumSet.copyOf(possibleTypes);
        newPossible.retainAll(allowedTypes);

        if (newPossible.isEmpty()) {
            return false; // Contradiction!
        }

        boolean changed = !newPossible.equals(possibleTypes);
        this.possibleTypes = newPossible;
        this.entropy = possibleTypes.size();

        // Auto-collapse if only one option remains
        if (possibleTypes.size() == 1) {
            collapsedType = possibleTypes.iterator().next();
            entropy = 0;
        }

        return changed;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        if (isCollapsed()) {
            return position + ": " + collapsedType;
        } else {
            return position + ": " + possibleTypes.size() + " options";
        }
    }
}
