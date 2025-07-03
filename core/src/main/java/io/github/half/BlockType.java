package io.github.half;

public enum BlockType {
    AIR(false, 0f),
    STONE(true, 2.0f),
    DIRT(true, 0.5f),
    GRASS(true, 0.6f),
    SAND(true, 0.5f),
    WATER(false, 0f),
    COAL(true, 2.5f),
    IRON(true, 4.0f),
    GOLD(true, 3.0f),
    DIAMOND(true, 5.0f),
    CRYSTAL(true, 4.5f),
    OIL(false, 0f),
    WOOD(true, 1.5f),
    GRAVEL(true, 0.6f),
    CLAY(true, 0.7f),
    LEAVES(true, 0.2f),
    SANDSTONE(true, 0.8f),
    CACTUS(true, 0.4f),
    COAL_ORE(true, 3.0f),
    IRON_ORE(true, 5.0f);

    private final boolean solid;
    private final float hardness;

    BlockType(boolean solid, float hardness) {
        this.solid = solid;
        this.hardness = hardness;
    }

    public boolean isSolid() {
        return solid;
    }

    public float getHardness() {
        return hardness;
    }
}
