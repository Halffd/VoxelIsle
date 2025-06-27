package io.github.half.wfc;

import io.github.half.BlockType;
import java.util.HashMap;
import java.util.Map;

public class LocalWorldContext implements WorldContext {
    private Map<Position, BlockType> localBlocks;
    private final int centerX, centerY, centerZ;
    private final boolean isIsland;

    public LocalWorldContext(int centerX, int centerY, int centerZ, boolean isIsland) {
        this.localBlocks = new HashMap<>();
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.isIsland = isIsland;
    }

    @Override
    public BlockType getBlockAt(Position pos) {
        return localBlocks.get(pos);
    }

    @Override
    public void setBlockAt(Position pos, BlockType blockType) {
        localBlocks.put(pos, blockType);
    }

    @Override
    public boolean isGenerated(Position pos) {
        return localBlocks.containsKey(pos);
    }

    @Override
    public float getHeightAt(int x, int z) {
        // Simplified height calculation for local context
        if (isIsland) {
            return 35 + (float)(Math.sin(x * 0.1) * Math.cos(z * 0.1)) * 5;
        } else {
            return 32; // Sea level
        }
    }

    @Override
    public float getTemperatureAt(Position pos) {
        return Math.max(5, 25 - pos.y * 0.3f);
    }

    @Override
    public float getHumidityAt(Position pos) {
        float baseHumidity = isIsland ? 60f : 80f;
        return Math.min(100f, baseHumidity + (32 - pos.y) * 2f);
    }
}
