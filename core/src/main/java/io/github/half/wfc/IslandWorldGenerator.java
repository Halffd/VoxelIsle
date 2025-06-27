package io.github.half.wfc;
import io.github.half.*;
import java.util.*;
import io.github.half.wfc.constraints.*;

public class IslandWorldGenerator extends WorldGenerator {
    private static final float ISLAND_FREQUENCY = 0.003f; // More islands
    private static final float OCEAN_BIAS = 0.7f; // 70% ocean coverage
    private static final float ISLAND_SIZE_VARIANCE = 2.5f;

    private WFCSolver wfcSolver;
    private PerlinNoise islandNoise;
    private PerlinNoise islandShapeNoise;
    private PerlinNoise archipelagoNoise;

    public IslandWorldGenerator() {
        super();
        setupWFC();
        setupIslandGeneration();
    }

    private void setupIslandGeneration() {
        long seed = System.currentTimeMillis();
        islandNoise = new PerlinNoise(seed * 12289);
        islandShapeNoise = new PerlinNoise(seed * 37171);
        archipelagoNoise = new PerlinNoise(seed * 65537);
    }

    private void setupWFC() {
        Set<Constraint> worldConstraints = new HashSet<>();

        // ISLAND-SPECIFIC CONSTRAINTS

        // 1. Water level constraints - favor bigger oceans
        worldConstraints.add(new HeightConstraint(BlockType.WATER, 0, 35));
        worldConstraints.add(new HeightConstraint(BlockType.AIR, 32, 64));

        // 2. Island formation rules
        worldConstraints.add(new IslandFormationConstraint());

        // 3. Beach/shore transitions
        worldConstraints.add(new ShorelineConstraint());

        // 4. Grass needs dirt underneath (classic rule)
        worldConstraints.add(new AdjacencyConstraint(BlockType.GRASS, Direction.DOWN,
            BlockType.DIRT, BlockType.STONE));

        // 5. Sand appears near water
        worldConstraints.add(new ProximityConstraint(BlockType.SAND, BlockType.WATER, 3));

        // 6. Cave systems in islands
        worldConstraints.add(new ErosionConstraint(0.4f));

        // 7. Ore distribution in island cores
        worldConstraints.add(new GeologicalConstraint(BlockType.IRON, BlockType.STONE, 0.6f));
        worldConstraints.add(new GeologicalConstraint(BlockType.GOLD, BlockType.STONE, 0.8f));
        worldConstraints.add(new GeologicalConstraint(BlockType.DIAMOND, BlockType.STONE, 0.9f));

        wfcSolver = new WFCSolver(worldConstraints, System.currentTimeMillis());
    }

    @Override
    public BlockType getBlockAt(int worldX, int worldY, int worldZ) {
        // Use WFC for surface and near-surface generation
        if (worldY >= 25) {
            return getWFCBlockAt(worldX, worldY, worldZ);
        } else {
            // Use traditional generation for deep underground
            return super.getBlockAt(worldX, worldY, worldZ);
        }
    }

    private BlockType getWFCBlockAt(int worldX, int worldY, int worldZ) {
        // Check if this is an island area
        float islandValue = getIslandValue(worldX, worldZ);

        if (islandValue > OCEAN_BIAS) {
            // Island area - use WFC with island constraints
            return solveForPosition(worldX, worldY, worldZ, true);
        } else {
            // Ocean area
            if (worldY <= 32) {
                return BlockType.WATER;
            } else {
                return BlockType.AIR;
            }
        }
    }

    private float getIslandValue(int x, int z) {
        // Multi-scale island generation

        // 1. Archipelago pattern (large scale)
        float archipelago = archipelagoNoise.noise(x * 0.0008f, z * 0.0008f);

        // 2. Individual island centers (medium scale)
        float islandCenters = islandNoise.noise(x * ISLAND_FREQUENCY, z * ISLAND_FREQUENCY);

        // 3. Island shape variation (small scale)
        float shapeDetail = islandShapeNoise.noise(x * 0.01f, z * 0.01f) * 0.3f;

        // Combine scales with island bias
        float combined = (archipelago + 1) * 0.5f; // Normalize [-1,1] to [0,1]
        combined += (islandCenters + 1) * 0.3f; // Add island centers
        combined += shapeDetail; // Add shape variation

        // Apply island size variance
        combined *= ISLAND_SIZE_VARIANCE;

        return Math.min(1.0f, Math.max(0.0f, combined));
    }

    private BlockType solveForPosition(int x, int y, int z, boolean isIsland) {
        // Create a mini WFC problem for this position and its neighbors
        Set<Position> localPositions = new HashSet<>();
        Position center = new Position(x, y, z);

        // Add local neighborhood for context
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    localPositions.add(new Position(x + dx, y + dy, z + dz));
                }
            }
        }

        // Create local context
        LocalWorldContext localContext = new LocalWorldContext(x, y, z, isIsland);

        // Solve with WFC
        if (wfcSolver.solve(localContext, localPositions)) {
            return localContext.getBlockAt(center);
        } else {
            // Fallback to height-based generation
            return getHeightBasedBlock(x, y, z, isIsland);
        }
    }

    private BlockType getHeightBasedBlock(int x, int y, int z, boolean isIsland) {
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
    }

    private float generateIslandHeight(int x, int z) {
        float islandValue = getIslandValue(x, z);

        // Islands rise above sea level based on their "strength"
        float baseHeight = 32; // Sea level
        float islandHeight = (islandValue - OCEAN_BIAS) / (1.0f - OCEAN_BIAS);
        islandHeight = Math.max(0, islandHeight);

        // Apply height curve for more natural islands
        islandHeight = (float) Math.pow(islandHeight, 0.7f);

        return baseHeight + islandHeight * 25; // Max island height ~57
    }
}

