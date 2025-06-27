package io.github.half.wfc;

import io.github.half.*;
import com.badlogic.gdx.graphics.g3d.Model;

public class WFCChunk extends Chunk {
    private IslandWorldGenerator islandGenerator;

    public WFCChunk(int chunkX, int chunkZ, IslandWorldGenerator generator, Model[] blockModels) {
        super(chunkX, chunkZ, generator, blockModels);
        this.islandGenerator = generator;
    }

    @Override
    public void generate() {
        System.out.println("Generating WFC chunk at " + chunkX + ", " + chunkZ);

        // Use WFC-based island generation
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = 0; y < WORLD_HEIGHT; y++) {
                    int worldX = chunkX * CHUNK_SIZE + x;
                    int worldZ = chunkZ * CHUNK_SIZE + z;

                    BlockType blockType = islandGenerator.getBlockAt(worldX, y, worldZ);
                    setBlockAt(x, y, z, blockType);
                }
            }
        }

        generated = true;
        createMesh();
    }
}
