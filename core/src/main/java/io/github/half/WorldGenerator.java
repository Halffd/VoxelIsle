package io.github.half;

import java.util.Random;

public class WorldGenerator {
    private static final int WATER_LEVEL = 32;
    // Larger seed range for better random distribution
    private static final long SEED = System.currentTimeMillis();

    private PerlinNoise heightNoise;
    private PerlinNoise heightDetailNoise;
    private PerlinNoise caveNoise;
    private PerlinNoise caveDensityNoise;
    private PerlinNoise oreNoise;
    private PerlinNoise biomeNoise;
    private Random random;

    public WorldGenerator() {
        random = new Random(SEED);
        // Use specific seeds derived from main seed to avoid correlation
        heightNoise = new PerlinNoise(SEED * 16807);
        heightDetailNoise = new PerlinNoise(SEED * 48271);
        caveNoise = new PerlinNoise(SEED * 65539);
        caveDensityNoise = new PerlinNoise(SEED * 22699);
        oreNoise = new PerlinNoise(SEED * 37449);
        biomeNoise = new PerlinNoise(SEED * 104729);

        System.out.println("World generation started with seed: " + SEED);
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
        // Improved terrain generation with better scale separation and biome influence
        float scale1 = 0.005f;  // Continental features
        float scale2 = 0.02f;   // Medium features (hills)
        float scale3 = 0.08f;   // Small details
        float scale4 = 0.15f;   // Micro details

        // Biome influence (0.0 - 1.0)
        float biomeValue = (biomeNoise.noise(x * 0.004f, z * 0.004f) + 1) * 0.5f;

        // Base continental height
        float continentalHeight = (heightNoise.noise(x * scale1, z * scale1) + 1) * 0.5f;

        // Apply exponential curve to create more distinct land and ocean areas
        continentalHeight = (float)Math.pow(continentalHeight, 1.5);

        // Hills and medium features
        float hillHeight = heightNoise.noise(x * scale2, z * scale2);

        // Small terrain details
        float detailHeight = heightDetailNoise.noise(x * scale3, z * scale3) * 0.3f +
                             heightDetailNoise.noise(x * scale4, z * scale4) * 0.15f;

        // Combine all features with appropriate weights
        float height = WATER_LEVEL;
        height += continentalHeight * 30; // Continental scale (±30 blocks)
        height += hillHeight * 12 * continentalHeight; // Hills, more prominent on land
        height += detailHeight * 6 * continentalHeight; // Small details, more prominent on land

        // Biome-specific height modifications
        if (biomeValue > 0.6f) { // Mountains
            float mountainFactor = (biomeValue - 0.6f) / 0.4f;
            height += mountainFactor * mountainFactor * 20;
        } else if (biomeValue < 0.3f) { // Plains or lowlands
            float plainsFactor = (0.3f - biomeValue) / 0.3f;
            height -= plainsFactor * 8;
        }

        // Ensure minimum height
        height = Math.max(height, 5);

        return height;
    }

    private boolean hasCave(int x, int y, int z) {
        // No caves very near surface or at bottom
        if (y < 8 || y > 55) return false;

        // Calculate 3D noise value for cave shape
        float caveShape = caveNoise.noise(x * 0.03f, y * 0.04f, z * 0.03f);

        // Vary cave density based on depth
        float caveDensity = 0.6f;

        // More caves in middle layers (20-40)
        float depthFactor = 1.0f;
        if (y > 40) {
            depthFactor = 1.0f - (y - 40) / 15.0f; // Reduce caves toward surface
        } else if (y < 20) {
            depthFactor = 0.7f + (y - 8) / 40.0f; // Slightly fewer caves at very bottom
        }

        // Add some local variation to cave density
        float localDensity = caveDensityNoise.noise(x * 0.01f, y * 0.01f, z * 0.01f) * 0.1f;

        // Calculate final threshold adjusted by depth
        float threshold = (caveDensity - localDensity) * depthFactor;

        // Adjust cave size with depth - larger caves in middle layers
        if (y > 15 && y < 45) {
            // Make some areas have larger connected caves
            float connectorNoise = caveDensityNoise.noise(x * 0.008f, y * 0.008f, z * 0.008f);
            if (connectorNoise > 0.4f) {
                threshold -= 0.1f; // Create larger connected areas
            }
        }

        return caveShape > threshold;
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
