package io.github.half.wfc;
import io.github.half.*;

import io.github.half.BlockType;

// Implementation that wraps your existing world
public class ChunkWorldContext implements WorldContext {
    private final Chunk chunk;
    private final World world;
    private final int chunkX, chunkZ;

    public ChunkWorldContext(Chunk chunk, World world, int chunkX, int chunkZ) {
        this.chunk = chunk;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public BlockType getBlockAt(Position pos) {
        // Convert to world coordinates
        int worldX = pos.x + chunkX * 16;
        int worldZ = pos.z + chunkZ * 16;

        // Check if it's within this chunk
        if (pos.x >= 0 && pos.x < 16 && pos.z >= 0 && pos.z < 16 &&
            pos.y >= 0 && pos.y < 64) {
            return chunk.getBlockAt(pos.x, pos.y, pos.z);
        }

        // Query neighboring chunks through world
        return world.getBlockAt(worldX, pos.y, worldZ);
    }

    @Override
    public void setBlockAt(Position pos, BlockType blockType) {
        if (pos.x >= 0 && pos.x < 16 && pos.z >= 0 && pos.z < 16 &&
            pos.y >= 0 && pos.y < 64) {
            chunk.setBlockAt(pos.x, pos.y, pos.z, blockType);
        }
    }

    @Override
    public boolean isGenerated(Position pos) {
        // Check if this position has been determined yet
        return getBlockAt(pos) != null;
    }

    @Override
    public float getHeightAt(int x, int z) {
        // Use your existing height generation
        int worldX = x + chunkX * 16;
        int worldZ = z + chunkZ * 16;
        return world.getHeightAt(worldX, worldZ); // You'll need to add this method
    }

    @Override
    public float getTemperatureAt(Position pos) {
        // Simplified temperature model
        return Math.max(0, 30 - pos.y * 0.5f); // Cooler with altitude
    }

    @Override
    public float getHumidityAt(Position pos) {
        // Simplified humidity model
        float baseHumidity = 50f;
        if (getBlockAt(pos.add(Direction.DOWN)) == BlockType.WATER) {
            baseHumidity += 30f;
        }
        return Math.min(100f, baseHumidity);
    }
}
