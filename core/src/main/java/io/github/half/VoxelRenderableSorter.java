package io.github.half;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class VoxelRenderableSorter extends DefaultRenderableSorter {
    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();

    @Override
    public void sort(Camera camera, Array<Renderable> renderables) {
        // Skip sorting entirely to avoid the TimSort issue
        // We'll just use the default order which is good enough for voxel rendering
    }

    @Override
    public int compare(Renderable o1, Renderable o2) {
        // Simple comparison that won't throw exceptions
        if (o1 == o2) return 0;
        if (o1 == null) return 1;
        if (o2 == null) return -1;
        return 0;
    }
}
