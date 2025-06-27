package io.github.half;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class World {
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 64;

    ChunkManager chunkManager;
    private WorldGenerator worldGenerator;
    private Model[] blockModels;

    public World(Model[] blockModels) {
        this.blockModels = blockModels;
        this.worldGenerator = new WorldGenerator();
        this.chunkManager = new ChunkManager(blockModels);
    }

    public void update(Vector3 playerPosition) {
        // Update chunk loading through the chunk manager
        chunkManager.update(playerPosition);
    }

    public void render(ModelBatch batch, Camera camera, Environment environment) {
        ObjectMap<String, Chunk> loadedChunks = chunkManager.getLoadedChunks();

        for (Chunk chunk : loadedChunks.values()) {
            try {
                if (chunk != null && chunk.isVisible(camera) && chunk.isReady()) {
                    Array<ModelInstance> instances = chunk.getInstances();

                    if (instances != null && instances.size > 0) {
                        // Validação extra para prevenir arrays corrompidos
                        boolean hasValidInstances = true;
                        for (ModelInstance instance : instances) {
                            if (instance == null) {
                                hasValidInstances = false;
                                break;
                            }
                        }

                        if (hasValidInstances) {
                            batch.render(instances, environment);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Skipping corrupted chunk: " + e.getMessage());
            }
        }
    }
    public float getHeightAt(int x, int z) {
        // Use your world generator to get height
        return worldGenerator.generateHeight(x, z); // You'll need to make this method public
    }
    public BlockType getBlockAt(int x, int y, int z) {
        // Calculate which chunk this block belongs to
        int chunkX = Math.floorDiv(x, 16); // CHUNK_SIZE = 16
        int chunkZ = Math.floorDiv(z, 16);

        // Get the chunk
        String chunkKey = chunkX + "," + chunkZ;
        ObjectMap<String, Chunk> loadedChunks = chunkManager.getLoadedChunks();

        if (loadedChunks.containsKey(chunkKey)) {
            Chunk chunk = loadedChunks.get(chunkKey);

            // Convert world coordinates to local chunk coordinates
            int localX = x - (chunkX * 16);
            int localZ = z - (chunkZ * 16);

            return chunk.getBlockAt(localX, y, localZ);
        }

        // If chunk not loaded, return AIR
        return BlockType.AIR;
    }
    // E adiciona um método para saber quantos chunks estão carregados (usado no debug):
    public int getLoadedChunksCount() {
        return chunkManager.getLoadedChunks().size;
    }

    public void setBlockAt(int x, int y, int z, BlockType blockType) {
        if (y < 0 || y >= WORLD_HEIGHT) {
            return;
        }

        int chunkX = MathUtils.floor((float)x / CHUNK_SIZE);
        int chunkZ = MathUtils.floor((float)z / CHUNK_SIZE);
        String chunkKey = chunkX + "," + chunkZ;

        ObjectMap<String, Chunk> loadedChunks = chunkManager.getLoadedChunks();
        if (loadedChunks.containsKey(chunkKey)) {
            loadedChunks.get(chunkKey).setBlockAt(x - chunkX * CHUNK_SIZE, y, z - chunkZ * CHUNK_SIZE, blockType);

            // Queue mesh rebuild in chunk manager instead of rebuilding immediately
            chunkManager.queueRebuildMesh(chunkX, chunkZ);

            // Also check neighboring chunks if the block was on a chunk boundary
            int localX = x - chunkX * CHUNK_SIZE;
            int localZ = z - chunkZ * CHUNK_SIZE;

            if (localX == 0) chunkManager.queueRebuildMesh(chunkX - 1, chunkZ);
            if (localX == CHUNK_SIZE - 1) chunkManager.queueRebuildMesh(chunkX + 1, chunkZ);
            if (localZ == 0) chunkManager.queueRebuildMesh(chunkX, chunkZ - 1);
            if (localZ == CHUNK_SIZE - 1) chunkManager.queueRebuildMesh(chunkX, chunkZ + 1);
        }
    }

    public Vector3 raycast(Ray ray, float maxDistance) {
        Vector3 rayStart = new Vector3(ray.origin);
        Vector3 rayEnd = new Vector3(ray.direction).scl(maxDistance).add(rayStart);

        // DDA algorithm for raycasting through voxels
        int x = MathUtils.floor(rayStart.x);
        int y = MathUtils.floor(rayStart.y);
        int z = MathUtils.floor(rayStart.z);

        int endX = MathUtils.floor(rayEnd.x);
        int endY = MathUtils.floor(rayEnd.y);
        int endZ = MathUtils.floor(rayEnd.z);

        int dx = endX - x > 0 ? 1 : (endX - x < 0 ? -1 : 0);
        int dy = endY - y > 0 ? 1 : (endY - y < 0 ? -1 : 0);
        int dz = endZ - z > 0 ? 1 : (endZ - z < 0 ? -1 : 0);

        float tMaxX = dx != 0 ? ((dx > 0 ? (x + 1) : x) - rayStart.x) / ray.direction.x : Float.MAX_VALUE;
        float tMaxY = dy != 0 ? ((dy > 0 ? (y + 1) : y) - rayStart.y) / ray.direction.y : Float.MAX_VALUE;
        float tMaxZ = dz != 0 ? ((dz > 0 ? (z + 1) : z) - rayStart.z) / ray.direction.z : Float.MAX_VALUE;

        float tDeltaX = dx != 0 ? 1.0f / Math.abs(ray.direction.x) : Float.MAX_VALUE;
        float tDeltaY = dy != 0 ? 1.0f / Math.abs(ray.direction.y) : Float.MAX_VALUE;
        float tDeltaZ = dz != 0 ? 1.0f / Math.abs(ray.direction.z) : Float.MAX_VALUE;

        Vector3 lastPos = new Vector3(-1, -1, -1);

        // Step through the grid
        while ((x != endX || y != endY || z != endZ) && (tMaxX <= maxDistance || tMaxY <= maxDistance || tMaxZ <= maxDistance)) {
            BlockType blockType = getBlockAt(x, y, z);

            if (blockType != null && blockType != BlockType.AIR && blockType != BlockType.WATER) {
                // Hit a solid block
                return new Vector3(x, y, z);
            }

            lastPos.set(x, y, z);

            // Move to next cell
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += dx;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y += dy;
                tMaxY += tDeltaY;
            } else {
                z += dz;
                tMaxZ += tDeltaZ;
            }
        }

        return null; // No hit within range
    }

    public void dispose() {
        chunkManager.dispose();
    }
}
