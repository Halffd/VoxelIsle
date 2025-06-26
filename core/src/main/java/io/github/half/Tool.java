package io.github.half;

public enum Tool {
    HAND(0.2f, 1f, 0.1f, 0.05f, 0.01f),
    WOOD_PICKAXE(0.4f, 1.5f, 0.5f, 0.1f, 0.05f),
    STONE_PICKAXE(0.5f, 2f, 1f, 0.3f, 0.1f),
    IRON_PICKAXE(0.7f, 3f, 2f, 1f, 0.3f),
    DIAMOND_PICKAXE(1f, 4f, 3f, 2f, 1f);

    private final float dirtSpeed;
    private final float stoneSpeed;
    private final float ironSpeed;
    private final float goldSpeed;
    private final float diamondSpeed;

    Tool(float dirtSpeed, float stoneSpeed, float ironSpeed, float goldSpeed, float diamondSpeed) {
        this.dirtSpeed = dirtSpeed;
        this.stoneSpeed = stoneSpeed;
        this.ironSpeed = ironSpeed;
        this.goldSpeed = goldSpeed;
        this.diamondSpeed = diamondSpeed;
    }

    public float getMiningSpeedFor(BlockType blockType) {
        switch (blockType) {
            case DIRT:
            case GRASS:
            case SAND:
                return dirtSpeed;
            case STONE:
            case COAL:
                return stoneSpeed;
            case IRON:
                return ironSpeed;
            case GOLD:
                return goldSpeed;
            case DIAMOND:
            case CRYSTAL:
                return diamondSpeed;
            case WATER:
            case OIL:
            case AIR:
                return 0f; // Can't mine these
            default:
                return 0.1f; // Default slow speed
        }
    }
}
