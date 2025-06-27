package io.github.half.wfc;

import io.github.half.*;
import com.badlogic.gdx.graphics.g3d.Model;

public class WFCChunkManager extends ChunkManager {
    private IslandWorldGenerator islandGenerator;

    public WFCChunkManager(Model[] blockModels) {
        super(blockModels);
        this.islandGenerator = new IslandWorldGenerator();
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
