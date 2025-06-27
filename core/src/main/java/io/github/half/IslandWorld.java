package io.github.half;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import io.github.half.wfc.*;
public class IslandWorld extends World {
    private WFCChunkManager wfcChunkManager;

    public IslandWorld(Model[] blockModels) {
        super(blockModels);
        // Replace the standard chunk manager with WFC version
        this.chunkManager = new WFCChunkManager(blockModels);
        this.wfcChunkManager = (WFCChunkManager) this.chunkManager;

        System.out.println("Island World initialized with WFC generation");
        System.out.println("Ocean coverage: " + (IslandConfig.OCEAN_COVERAGE * 100) + "%");
        System.out.println("Island density: " + IslandConfig.ISLAND_DENSITY);
    }

    @Override
    public void update(Vector3 playerPosition) {
        super.update(playerPosition);

        // Optional: Debug info
        if (Math.random() < 0.001) { // Occasionally print stats
            System.out.println("Loaded chunks: " + getLoadedChunksCount());
            System.out.println("Player position: " + playerPosition);
        }
    }
}
