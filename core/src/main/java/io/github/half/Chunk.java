
package io.github.half;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

public class Chunk {
    public final int chunkX, chunkZ;
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 64;

    private BlockType[][][] blocks;
    private Array<ModelInstance> instances;
    private WorldGenerator worldGenerator;
    private Model[] blockModels;
    private BoundingBox boundingBox;
    private boolean generated = false;
    private boolean needsRebuild = false;

    public Chunk(int chunkX, int chunkZ, WorldGenerator worldGenerator, Model[] blockModels) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldGenerator = worldGenerator;
        this.blockModels = blockModels;
        this.blocks = new BlockType[CHUNK_SIZE][WORLD_HEIGHT][CHUNK_SIZE];
        this.instances = new Array<>();

        // Calculate bounding box
        float minX = chunkX * CHUNK_SIZE;
        float minZ = chunkZ * CHUNK_SIZE;
        float maxX = minX + CHUNK_SIZE;
        float maxZ = minZ + CHUNK_SIZE;
        boundingBox = new BoundingBox(new Vector3(minX, 0, minZ), new Vector3(maxX, WORLD_HEIGHT, maxZ));
    }

    public void generate() {
        if (generated) return;

        // Generate blocks
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    int worldX = chunkX * CHUNK_SIZE + x;
                    int worldZ = chunkZ * CHUNK_SIZE + z;
                    blocks[x][y][z] = worldGenerator.getBlockAt(worldX, y, worldZ);
                }
            }
        }

        // Create mesh instances
        createMesh();
        generated = true;
    }

    private void createMesh() {
        instances.clear();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    BlockType blockType = blocks[x][y][z];

                    if (blockType != BlockType.AIR && isBlockVisible(x, y, z)) {
                        ModelInstance instance = new ModelInstance(blockModels[blockType.ordinal()]);
                        float worldX = chunkX * CHUNK_SIZE + x;
                        float worldZ = chunkZ * CHUNK_SIZE + z;
                        instance.transform.setToTranslation(worldX, y, worldZ);
                        instances.add(instance);
                    }
                }
            }
        }
    }

    private boolean isBlockVisible(int x, int y, int z) {
        // Check if any adjacent face is exposed to air
        return isAir(x + 1, y, z) || isAir(x - 1, y, z) ||
               isAir(x, y + 1, z) || isAir(x, y - 1, z) ||
               isAir(x, y, z + 1) || isAir(x, y, z - 1);
    }

    private boolean isAir(int x, int y, int z) {
        if (y < 0 || y >= WORLD_HEIGHT) {
            return y >= WORLD_HEIGHT; // Above world is air, below is solid
        }

        // Check within chunk bounds
        if (x >= 0 && x < CHUNK_SIZE && z >= 0 && z < CHUNK_SIZE) {
            return blocks[x][y][z] == BlockType.AIR;
        }

        // Outside chunk bounds - check with world generator
        int worldX = chunkX * CHUNK_SIZE + x;
        int worldZ = chunkZ * CHUNK_SIZE + z;
        return worldGenerator.getBlockAt(worldX, y, worldZ) == BlockType.AIR;
    }

    public boolean isVisible(Camera camera) {
        return camera.frustum.boundsInFrustum(boundingBox);
    }

    public Array<ModelInstance> getInstances() {
        return instances;
    }

    public void dispose() {
        // Models are shared, don't dispose them here
        instances.clear();
    }

    public BlockType getBlockAt(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= WORLD_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return BlockType.AIR;
        }
        return blocks[x][y][z];
    }

    public void setBlockAt(int x, int y, int z, BlockType blockType) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= WORLD_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        blocks[x][y][z] = blockType;
        needsRebuild = true;
        createMesh(); // Rebuild mesh immediately
    }

    public void update() {
        if (needsRebuild) {
            createMesh();
            needsRebuild = false;
        }
    }
}
