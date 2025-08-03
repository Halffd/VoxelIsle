package io.github.half.wfc;
import io.github.half.*;
import java.util.*;
import io.github.half.wfc.constraints.*;

public class IslandWorldGenerator extends WorldGenerator {
    // Island generation parameters
    private static final float ISLAND_FREQUENCY = 0.002f;  // More spread out islands
    private static final float OCEAN_BIAS = 0.6f;          // More land
    private static final float ISLAND_SIZE_VARIANCE = 3.0f; // More varied island sizes
    private static final float SHORE_FALLOFF = 0.1f;       // Smoother shorelines

    // Safety limits
    private static final int MAX_WFC_ATTEMPTS = 5;
    private static final long MAX_WFC_TIME_MS = 10; // 10ms timeout per solve
    private static final int MAX_CONSTRAINT_FAILURES = 100;

    private WFCSolver wfcSolver;
    private Set<Constraint> worldConstraints;
    private PerlinNoise islandNoise;
    private PerlinNoise islandShapeNoise;
    private PerlinNoise archipelagoNoise;
    private PerlinNoise detailNoise1;
    private PerlinNoise detailNoise2;
    private PerlinNoise archipelagoNoise;

    // Safety counters
    private int constraintFailureCount = 0;
    private boolean wfcEnabled = true;

    public IslandWorldGenerator() {
        super();
        try {
            setupWFC();
            setupIslandGeneration();
            System.out.println("IslandWorldGenerator initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize WFC, falling back to traditional generation: " + e.getMessage());
            wfcEnabled = false;
        }
    }

    private void setupIslandGeneration() {
        try {
            long seed = 1752126679928L; // Fixed seed for consistency
            
            // Multi-octave noise for better terrain detail
            islandNoise = new PerlinNoise(seed * 12289);
            islandShapeNoise = new PerlinNoise(seed * 37171);
            archipelagoNoise = new PerlinNoise(seed * 65537);
            
            // Additional noise layers for more detail
            detailNoise1 = new PerlinNoise(seed * 8191);
            detailNoise2 = new PerlinNoise(seed * 16381);
            
            System.out.println("Island generation initialized with seed: " + seed);
        } catch (Exception e) {
            System.err.println("Failed to setup island noise: " + e.getMessage());
            // Fallback to simple noise
            islandNoise = new PerlinNoise(12345);
            islandShapeNoise = new PerlinNoise(67890);
            archipelagoNoise = new PerlinNoise(11111);
        }
    }

    private void setupWFC() {
        try {
            this.worldConstraints = new HashSet<>();

            // SIMPLIFIED CONSTRAINTS - less conflicts

            // 1. Basic height constraints with overlap zones
            this.worldConstraints.add(new HeightConstraint(BlockType.WATER, 0, 40)); // Extended range
            this.worldConstraints.add(new HeightConstraint(BlockType.AIR, 25, 64));  // Overlap zone

            // 2. Simple adjacency rules
            this.worldConstraints.add(new AdjacencyConstraint(BlockType.GRASS, Direction.DOWN,
                BlockType.DIRT, BlockType.STONE, BlockType.SAND)); // More options

            // 3. Simplified proximity - more lenient
            this.worldConstraints.add(new ProximityConstraint(BlockType.SAND, BlockType.WATER, 5)); // Larger range

            this.worldConstraints.add(new BiomeConstraint(10)); // Add BiomeConstraint
            this.worldConstraints.add(new StructureConstraint(5)); // Add StructureConstraint
            this.worldConstraints.add(new StructureConstraint(5)); // Add StructureConstraint

            wfcSolver = new WFCSolver(this.worldConstraints, System.currentTimeMillis());
            System.out.println("WFC setup complete with " + this.worldConstraints.size() + " constraints");

        } catch (Exception e) {
            System.err.println("WFC setup failed: " + e.getMessage());
            wfcEnabled = false;
        }
    }

    @Override
    public BlockType getBlockAt(int worldX, int worldY, int worldZ) {
        // Null safety check
        if (worldX < 0 || worldY < 0 || worldZ < 0 || worldY >= 64) {
            return BlockType.AIR;
        }

        try {
            // Use WFC for surface and near-surface generation IF enabled and not too many failures
            if (wfcEnabled && constraintFailureCount < MAX_CONSTRAINT_FAILURES && worldY >= 25) {
                BlockType wfcResult = getWFCBlockAt(worldX, worldY, worldZ);
                if (wfcResult != null) {
                    return wfcResult;
                } else {
                    constraintFailureCount++;
                    if (constraintFailureCount >= MAX_CONSTRAINT_FAILURES) {
                        System.err.println("Too many WFC failures, disabling WFC");
                        wfcEnabled = false;
                    }
                }
            }

            // Fallback to traditional generation
            return getTraditionalBlockAt(worldX, worldY, worldZ);

        } catch (Exception e) {
            System.err.println("Error generating block at (" + worldX + "," + worldY + "," + worldZ + "): " + e.getMessage());
            // Ultimate fallback
            return getBasicBlockAt(worldX, worldY, worldZ);
        }
    }

    private BlockType getWFCBlockAt(int worldX, int worldY, int worldZ) {
        try {
            // Check if this is an island area
            float islandValue = getIslandValue(worldX, worldZ);

            if (islandValue > OCEAN_BIAS) {
                // Island area - try WFC with timeout
                return solveForPositionSafe(worldX, worldY, worldZ, true);
            } else {
                // Ocean area - simple logic
                return worldY <= 32 ? BlockType.WATER : BlockType.AIR;
            }
        } catch (Exception e) {
            System.err.println("WFC block generation failed: " + e.getMessage());
            return null; // Trigger fallback
        }
    }

    private BlockType solveForPositionSafe(int x, int y, int z, boolean isIsland) {
        try {
            // Time-limited WFC solving
            long startTime = System.currentTimeMillis();

            for (int attempt = 0; attempt < MAX_WFC_ATTEMPTS; attempt++) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > MAX_WFC_TIME_MS) {
                    System.err.println("WFC timeout at (" + x + "," + y + "," + z + ")");
                    break;
                }

                try {
                    // Create a mini WFC problem
                    Set<Position> localPositions = new HashSet<>();
                    Position center = new Position(x, y, z);

                    // Smaller neighborhood to reduce complexity
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = 0; dy <= 1; dy++) { // Reduced Y range
                            for (int dz = -1; dz <= 1; dz++) {
                                localPositions.add(new Position(x + dx, y + dy, z + dz));
                            }
                        }
                    }

                    // Create local context with null safety
                    LocalWorldContext localContext = new LocalWorldContext(x, y, z, isIsland);

                    // Create a new solver for each attempt to ensure different random seeds
                    WFCSolver attemptSolver = new WFCSolver(this.worldConstraints, System.currentTimeMillis() + attempt);

                    // Solve with WFC
                    if (attemptSolver.solve(localContext, localPositions)) {
                        BlockType result = localContext.getBlockAt(center);
                        if (result != null) {
                            return result;
                        }
                    }
                } catch (Exception e) {
                    // Silent retry on individual attempt failure
                    continue;
                }
            }

            // All attempts failed
            return null;

        } catch (Exception e) {
            System.err.println("Solve position failed: " + e.getMessage());
            return null;
        }
    }

    private float getIslandValue(int x, int z) {
        try {
            // Base island shape (large scale)
            float baseScale = 0.0005f;
            float baseNoise = archipelagoNoise.noise(x * baseScale, z * baseScale);
            
            // Medium scale features (island groups)
            float mediumScale = 0.0015f;
            float mediumNoise = islandNoise.noise(x * mediumScale, z * mediumScale) * 0.7f;
            
            // Small scale details (individual islands)
            float smallScale = 0.01f;
            float smallNoise = islandShapeNoise.noise(x * smallScale, z * smallScale) * 0.3f;
            
            // Combine noise layers
            float combined = baseNoise * 0.5f + mediumNoise * 0.3f + smallNoise * 0.2f;
            
            // Apply falloff from center (circular world)
            float centerX = 320; // Half of world size
            float centerZ = 320;
            float distX = (x - centerX) / centerX;
            float distZ = (z - centerZ) / centerZ;
            float distFromCenter = (float) Math.sqrt(distX * distX + distZ * distZ);
            
            // Apply circular falloff
            float falloff = 1.0f - distFromCenter;
            falloff = Math.max(0, falloff);
            falloff = (float) Math.pow(falloff, 0.8f);
            
            // Combine with noise
            combined = (combined + 1) * 0.5f; // Convert to 0-1 range
            combined = combined * falloff * ISLAND_SIZE_VARIANCE;
            
            // Add some extra detail noise
            float detail = detailNoise1.noise(x * 0.02f, z * 0.02f) * 0.1f;
            combined += detail;
            
            return Math.min(1.0f, Math.max(0.0f, combined));
            
        } catch (Exception e) {
            System.err.println("Island value calculation failed: " + e.getMessage());
            return 0.5f; // Default to some land
        }
    }

    // Traditional generation fallback
    private BlockType getTraditionalBlockAt(int worldX, int worldY, int worldZ) {
        try {
            float islandValue = getIslandValue(worldX, worldZ);
            boolean isIsland = islandValue > OCEAN_BIAS;

            return getHeightBasedBlock(worldX, worldY, worldZ, isIsland);
        } catch (Exception e) {
            if (GameSettings.getInstance().isWfcVerboseLoggingEnabled()) {
                System.out.println("WFC setup complete with " + this.worldConstraints.size() + " constraints");
            }
            return getBasicBlockAt(worldX, worldY, worldZ);
        }
    }

    private BlockType getHeightBasedBlock(int x, int y, int z, boolean isIsland) {
        try {
            if (!isIsland) {
                // Ocean floor
                if (y < 20) return BlockType.STONE;
                if (y <= 32) return BlockType.WATER;
                return BlockType.AIR;
            }

            float height = generateIslandHeight(x, z);
            float surfaceDepth = height - y;

            // Above surface
            if (y > height) {
                return y <= 32 ? BlockType.WATER : BlockType.AIR;
            }

            // Surface layer
            if (surfaceDepth < 1) {
                // Beach or grass
                if (height <= 33) return BlockType.SAND;
                
                // Add some variation to grass
                float detail = detailNoise1.noise(x * 0.2f, z * 0.2f);
                if (detail > 0.8f) return BlockType.DIRT;
                if (detail < 0.2f) return BlockType.SAND;
                return BlockType.GRASS;
            }
            
            // Underground layers
            if (surfaceDepth < 4) {
                return height > 33 ? BlockType.DIRT : BlockType.SAND;
            }
            
            // Add some stone variation
            float stoneNoise = detailNoise2.noise(x * 0.1f, y * 0.1f, z * 0.1f);
            if (stoneNoise > 0.7f) return BlockType.STONE;
            if (stoneNoise < 0.3f) return BlockType.DIRT;
            
            // Default to stone
            return BlockType.STONE;
        } catch (Exception e) {
            return getBasicBlockAt(x, y, z);
        }
    }

    private float generateIslandHeight(int x, int z) {
        try {
            float islandValue = getIslandValue(x, z);
            
            // Base height (ocean floor)
            float baseHeight = 20f;
            
            // Calculate island height based on value
            float islandHeight;
            if (islandValue > OCEAN_BIAS) {
                // Above water - create islands
                float normalized = (islandValue - OCEAN_BIAS) / (1.0f - OCEAN_BIAS);
                islandHeight = (float) Math.pow(normalized, 1.5f) * 40f; // Higher islands
                
                // Add some noise for more natural look
                float noise = detailNoise2.noise(x * 0.05f, z * 0.05f) * 5f;
                islandHeight += noise;
                
                // Make some areas higher (mountains)
                float mountainNoise = islandNoise.noise(x * 0.01f, z * 0.01f);
                if (mountainNoise > 0.7f) {
                    islandHeight *= 1.5f + (mountainNoise - 0.7f) * 3f;
                }
            } else {
                // Ocean floor - gently sloping
                islandHeight = (islandValue / OCEAN_BIAS) * 15f;
            }
            
            // Ensure minimum height
            return baseHeight + Math.max(0, islandHeight);
            
        } catch (Exception e) {
            System.err.println("Height generation failed: " + e.getMessage());
            return 32f; // Fallback height
        }
    }

    // Ultimate fallback - basic block generation
    private BlockType getBasicBlockAt(int x, int y, int z) {
        if (y <= 30) return BlockType.STONE;
        if (y <= 32) return BlockType.WATER;
        if (y <= 35) return BlockType.SAND;
        return BlockType.AIR;
    }

    // Getter for debugging
    public boolean isWFCEnabled() {
        return wfcEnabled;
    }

    public int getConstraintFailureCount() {
        return constraintFailureCount;
    }
}
