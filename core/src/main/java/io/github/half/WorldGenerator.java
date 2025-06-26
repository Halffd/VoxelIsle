package io.github.half;

import java.util.Random;

public class WorldGenerator {
    private static final int WATER_LEVEL = 32;
    private static final long SEED = 12345L;
    private PerlinNoise heightNoise;
    private PerlinNoise caveNoise;
    private PerlinNoise oreNoise;
    private Random random;
    
    public WorldGenerator() {
        random = new Random(SEED);
        heightNoise = new PerlinNoise(random.nextLong());
        caveNoise = new PerlinNoise(random.nextLong());
        oreNoise = new PerlinNoise(random.nextLong());
    }
    
    public BlockType getBlockAt(int worldX, int worldY, int worldZ) {
        // Generate height using multiple octaves of Perlin noise
        float height = generateHeight(worldX, worldZ);
        
        // Below sea level
        if (worldY <= WATER_LEVEL) {
            if (worldY > height) {
                return BlockType.WATER;
            }
        }
        
        // Above terrain height
        if (worldY > height) {
            return BlockType.AIR;
        }
        
        // Check for caves
        if (worldY < height - 5 && hasCave(worldX, worldY, worldZ)) {
            if (worldY <= WATER_LEVEL) {
                return BlockType.WATER;
            }
            return BlockType.AIR;
        }
        
        // Check for ores and special blocks
        BlockType ore = generateOre(worldX, worldY, worldZ, height);
        if (ore != BlockType.AIR) {
            return ore;
        }
        
        // Terrain layers
        float surfaceDepth = height - worldY;
        
        if (surfaceDepth < 1) {
            // Surface layer
            if (height > WATER_LEVEL + 2) {
                return BlockType.GRASS;
            } else if (height > WATER_LEVEL - 3) {
                return BlockType.SAND;
            } else {
                return BlockType.SAND;
            }
        } else if (surfaceDepth < 4) {
            // Subsurface layer
            if (height > WATER_LEVEL) {
                return BlockType.DIRT;
            } else {
                return BlockType.SAND;
            }
        } else {
            // Deep layer
            return BlockType.STONE;
        }
    }
    
    private float generateHeight(int x, int z) {
        float scale1 = 0.01f;  // Large features
        float scale2 = 0.05f;  // Medium features
        float scale3 = 0.1f;   // Small details
        
        float height = 0;
        height += heightNoise.noise(x * scale1, z * scale1) * 25;
        height += heightNoise.noise(x * scale2, z * scale2) * 10;
        height += heightNoise.noise(x * scale3, z * scale3) * 5;
        
        // Bias towards sea level
        height += WATER_LEVEL;
        
        // Ensure minimum height
        height = Math.max(height, 5);
        
        return height;
    }
    
    private boolean hasCave(int x, int y, int z) {
        if (y < 10 || y > 50) return false; // Caves only in certain Y range
        
        float caveValue = caveNoise.noise(x * 0.03f, y * 0.03f, z * 0.03f);
        return caveValue > 0.6f; // Threshold for cave generation
    }
    
    private BlockType generateOre(int x, int y, int z, float surfaceHeight) {
        float oreValue = oreNoise.noise(x * 0.1f, y * 0.1f, z * 0.1f);
        float depth = surfaceHeight - y;
        
        // Oil deposits (deep underground, rare)
        if (y < 15 && oreValue > 0.85f && random.nextFloat() < 0.001f) {
            return BlockType.OIL;
        }
        
        // Diamond (very deep, very rare)
        if (y < 20 && depth > 20 && oreValue > 0.9f && random.nextFloat() < 0.0005f) {
            return BlockType.DIAMOND;
        }
        
        // Crystal (rare, mid-depth)
        if (y > 20 && y < 40 && oreValue > 0.88f && random.nextFloat() < 0.001f) {
            return BlockType.CRYSTAL;
        }
        
        // Gold (deep, rare)
        if (y < 30 && depth > 15 && oreValue > 0.82f && random.nextFloat() < 0.003f) {
            return BlockType.GOLD;
        }
        
        // Iron (common)
        if (y < 50 && depth > 5 && oreValue > 0.75f && random.nextFloat() < 0.01f) {
            return BlockType.IRON;
        }
        
        // Coal (very common)
        if (y < 55 && depth > 3 && oreValue > 0.7f && random.nextFloat() < 0.02f) {
            return BlockType.COAL;
        }
        
        return BlockType.AIR; // No ore
    }
}