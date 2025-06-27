package io.github.half.wfc;

public class IslandConfig {
    // Island generation parameters
    public static final float OCEAN_COVERAGE = 0.75f;        // 75% ocean
    public static final float ISLAND_DENSITY = 0.008f;       // More frequent islands
    public static final float ISLAND_SIZE_MIN = 0.3f;        // Minimum island size
    public static final float ISLAND_SIZE_MAX = 2.0f;        // Maximum island size
    public static final float ARCHIPELAGO_SCALE = 0.002f;    // Large-scale island grouping

    // Island shape parameters
    public static final float SHORE_ROUGHNESS = 0.4f;        // Coastline complexity
    public static final float ELEVATION_VARIANCE = 0.6f;     // Height variation
    public static final int MAX_ISLAND_HEIGHT = 25;          // Blocks above sea level

    // Biome parameters for islands
    public static final float BEACH_WIDTH = 2.5f;            // Beach zone size
    public static final float FOREST_COVERAGE = 0.3f;        // Vegetation density
    public static final float MOUNTAIN_THRESHOLD = 0.7f;     // When islands become mountainous

    // WFC solver parameters
    public static final int WFC_MAX_ITERATIONS = 1000;       // Safety limit
    public static final float CONSTRAINT_RELAXATION = 0.1f;  // Fallback tolerance
}
