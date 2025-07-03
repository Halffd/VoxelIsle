package io.github.half.wfc;
import io.github.half.*;
import java.util.*;
import io.github.half.wfc.constraints.*;

public class IslandWorldGenerator extends WorldGenerator {
    private static final float ISLAND_FREQUENCY = 0.003f;
    private static final float OCEAN_BIAS = 0.7f;
    private static final float ISLAND_SIZE_VARIANCE = 2.5f;

    // Safety limits
    private static final int MAX_WFC_ATTEMPTS = 5;
    private static final long MAX_WFC_TIME_MS = 10; // 10ms timeout per solve
    private static final int MAX_CONSTRAINT_FAILURES = 100;

    private WFCSolver wfcSolver;
    private PerlinNoise islandNoise;
    private PerlinNoise islandShapeNoise;
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
            long seed = System.currentTimeMillis();
            islandNoise = new PerlinNoise(seed * 12289);
            islandShapeNoise = new PerlinNoise(seed * 37171);
            archipelagoNoise = new PerlinNoise(seed * 65537);
        } catch (Exception e) {
            System.err.println("Failed to setup island noise, using default seed: " + e.getMessage());
            islandNoise = new PerlinNoise(12345);
            islandShapeNoise = new PerlinNoise(67890);
            archipelagoNoise = new PerlinNoise(11111);
        }
    }

    private void setupWFC() {
        try {
            Set<Constraint> worldConstraints = new HashSet<>();

            // SIMPLIFIED CONSTRAINTS - less conflicts

            // 1. Basic height constraints with overlap zones
            worldConstraints.add(new HeightConstraint(BlockType.WATER, 0, 40)); // Extended range
            worldConstraints.add(new HeightConstraint(BlockType.AIR, 25, 64));  // Overlap zone

            // 2. Simple adjacency rules
            worldConstraints.add(new AdjacencyConstraint(BlockType.GRASS, Direction.DOWN,
                BlockType.DIRT, BlockType.STONE, BlockType.SAND)); // More options

            // 3. Simplified proximity - more lenient
            worldConstraints.add(new ProximityConstraint(BlockType.SAND, BlockType.WATER, 5)); // Larger range

            // 4. Basic ore distribution - only if we have stone
            worldConstraints.add(new GeologicalConstraint(BlockType.IRON, BlockType.STONE, 0.3f)); // Lower threshold

            wfcSolver = new WFCSolver(worldConstraints, System.currentTimeMillis());
            System.out.println("WFC setup complete with " + worldConstraints.size() + " constraints");

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

                    // Solve with WFC
                    if (wfcSolver != null && wfcSolver.solve(localContext, localPositions)) {
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
            if (islandNoise == null || islandShapeNoise == null || archipelagoNoise == null) {
                // Fallback to simple calculation
                return (float) ((Math.sin(x * 0.01f) * Math.cos(z * 0.01f) + 1) * 0.5f);
            }

            // Multi-scale island generation
            float archipelago = archipelagoNoise.noise(x * 0.0008f, z * 0.0008f);
            float islandCenters = islandNoise.noise(x * ISLAND_FREQUENCY, z * ISLAND_FREQUENCY);
            float shapeDetail = islandShapeNoise.noise(x * 0.01f, z * 0.01f) * 0.3f;

            // Combine scales with island bias
            float combined = (archipelago + 1) * 0.5f;
            combined += (islandCenters + 1) * 0.3f;
            combined += shapeDetail;
            combined *= ISLAND_SIZE_VARIANCE;

            return Math.min(1.0f, Math.max(0.0f, combined));

        } catch (Exception e) {
            System.err.println("Island value calculation failed: " + e.getMessage());
            // Simple fallback
            return 0.3f; // Default to mostly ocean
        }
    }

    // Traditional generation fallback
    private BlockType getTraditionalBlockAt(int worldX, int worldY, int worldZ) {
        try {
            float islandValue = getIslandValue(worldX, worldZ);
            boolean isIsland = islandValue > OCEAN_BIAS;

            return getHeightBasedBlock(worldX, worldY, worldZ, isIsland);
        } catch (Exception e) {
            System.err.println("Traditional generation failed: " + e.getMessage());
            return getBasicBlockAt(worldX, worldY, worldZ);
        }
    }

    private BlockType getHeightBasedBlock(int x, int y, int z, boolean isIsland) {
        try {
            if (!isIsland) {
                return y <= 32 ? BlockType.WATER : BlockType.AIR;
            }

            float height = generateIslandHeight(x, z);

            if (y > height) return BlockType.AIR;
            if (y <= 32 && y > height) return BlockType.WATER;

            float surfaceDepth = height - y;

            if (surfaceDepth < 1) {
                return height > 33 ? BlockType.GRASS : BlockType.SAND;
            } else if (surfaceDepth < 3) {
                return height > 33 ? BlockType.DIRT : BlockType.SAND;
            } else {
                return BlockType.STONE;
            }
        } catch (Exception e) {
            return getBasicBlockAt(x, y, z);
        }
    }

    private float generateIslandHeight(int x, int z) {
        try {
            float islandValue = getIslandValue(x, z);

            float baseHeight = 32;
            float islandHeight = (islandValue - OCEAN_BIAS) / (1.0f - OCEAN_BIAS);
            islandHeight = Math.max(0, islandHeight);
            islandHeight = (float) Math.pow(islandHeight, 0.7f);

            return baseHeight + islandHeight * 25;
        } catch (Exception e) {
            return 32 + (float)(Math.sin(x * 0.01) * Math.cos(z * 0.01)) * 10;
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
