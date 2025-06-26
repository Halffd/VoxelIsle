package io.github.half;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
    private PerspectiveCamera camera;
    private VoxelCameraController cameraController;
    private ModelBatch modelBatch;
    private Environment environment;

    // World settings
    private static final int WORLD_SIZE = 640; // 10x larger
    private static final int WORLD_HEIGHT = 64;
    private static final int CHUNK_SIZE = 16;
    private static final int WATER_LEVEL = 32;
    private static final int RENDER_DISTANCE = 8; // chunks

    // Chunk management
    private ObjectMap<String, Chunk> loadedChunks;
    private Vector3 lastPlayerChunk;
    private WorldGenerator worldGenerator;

    // Block types
    private enum BlockType {
        AIR, STONE, DIRT, GRASS, SAND, WATER, COAL, IRON, GOLD, DIAMOND, CRYSTAL, OIL
    }

    private Model[] blockModels;

    @Override
    public void create() {
        // Initialize camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(WORLD_SIZE / 2f, WORLD_HEIGHT + 20f, WORLD_SIZE / 2f);
        camera.lookAt(WORLD_SIZE / 2f, WORLD_HEIGHT, WORLD_SIZE / 2f - 10);
        camera.near = 0.1f;
        camera.far = 500f;
        camera.update();

        // Initialize camera controller with mouse grabbing
        cameraController = new VoxelCameraController(camera);
        Gdx.input.setInputProcessor(cameraController);
        Gdx.input.setCursorCatched(true);

        // Initialize rendering
        modelBatch = new ModelBatch();

        // Set up environment lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Initialize world system
        loadedChunks = new ObjectMap<>();
        lastPlayerChunk = new Vector3(-1, -1, -1);
        worldGenerator = new WorldGenerator();

        createBlockModels();

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

    @Override
    public void render() {
        handleInput();
        cameraController.update();
        updateChunks();

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);

        // Render visible chunks
        modelBatch.begin(camera);
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isVisible(camera)) {
                modelBatch.render(chunk.getInstances(), environment);
            }
        }
        modelBatch.end();
    }

    private void updateChunks() {
        // Get current player chunk position
        int chunkX = (int) (camera.position.x / CHUNK_SIZE);
        int chunkZ = (int) (camera.position.z / CHUNK_SIZE);

        Vector3 currentChunk = new Vector3(chunkX, 0, chunkZ);

        // Only update if player moved to a different chunk
        if (!currentChunk.equals(lastPlayerChunk)) {
            lastPlayerChunk.set(currentChunk);

            // Load new chunks in render distance
            for (int x = chunkX - RENDER_DISTANCE; x <= chunkX + RENDER_DISTANCE; x++) {
                for (int z = chunkZ - RENDER_DISTANCE; z <= chunkZ + RENDER_DISTANCE; z++) {
                    String chunkKey = x + "," + z;
                    if (!loadedChunks.containsKey(chunkKey)) {
                        loadChunk(x, z);
                    }
                }
            }

            // Unload distant chunks
            Array<String> chunksToRemove = new Array<>();
            for (ObjectMap.Entry<String, Chunk> entry : loadedChunks.entries()) {
                Chunk chunk = entry.value;
                float distance = Vector3.dst(chunk.chunkX, 0, chunk.chunkZ, chunkX, 0, chunkZ);
                if (distance > RENDER_DISTANCE + 2) {
                    chunksToRemove.add(entry.key);
                }
            }

            for (String key : chunksToRemove) {
                Chunk chunk = loadedChunks.remove(key);
                chunk.dispose();
            }

            System.out.println("Loaded chunks: " + loadedChunks.size + " | Player chunk: " + chunkX + "," + chunkZ);
        }
    }

    private void loadChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ, worldGenerator, blockModels);
        chunk.generate();
        loadedChunks.put(chunkX + "," + chunkZ, chunk);
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (Gdx.input.isCursorCatched()) {
                Gdx.input.setCursorCatched(false);
            } else {
                Gdx.app.exit();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }

        // Regenerate current chunk
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            int chunkX = (int) (camera.position.x / CHUNK_SIZE);
            int chunkZ = (int) (camera.position.z / CHUNK_SIZE);
            String chunkKey = chunkX + "," + chunkZ;

            Chunk oldChunk = loadedChunks.remove(chunkKey);
            if (oldChunk != null) {
                oldChunk.dispose();
            }
            loadChunk(chunkX, chunkZ);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
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
        for (Chunk chunk : loadedChunks.values()) {
            chunk.dispose();
        }
        loadedChunks.clear();
    }
}
