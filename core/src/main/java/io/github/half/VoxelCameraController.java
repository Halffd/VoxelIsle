
package io.github.half;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class VoxelCameraController implements InputProcessor {
    private PerspectiveCamera camera;
    private Vector3 velocity = new Vector3();
    private Vector3 tmp = new Vector3();

    // Movement settings
    private float moveSpeed = 50f;
    private float sprintMultiplier = 2f;
    private float touchSensitivity = 0.3f;

    // Get mouse sensitivity from settings
    private float getMouseSensitivity() {
        return GameSettings.getInstance().getMouseSensitivity();
    }

    // Input state
    private boolean[] keys = new boolean[256];
    private boolean isTouchDragging = false;
    private int lastTouchX, lastTouchY;

    public VoxelCameraController(PerspectiveCamera camera) {
        this.camera = camera;
    }

    public void update() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Handle movement
        velocity.setZero();

        if (keys[Input.Keys.W] || keys[Input.Keys.UP]) {
            tmp.set(camera.direction).nor();
            velocity.add(tmp);
        }
        if (keys[Input.Keys.S] || keys[Input.Keys.DOWN]) {
            tmp.set(camera.direction).nor().scl(-1);
            velocity.add(tmp);
        }
        if (keys[Input.Keys.A] || keys[Input.Keys.LEFT]) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(-1);
            velocity.add(tmp);
        }
        if (keys[Input.Keys.D] || keys[Input.Keys.RIGHT]) {
            tmp.set(camera.direction).crs(camera.up).nor();
            velocity.add(tmp);
        }
        if (keys[Input.Keys.SPACE]) {
            velocity.y += 1;
        }
        if (keys[Input.Keys.SHIFT_LEFT] || keys[Input.Keys.CONTROL_LEFT]) {
            velocity.y -= 1;
        }

        // Apply sprint
        float currentSpeed = moveSpeed;
        if (keys[Input.Keys.SHIFT_LEFT] && !keys[Input.Keys.CONTROL_LEFT]) {
            currentSpeed *= sprintMultiplier;
        }

        // Normalize and apply velocity
        if (velocity.len2() > 0) {
            velocity.nor().scl(currentSpeed * deltaTime);
            camera.position.add(velocity);
        }

        camera.update();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode >= 0 && keycode < keys.length) {
            keys[keycode] = true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode >= 0 && keycode < keys.length) {
            keys[keycode] = false;
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            isTouchDragging = true;
            lastTouchX = screenX;
            lastTouchY = screenY;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            isTouchDragging = false;
        }
        return true;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        boolean isCancelled = isTouchDragging;
        isTouchDragging = false;
        return isCancelled;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android && isTouchDragging) {
            // Touch look controls for Android
            float deltaX = (screenX - lastTouchX) * touchSensitivity;
            float deltaY = (screenY - lastTouchY) * touchSensitivity;

            camera.rotate(camera.up, -deltaX);
            camera.rotate(tmp.set(camera.direction).crs(camera.up).nor(), deltaY);

            lastTouchX = screenX;
            lastTouchY = screenY;
        } else if (Gdx.input.isCursorCatched()) {
            // Mouse look controls for PC
            float deltaX = -Gdx.input.getDeltaX() * getMouseSensitivity();
            float deltaY = -Gdx.input.getDeltaY() * getMouseSensitivity();

            camera.rotate(camera.up, deltaX);
            camera.rotate(tmp.set(camera.direction).crs(camera.up).nor(), deltaY);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (Gdx.input.isCursorCatched()) {
            float deltaX = -Gdx.input.getDeltaX() * getMouseSensitivity();
            float deltaY = -Gdx.input.getDeltaY() * getMouseSensitivity();

            camera.rotate(camera.up, deltaX);
            camera.rotate(tmp.set(camera.direction).crs(camera.up).nor(), deltaY);
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
