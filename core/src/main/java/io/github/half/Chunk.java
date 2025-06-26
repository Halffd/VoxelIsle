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
    private Array<ModelInstance> renderInstances; // NOVO: Snapshot thread-safe para render
    private WorldGenerator worldGenerator;
    private Model[] blockModels;
    private BoundingBox boundingBox;
    private boolean generated = false;
    private boolean needsRebuild = false;
    private boolean meshReady = false; // NOVO: Flag para saber se mesh está pronto

    public Chunk(int chunkX, int chunkZ, WorldGenerator worldGenerator, Model[] blockModels) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldGenerator = worldGenerator;
        this.blockModels = blockModels;
        this.blocks = new BlockType[CHUNK_SIZE][WORLD_HEIGHT][CHUNK_SIZE];
        this.instances = new Array<>();
        this.renderInstances = new Array<>(); // NOVO: Inicializa snapshot

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

    // THREAD-SAFE createMesh
    public void createMesh() {
        // Cria array local para não interferir no rendering
        Array<ModelInstance> newInstances = new Array<>();
        int totalBlocks = 0;
        int visibleBlocks = 0;
        int addedInstances = 0;

        System.out.println("Creating mesh for chunk (" + chunkX + ", " + chunkZ + ")");

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    try {
                        BlockType blockType = blocks[x][y][z];
                        totalBlocks++;

                        if (blockType != null && blockType != BlockType.AIR) {
                            if (isBlockVisible(x, y, z)) {
                                visibleBlocks++;

                                if (blockModels != null && blockType.ordinal() < blockModels.length
                                    && blockModels[blockType.ordinal()] != null) {

                                    ModelInstance instance = new ModelInstance(blockModels[blockType.ordinal()]);
                                    float worldX = chunkX * CHUNK_SIZE + x;
                                    float worldZ = chunkZ * CHUNK_SIZE + z;
                                    instance.transform.setToTranslation(worldX, y, worldZ);
                                    newInstances.add(instance);
                                    addedInstances++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error at (" + x + "," + y + "," + z + "): " + e);
                    }
                }
            }
        }

        // ATOMIC SWAP - só uma operação thread-safe
        synchronized (this) {
            instances.clear();
            instances.addAll(newInstances);

            // Cria snapshot para rendering
            renderInstances.clear();
            renderInstances.addAll(newInstances);

            meshReady = true;
        }

        System.out.println("Chunk (" + chunkX + ", " + chunkZ + ") - Total: " + totalBlocks +
            ", Visible: " + visibleBlocks + ", Instances: " + addedInstances);
        needsRebuild = false;
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

    // THREAD-SAFE getInstances - retorna snapshot
    public Array<ModelInstance> getInstances() {
        synchronized (this) {
            if (!meshReady) {
                return new Array<>(); // Retorna array vazio se mesh não está pronto
            }

            // Retorna cópia do snapshot (extra safe)
            return new Array<>(renderInstances);
        }
    }

    public void dispose() {
        synchronized (this) {
            instances.clear();
            renderInstances.clear();
            meshReady = false;
        }
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
        // Mesh rebuild will be queued by World class
    }

    public void update() {
        if (needsRebuild) {
            createMesh();
            needsRebuild = false;
        }
    }

    // NOVO: Método para verificar se chunk está pronto para render
    public boolean isReady() {
        synchronized (this) {
            return meshReady && renderInstances.size > 0;
        }
    }
}
