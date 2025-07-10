package io.github.half;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;

public class VoxelCameraController extends InputAdapter {
    private final Camera camera;
    private final Vector3 tmp = new Vector3();
    private float velocity = 5;
    private float degreesPerPixel = 0.5f;

    public VoxelCameraController(Camera camera) {
        this.camera = camera;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (Gdx.input.isCursorCatched()) {
            float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
            float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
            camera.direction.rotate(camera.up, deltaX);
            tmp.set(camera.direction).crs(camera.up).nor();
            camera.direction.rotate(tmp, deltaY);
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            Gdx.input.setCursorCatched(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ENTER) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }
        return false;
    }
}