package io.github.half;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class UIRenderer {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private OrthographicCamera camera;
    private ScreenViewport viewport;
    private TextureRegion[] blockTextures;
    private TextureRegion[] toolTextures;

    private static final int HOTBAR_SIZE = 9;
    private static final float HOTBAR_WIDTH = 400f;
    private static final float HOTBAR_HEIGHT = 40f;
    private static final float HOTBAR_ITEM_SIZE = 32f;
    private static final float HOTBAR_PADDING = 4f;

    public UIRenderer() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.apply();
        camera.update();

        loadTextures();
    }

    private void loadTextures() {
        // In a real implementation, load actual textures for blocks and tools
        // Here we'll just create placeholder colors
        blockTextures = new TextureRegion[BlockType.values().length];
        toolTextures = new TextureRegion[Tool.values().length];

        // Placeholder: use colored squares for now
        Texture pixelTexture = new Texture(1, 1, Gdx.graphics.getFormat());
        TextureRegion whitePixel = new TextureRegion(pixelTexture);

        for (BlockType type : BlockType.values()) {
            blockTextures[type.ordinal()] = whitePixel;
        }

        for (Tool tool : Tool.values()) {
            toolTextures[tool.ordinal()] = whitePixel;
        }
    }

    public void render(Player player) {
        // Update viewport if window size changed
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Draw UI components
        drawCrosshair();
        drawHotbar(player);
        drawBreakingProgress(player);
        drawInventory(player);
    }

    private void drawCrosshair() {
        // Prepare shape renderer
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw crosshair at center of screen
        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;
        float size = 10f;

        shapeRenderer.setColor(1f, 1f, 1f, 0.8f);

        // Horizontal line
        shapeRenderer.rectLine(centerX - size, centerY, centerX + size, centerY, 2f);
        // Vertical line
        shapeRenderer.rectLine(centerX, centerY - size, centerX, centerY + size, 2f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHotbar(Player player) {
        // Draw hotbar at bottom center of screen
        float startX = (viewport.getWorldWidth() - HOTBAR_WIDTH) / 2f;
        float startY = 20f;

        // Draw hotbar background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(startX, startY, HOTBAR_WIDTH, HOTBAR_HEIGHT);
        shapeRenderer.end();

        // Draw slots and items
        Inventory inventory = player.getInventory();
        float slotWidth = HOTBAR_WIDTH / HOTBAR_SIZE;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int i = 0; i < HOTBAR_SIZE; i++) {
            float itemX = startX + i * slotWidth + HOTBAR_PADDING;
            float itemY = startY + HOTBAR_PADDING;

            // Draw slot background (white outline for selected slot)
            if (player.getSelectedBlockType() == inventory.getBlockTypeAt(i)) {
                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                batch.setColor(0.3f, 0.3f, 0.3f, 1f);
            }

            batch.draw(blockTextures[0], itemX, itemY, slotWidth - HOTBAR_PADDING * 2, HOTBAR_HEIGHT - HOTBAR_PADDING * 2);

            // Draw block icon if available
            if (i < inventory.getAllBlocks().size) {
                BlockType blockType = inventory.getBlockTypeAt(i);

                if (blockType != null && blockType != BlockType.AIR) {
                    // Draw colored icon based on block type
                    switch (blockType) {
                        case STONE: batch.setColor(0.5f, 0.5f, 0.5f, 1f); break;
                        case DIRT: batch.setColor(0.6f, 0.4f, 0.2f, 1f); break;
                        case GRASS: batch.setColor(0.3f, 0.8f, 0.2f, 1f); break;
                        case SAND: batch.setColor(0.9f, 0.8f, 0.4f, 1f); break;
                        case WATER: batch.setColor(0.2f, 0.5f, 1f, 0.7f); break;
                        case COAL: batch.setColor(0.2f, 0.2f, 0.2f, 1f); break;
                        case IRON: batch.setColor(0.7f, 0.5f, 0.3f, 1f); break;
                        case GOLD: batch.setColor(1f, 0.8f, 0f, 1f); break;
                        case DIAMOND: batch.setColor(0.6f, 0.9f, 1f, 1f); break;
                        case CRYSTAL: batch.setColor(0.9f, 0.2f, 0.9f, 1f); break;
                        case OIL: batch.setColor(0.1f, 0.1f, 0.1f, 1f); break;
                        case WOOD: batch.setColor(0.6f, 0.3f, 0.1f, 1f); break;
                        default: batch.setColor(1f, 1f, 1f, 1f);
                    }

                    batch.draw(blockTextures[blockType.ordinal()],
                              itemX + 4, itemY + 4,
                              HOTBAR_ITEM_SIZE, HOTBAR_ITEM_SIZE);

                    // Draw count
                    batch.setColor(1f, 1f, 1f, 1f);
                    font.draw(batch, "" + inventory.getBlockCount(blockType),
                              itemX + slotWidth - 14, itemY + 14);
                }
            }

            // Draw slot number
            batch.setColor(1f, 1f, 1f, 0.8f);
            font.draw(batch, "" + (i + 1), itemX + 4, itemY + HOTBAR_HEIGHT - 6);
        }

        // Draw current tool
        float toolX = startX + HOTBAR_WIDTH + 10;
        float toolY = startY;

        batch.setColor(1f, 1f, 1f, 0.8f);
        font.draw(batch, "Tool: " + player.getCurrentTool().name(), toolX, toolY + HOTBAR_HEIGHT - 6);

        batch.end();
    }

    private void drawBreakingProgress(Player player) {
        if (player.getBreakingProgress() > 0) {
            Vector3 lookingAt = player.getLookingAt();
            if (lookingAt.x >= 0) { // Valid block being broken
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

                // Draw breaking progress bar at center of screen
                float centerX = viewport.getWorldWidth() / 2f;
                float centerY = viewport.getWorldHeight() / 2f + 30f;
                float width = 100f;
                float height = 10f;

                // Background
                shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
                shapeRenderer.rect(centerX - width/2f, centerY, width, height);

                // Progress
                shapeRenderer.setColor(1f, 0.3f, 0.3f, 0.8f);
                shapeRenderer.rect(centerX - width/2f, centerY, width * player.getBreakingProgress(), height);

                shapeRenderer.end();
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }
        }
    }

    private void drawInventory(Player player) {
        // Draw block counts in top right corner
        float startX = viewport.getWorldWidth() - 150;
        float startY = viewport.getWorldHeight() - 20;

        batch.begin();

        // Draw current tool
        batch.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, "Tool: " + player.getCurrentTool().name(), startX, startY);
        startY -= 20;

        // Draw each block type and count
        ObjectIntMap<BlockType> blocks = player.getInventory().getAllBlocks();
        for (BlockType type : BlockType.values()) {
            if (type != BlockType.AIR && type != BlockType.WATER && blocks.containsKey(type)) {
                int count = blocks.get(type, 0);
                if (count > 0) {
                    batch.setColor(1f, 1f, 1f, 1f);
                    font.draw(batch, type.name() + ": " + count, startX, startY);
                    startY -= 20;

                    if (startY < 100) break; // Prevent too many entries
                }
            }
        }

        batch.end();
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
