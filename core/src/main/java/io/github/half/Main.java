package io.github.half;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private ModelBatch modelBatch;
    private Environment environment;

    // World settings
    private static final int WORLD_SIZE = 640; // 10x larger
    private static final int WORLD_HEIGHT = 64;
    private static final int CHUNK_SIZE = 16;
    private static final int WATER_LEVEL = 32;

    // Game objects
    private Player player;
    private World world;
    private UIRenderer uiRenderer;

    private Model[] blockModels;

    @Override
    public void create() {
        // Initialize game settings
        GameSettings.getInstance();

        // Initialize rendering
        modelBatch = new ModelBatch();

        // Set up environment lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Create block models
        createBlockModels();

        // Initialize world
        world = new World(blockModels);

        // Initialize player
        player = new Player(WORLD_SIZE / 2f, WORLD_HEIGHT + 10f, WORLD_SIZE / 2f);

        // Create UI renderer
        uiRenderer = new UIRenderer();

        // Set input processor to player's camera
        Gdx.input.setInputProcessor(new InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.app.exit();
                }
                if (keycode == Input.Keys.TAB) {
                    Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
                }
                return false;
            }

            // Implementa os outros métodos como false
            @Override public boolean keyUp(int keycode) { return false; }
            @Override public boolean keyTyped(char character) { return false; }
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
            @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

            @Override
            public boolean touchCancelled(int i, int i1, int i2, int i3) {
                return false;
            }

            @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
            @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
            @Override public boolean scrolled(float amountX, float amountY) { return false; }
        });
        Gdx.input.setCursorCatched(true);

        System.out.println("Voxel world initialized. World size: " + WORLD_SIZE + "x" + WORLD_HEIGHT + "x" + WORLD_SIZE);
    }

    private void createBlockModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        blockModels = new Model[BlockType.values().length];

        // Basic blocks
        blockModels[BlockType.STONE.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.DIRT.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.6f, 0.4f, 0.2f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.GRASS.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.8f, 0.2f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.SAND.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.9f, 0.8f, 0.4f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.WATER.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.5f, 1f, 0.7f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Ores and special blocks
        blockModels[BlockType.COAL.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.2f, 0.2f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.IRON.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.7f, 0.5f, 0.3f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.GOLD.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(1f, 0.8f, 0f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.DIAMOND.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.6f, 0.9f, 1f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.CRYSTAL.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.9f, 0.2f, 0.9f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        blockModels[BlockType.OIL.ordinal()] = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.1f, 0.1f, 0.1f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
    }

    // Gravity toggle cooldown
    private float gravityToggleCooldown = 0f;
    private static final float GRAVITY_TOGGLE_COOLDOWN_TIME = 0.5f; // Half a second cooldown

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Update gravity toggle cooldown
        if (gravityToggleCooldown > 0) {
            gravityToggleCooldown -= deltaTime;
        }

        // Check for gravity toggle with cooldown
        if (Gdx.input.isKeyJustPressed(Input.Keys.G) && gravityToggleCooldown <= 0) {
            GameSettings.getInstance().togglePlayerGravity();
            gravityToggleCooldown = GRAVITY_TOGGLE_COOLDOWN_TIME;
        }

        // Update game logic
        player.update(deltaTime, world);
        world.update(player.getPosition());

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);

        // Render world
        modelBatch.begin(player.getCamera());
        world.render(modelBatch, player.getCamera(), environment);
        modelBatch.end();

        // Render UI
        uiRenderer.render(player);
    }

    // Input processor for player's camera
    private class PlayerInputProcessor implements InputProcessor {
        private PerspectiveCamera camera;

        public PlayerInputProcessor(PerspectiveCamera camera) {
            this.camera = camera;
        }

        private float getMouseSensitivity() {
            return GameSettings.getInstance().getMouseSensitivity();
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                if (Gdx.input.isCursorCatched()) {
                    Gdx.input.setCursorCatched(false);
                } else {
                    Gdx.app.exit();
                }
            }

            if (keycode == Input.Keys.TAB || keycode == Input.Keys.ENTER) {
                boolean newCatchState = !Gdx.input.isCursorCatched();
                Gdx.input.setCursorCatched(newCatchState);

                // Center cursor when grabbing
                if (newCatchState) {
                    Gdx.input.setCursorPosition(
                        Gdx.graphics.getWidth() / 2,
                        Gdx.graphics.getHeight() / 2);
                }
            }

            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (Gdx.input.isCursorCatched()) {
                float deltaX = -Gdx.input.getDeltaX() * getMouseSensitivity();
                float deltaY = -Gdx.input.getDeltaY() * getMouseSensitivity();

                // Rotate camera
                camera.rotate(camera.up, deltaX);
                camera.rotate(new Vector3(camera.direction).crs(camera.up).nor(), deltaY);
                camera.update();
            }
            return true;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            if (Gdx.input.isCursorCatched()) {
                float deltaX = -Gdx.input.getDeltaX() * getMouseSensitivity();
                float deltaY = -Gdx.input.getDeltaY() * getMouseSensitivity();

                // Rotate camera
                camera.rotate(camera.up, deltaX);
                camera.rotate(new Vector3(camera.direction).crs(camera.up).nor(), deltaY);
                camera.update();
            }
            return true;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            return false;
        }
    }

    @Override
    public void resize(int width, int height) {
        player.getCamera().viewportWidth = width;
        player.getCamera().viewportHeight = height;
        player.getCamera().update();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        if (blockModels != null) {
            for (Model model : blockModels) {
                if (model != null) {
                    model.dispose();
                }
            }
        }
        world.dispose();
        uiRenderer.dispose();
    }
}
