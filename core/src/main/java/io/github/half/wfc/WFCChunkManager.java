package io.github.half.wfc;

import io.github.half.*;
import com.badlogic.gdx.graphics.g3d.Model;

public class WFCChunkManager extends ChunkManager {
    private IslandWorldGenerator islandGenerator;

    public WFCChunkManager(Model[] blockModels) {
        super(blockModels);
        this.islandGenerator = new IslandWorldGenerator();
    }

    // Allow callers to bias generation near spawn
    public void setSpawnHint(int x, int z, int radius) {
        if (islandGenerator != null) {
            islandGenerator.setSpawnHint(x, z, radius);
        }
    }

    // Synchronously prewarm using WFC chunks so visuals and collisions match
    @Override
    public void prewarmAreaWorld(int centerWorldX, int centerWorldZ, int radiusChunks) {
        int centerChunkX = (int) Math.floor((float) centerWorldX / 16f);
        int centerChunkZ = (int) Math.floor((float) centerWorldZ / 16f);

        for (int cx = centerChunkX - radiusChunks; cx <= centerChunkX + radiusChunks; cx++) {
            for (int cz = centerChunkZ - radiusChunks; cz <= centerChunkZ + radiusChunks; cz++) {
                if (!containsLoadedChunk(cx, cz)) {
                    WFCChunk chunk = new WFCChunk(cx, cz, islandGenerator, blockModels);
                    chunk.generate();
                    putLoadedChunk(chunk);
                }
            }
        }
    }

    @Override
    public void queueChunkOperation(ChunkOperation operation) {
        if (operation.type == ChunkOperation.Type.GENERATE) {
            // Replace with WFC chunk
            WFCChunk wfcChunk = new WFCChunk(
                operation.chunk.chunkX,
                operation.chunk.chunkZ,
                islandGenerator,
                blockModels
            );

            ChunkOperation wfcOperation = new ChunkOperation(
                ChunkOperation.Type.GENERATE,
                wfcChunk
            );

            super.queueChunkOperation(wfcOperation);
        } else {
            super.queueChunkOperation(operation);
        }
    }
}
