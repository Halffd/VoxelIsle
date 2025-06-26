package io.github.half;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class World {
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 64;
    private static final int RENDER_DISTANCE = 8;

    private ObjectMap<String, Chunk> loadedChunks;
    private Vector3 lastPlayerChunk;
    private WorldGenerator worldGenerator;
    private Model[] blockModels;

    public World(Model[] blockModels) {
        this.blockModels = blockModels;
        this.loadedChunks = new ObjectMap<>();
        this.lastPlayerChunk = new Vector3(-1, -1, -1);
        this.worldGenerator = new WorldGenerator();
    }

    public void update(Vector3 playerPosition) {
        int chunkX = MathUtils.floor(playerPosition.x / CHUNK_SIZE);
        int chunkZ = MathUtils.floor(playerPosition.z / CHUNK_SIZE);

        Vector3 currentChunk = new Vector3(chunkX, 0, chunkZ);

        // Only update if player moved to a different chunk
        if (!currentChunk.equals(lastPlayerChunk)) {
            lastPlayerChunk.set(currentChunk);

            // Load new chunks in render distance
            for (int x = chunkX - RENDER_DISTANCE; x <= chunkX + RENDER_DISTANCE; x++) {
                for (int z = chunkZ - RENDER_DISTANCE; z <= chunkZ + RENDER_DISTANCE; z++) {
                    String chunkKey = x + "," + z;
                    if (!loadedChunks.containsKey(chunkKey)) {
                        loadChunk(x, z);
                    }
                }
            }

            // Unload distant chunks
            Array<String> chunksToRemove = new Array<>();
            for (ObjectMap.Entry<String, Chunk> entry : loadedChunks.entries()) {
                Chunk chunk = entry.value;
                float distance = Vector3.dst(chunk.chunkX, 0, chunk.chunkZ, chunkX, 0, chunkZ);
                if (distance > RENDER_DISTANCE + 2) {
                    chunksToRemove.add(entry.key);
                }
            }

            for (String key : chunksToRemove) {
                Chunk chunk = loadedChunks.remove(key);
                chunk.dispose();
            }
        }
    }

    public void render(ModelBatch batch, Camera camera, Environment environment) {
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isVisible(camera)) {
                batch.render(chunk.getInstances(), environment);
            }
        }
    }

    private void loadChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ, worldGenerator, blockModels);
        chunk.generate();
        loadedChunks.put(chunkX + "," + chunkZ, chunk);
    }

    public BlockType getBlockAt(int x, int y, int z) {
        if (y < 0 || y >= WORLD_HEIGHT) {
            return y < 0 ? BlockType.STONE : BlockType.AIR;
        }

        int chunkX = MathUtils.floor((float)x / CHUNK_SIZE);
        int chunkZ = MathUtils.floor((float)z / CHUNK_SIZE);
        String chunkKey = chunkX + "," + chunkZ;

        if (loadedChunks.containsKey(chunkKey)) {
            return loadedChunks.get(chunkKey).getBlockAt(x - chunkX * CHUNK_SIZE, y, z - chunkZ * CHUNK_SIZE);
        } else {
            return worldGenerator.getBlockAt(x, y, z);
        }
    }

    public void setBlockAt(int x, int y, int z, BlockType blockType) {
        if (y < 0 || y >= WORLD_HEIGHT) {
            return;
        }

        int chunkX = MathUtils.floor((float)x / CHUNK_SIZE);
        int chunkZ = MathUtils.floor((float)z / CHUNK_SIZE);
        String chunkKey = chunkX + "," + chunkZ;

        if (loadedChunks.containsKey(chunkKey)) {
            loadedChunks.get(chunkKey).setBlockAt(x - chunkX * CHUNK_SIZE, y, z - chunkZ * CHUNK_SIZE, blockType);
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
        for (Chunk chunk : loadedChunks.values()) {
            chunk.dispose();
        }
        loadedChunks.clear();
    }
}
