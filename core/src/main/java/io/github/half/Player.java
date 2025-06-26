package io.github.half;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

public class Player {
    // Player physics constants
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_EYE_HEIGHT = 1.65f;
    private static final float GRAVITY = -20f;
    private static final float JUMP_VELOCITY = 8f;
    private static final float WALK_SPEED = 5f;
    private static final float RUN_SPEED = 8f;
    private static final float SWIM_SPEED = 3f;
    private static final float WATER_LEVEL = 32f;

    // Camera and position
    private PerspectiveCamera camera;
    private Vector3 position;
    private Vector3 velocity;
    private Vector3 acceleration;
    private BoundingBox boundingBox;
    private boolean onGround;
    private boolean isSwimming;
    private boolean isSprinting;
    private boolean isJumping;
    private ViewMode viewMode;
    private float bobTimer;
    private float bobAmplitude;

    // Block interaction
    private Vector3 lookingAt;
    private BlockFace lookingAtFace;
    private Vector3 placementPosition;
    private float interactionReach = 5f;
    private float breakingProgress;
    private BlockType selectedBlockType;

    // Inventory
    private Inventory inventory;

    // Tool
    private Tool currentTool;

    public enum ViewMode {
        FIRST_PERSON,
        THIRD_PERSON
    }

    public Player(float startX, float startY, float startZ) {
        position = new Vector3(startX, startY, startZ);
        velocity = new Vector3();
        acceleration = new Vector3(0, GRAVITY, 0); // Default gravity
        boundingBox = new BoundingBox(
                new Vector3(-PLAYER_WIDTH/2, 0, -PLAYER_WIDTH/2),
                new Vector3(PLAYER_WIDTH/2, PLAYER_HEIGHT, PLAYER_WIDTH/2));

        // Initialize camera
        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        // Set defaults
        viewMode = ViewMode.FIRST_PERSON;
        lookingAt = new Vector3();
        placementPosition = new Vector3();
        bobTimer = 0;
        bobAmplitude = 0.05f;
        onGround = false;
        isSwimming = false;
        isSprinting = false;
        isJumping = false;
        breakingProgress = 0f;
        selectedBlockType = BlockType.DIRT; // Default block to place

        // Initialize inventory
        inventory = new Inventory();

        // Initial tools
        currentTool = Tool.HAND;
        inventory.addTool(Tool.HAND);

        updateCamera();
    }

    public void update(float deltaTime, World world) {
        handleInput(deltaTime);
        updatePhysics(deltaTime, world);
        updateCamera();
        updateBlockInteraction(world);
    }

    private void handleInput(float deltaTime) {
        // Toggle view mode
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            toggleViewMode();
        }

        // Toggle gravity mode
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            GameSettings.getInstance().togglePlayerGravity();
            GameSettings.getInstance().saveSettings();
        }

        // Jumping
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (onGround) {
                velocity.y = JUMP_VELOCITY;
                isJumping = true;
            } else if (isSwimming) {
                velocity.y = JUMP_VELOCITY * 0.5f;
            }
        }

        // Sprinting
        isSprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        // Select block type for placement (1-9 keys)
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                selectedBlockType = inventory.getBlockTypeAt(i);
            }
        }

        // Select tool (F1-F5 keys)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) currentTool = Tool.HAND;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2) && inventory.hasTool(Tool.WOOD_PICKAXE)) currentTool = Tool.WOOD_PICKAXE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3) && inventory.hasTool(Tool.STONE_PICKAXE)) currentTool = Tool.STONE_PICKAXE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4) && inventory.hasTool(Tool.IRON_PICKAXE)) currentTool = Tool.IRON_PICKAXE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5) && inventory.hasTool(Tool.DIAMOND_PICKAXE)) currentTool = Tool.DIAMOND_PICKAXE;
    }

    private void updatePhysics(float deltaTime, World world) {
        Vector3 oldPosition = new Vector3(position);

        // Check if in water
        isSwimming = position.y < WATER_LEVEL;

        // Apply acceleration (gravity) if enabled
        if (GameSettings.getInstance().isPlayerGravityEnabled()) {
            if (isSwimming) {
                // Buoyancy - reduced gravity in water
                acceleration.y = GRAVITY * 0.3f;
            } else {
                acceleration.y = GRAVITY;
            }
        } else {
            // No gravity when disabled
            acceleration.y = 0;
        }

        // Apply acceleration to velocity
        velocity.add(acceleration.x * deltaTime, acceleration.y * deltaTime, acceleration.z * deltaTime);

        // Apply drag in water
        if (isSwimming) {
            velocity.scl(0.9f);
        }

        // Movement input
        Vector3 movement = new Vector3();
        float speed = isSwimming ? SWIM_SPEED : (isSprinting ? RUN_SPEED : WALK_SPEED);

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            movement.set(camera.direction.x, 0, camera.direction.z).nor().scl(speed);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            movement.set(-camera.direction.x, 0, -camera.direction.z).nor().scl(speed);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            Vector3 left = new Vector3(camera.direction).crs(camera.up).nor().scl(-speed);
            movement.add(left.x, 0, left.z);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            Vector3 right = new Vector3(camera.direction).crs(camera.up).nor().scl(speed);
            movement.add(right.x, 0, right.z);
        }

        // Normalize movement if moving diagonally
        if (movement.len() > speed) {
            movement.nor().scl(speed);
        }

        // Add movement to velocity (only X and Z)
        velocity.x = movement.x;
        velocity.z = movement.z;

        // Apply velocity to position
        position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Update bounding box position
        updateBoundingBox();

        // Collision detection and response
        handleCollisions(world, oldPosition);

        // Update head bob effect while moving
        if (onGround && (velocity.x != 0 || velocity.z != 0)) {
            bobTimer += deltaTime * (isSprinting ? 2f : 1f);
            if (bobTimer > MathUtils.PI2) bobTimer -= MathUtils.PI2;
        }
    }

    private void handleCollisions(World world, Vector3 oldPosition) {
        // Determine which blocks the player is colliding with
        int minX = MathUtils.floor(boundingBox.min.x);
        int minY = MathUtils.floor(boundingBox.min.y);
        int minZ = MathUtils.floor(boundingBox.min.z);
        int maxX = MathUtils.floor(boundingBox.max.x);
        int maxY = MathUtils.floor(boundingBox.max.y);
        int maxZ = MathUtils.floor(boundingBox.max.z);

        boolean collisionY = false;
        onGround = false;

        // Check all potentially colliding blocks
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType blockType = world.getBlockAt(x, y, z);

                    if (blockType != null && blockType.isSolid()) {
                        // Create block bounding box
                        BoundingBox blockBox = new BoundingBox(
                                new Vector3(x, y, z),
                                new Vector3(x + 1, y + 1, z + 1));

                        // Check for collision
                        if (boundingBox.intersects(blockBox)) {
                            // Handle collision by separating the player from the block
                            handleBlockCollision(blockBox, oldPosition);

                            // Check if player is standing on ground
                            if (blockBox.max.y <= position.y + 0.1f && velocity.y <= 0) {
                                onGround = true;
                                collisionY = true;
                            }
                        }
                    }
                }
            }
        }

        // Reset Y velocity if colliding with ground or ceiling
        if (collisionY) {
            velocity.y = 0;
            isJumping = false;
        }

        // Update bounding box after collision resolution
        updateBoundingBox();
    }

    private void handleBlockCollision(BoundingBox blockBox, Vector3 oldPosition) {
        // Determine penetration on each axis
        float xOverlap = Math.min(boundingBox.max.x - blockBox.min.x, blockBox.max.x - boundingBox.min.x);
        float yOverlap = Math.min(boundingBox.max.y - blockBox.min.y, blockBox.max.y - boundingBox.min.y);
        float zOverlap = Math.min(boundingBox.max.z - blockBox.min.z, blockBox.max.z - boundingBox.min.z);

        // Find the minimum penetration axis
        if (xOverlap < yOverlap && xOverlap < zOverlap) {
            // X-axis has minimum penetration
            if (position.x > blockBox.getCenter(new Vector3()).x) {
                position.x += xOverlap;
            } else {
                position.x -= xOverlap;
            }
            velocity.x = 0;
        } else if (yOverlap < xOverlap && yOverlap < zOverlap) {
            // Y-axis has minimum penetration
            if (position.y > blockBox.getCenter(new Vector3()).y) {
                position.y += yOverlap;
            } else {
                position.y -= yOverlap;
            }
            velocity.y = 0;
        } else {
            // Z-axis has minimum penetration
            if (position.z > blockBox.getCenter(new Vector3()).z) {
                position.z += zOverlap;
            } else {
                position.z -= zOverlap;
            }
            velocity.z = 0;
        }

        // Update bounding box after position change
        updateBoundingBox();
    }

    private void updateBoundingBox() {
        boundingBox.min.set(position.x - PLAYER_WIDTH/2, position.y, position.z - PLAYER_WIDTH/2);
        boundingBox.max.set(position.x + PLAYER_WIDTH/2, position.y + PLAYER_HEIGHT, position.z + PLAYER_WIDTH/2);
    }

    private void updateCamera() {
        // Set camera position based on view mode
        if (viewMode == ViewMode.FIRST_PERSON) {
            // Calculate head bob
            float bobY = 0;
            if (onGround && (velocity.x != 0 || velocity.z != 0)) {
                bobY = MathUtils.sin(bobTimer) * bobAmplitude;
            }

            // First-person view at eye level
            camera.position.set(
                    position.x,
                    position.y + PLAYER_EYE_HEIGHT + bobY,
                    position.z);
        } else {
            // Third-person view, position camera behind player
            Vector3 behind = new Vector3(camera.direction).scl(-4);
            camera.position.set(
                    position.x + behind.x,
                    position.y + PLAYER_HEIGHT + 1 + behind.y * 0.5f,
                    position.z + behind.z);
        }

        camera.update();
    }

    private void toggleViewMode() {
        if (viewMode == ViewMode.FIRST_PERSON) {
            viewMode = ViewMode.THIRD_PERSON;
        } else {
            viewMode = ViewMode.FIRST_PERSON;
        }
    }

    private void updateBlockInteraction(World world) {
        // Ray cast from camera
        Ray ray = camera.getPickRay(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);

        // Check for block intersection
        Vector3 intersection = world.raycast(ray, interactionReach);
        if (intersection != null) {
            lookingAt.set(MathUtils.floor(intersection.x), MathUtils.floor(intersection.y), MathUtils.floor(intersection.z));

            // Determine which face of the block was hit
            lookingAtFace = determineFace(ray, lookingAt);

            // Calculate placement position for new block
            placementPosition.set(lookingAt);
            if (lookingAtFace == BlockFace.NORTH) placementPosition.z += 1;
            if (lookingAtFace == BlockFace.SOUTH) placementPosition.z -= 1;
            if (lookingAtFace == BlockFace.EAST) placementPosition.x += 1;
            if (lookingAtFace == BlockFace.WEST) placementPosition.x -= 1;
            if (lookingAtFace == BlockFace.TOP) placementPosition.y += 1;
            if (lookingAtFace == BlockFace.BOTTOM) placementPosition.y -= 1;

            // Handle block breaking
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                BlockType targetBlock = world.getBlockAt((int)lookingAt.x, (int)lookingAt.y, (int)lookingAt.z);

                if (targetBlock != null && targetBlock != BlockType.AIR) {
                    // Get mining speed based on current tool and target block
                    float miningSpeed = currentTool.getMiningSpeedFor(targetBlock);

                    breakingProgress += miningSpeed * Gdx.graphics.getDeltaTime();

                    // Break the block when progress is complete
                    if (breakingProgress >= 1.0f) {
                        world.setBlockAt((int)lookingAt.x, (int)lookingAt.y, (int)lookingAt.z, BlockType.AIR);
                        breakingProgress = 0f;

                        // Add block to inventory
                        inventory.addBlock(targetBlock);

                        // Check for crafting upgrades
                        checkCraftingUpgrades();
                    }
                }
            } else {
                breakingProgress = 0f; // Reset progress if not holding mouse button
            }

            // Handle block placement
            if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                // Check if we have the block in inventory
                if (inventory.getBlockCount(selectedBlockType) > 0) {
                    // Check if placement position doesn't intersect with player
                    BoundingBox placementBox = new BoundingBox(
                            new Vector3(placementPosition.x, placementPosition.y, placementPosition.z),
                            new Vector3(placementPosition.x + 1, placementPosition.y + 1, placementPosition.z + 1));

                    if (!boundingBox.intersects(placementBox)) {
                        world.setBlockAt((int)placementPosition.x, (int)placementPosition.y, (int)placementPosition.z, selectedBlockType);
                        inventory.removeBlock(selectedBlockType);
                    }
                }
            }
        } else {
            lookingAt.set(-1, -1, -1); // No block in sight
            breakingProgress = 0f;
        }
    }

    private BlockFace determineFace(Ray ray, Vector3 blockPos) {
        // Calculate the exact intersection point
        Vector3 direction = new Vector3(ray.direction).nor();
        float t = Float.MAX_VALUE;
        BlockFace face = BlockFace.TOP; // Default

        // Check intersection with each face of the cube
        float tX1 = (blockPos.x - ray.origin.x) / direction.x;
        float tX2 = (blockPos.x + 1 - ray.origin.x) / direction.x;
        float tY1 = (blockPos.y - ray.origin.y) / direction.y;
        float tY2 = (blockPos.y + 1 - ray.origin.y) / direction.y;
        float tZ1 = (blockPos.z - ray.origin.z) / direction.z;
        float tZ2 = (blockPos.z + 1 - ray.origin.z) / direction.z;

        // Find the minimum positive t
        if (tX1 > 0 && tX1 < t) { t = tX1; face = BlockFace.WEST; }
        if (tX2 > 0 && tX2 < t) { t = tX2; face = BlockFace.EAST; }
        if (tY1 > 0 && tY1 < t) { t = tY1; face = BlockFace.BOTTOM; }
        if (tY2 > 0 && tY2 < t) { t = tY2; face = BlockFace.TOP; }
        if (tZ1 > 0 && tZ1 < t) { t = tZ1; face = BlockFace.SOUTH; }
        if (tZ2 > 0 && tZ2 < t) { t = tZ2; face = BlockFace.NORTH; }

        return face;
    }

    private void checkCraftingUpgrades() {
        // Check for crafting conditions and create tools
        // Wood Pickaxe: 5 wood
        if (!inventory.hasTool(Tool.WOOD_PICKAXE) && inventory.getBlockCount(BlockType.WOOD) >= 5) {
            inventory.removeBlock(BlockType.WOOD, 5);
            inventory.addTool(Tool.WOOD_PICKAXE);
            currentTool = Tool.WOOD_PICKAXE;
        }

        // Stone Pickaxe: 5 stone and wood pickaxe
        if (!inventory.hasTool(Tool.STONE_PICKAXE) &&
            inventory.hasTool(Tool.WOOD_PICKAXE) &&
            inventory.getBlockCount(BlockType.STONE) >= 5) {
            inventory.removeBlock(BlockType.STONE, 5);
            inventory.addTool(Tool.STONE_PICKAXE);
            currentTool = Tool.STONE_PICKAXE;
        }

        // Iron Pickaxe: 5 iron and stone pickaxe
        if (!inventory.hasTool(Tool.IRON_PICKAXE) &&
            inventory.hasTool(Tool.STONE_PICKAXE) &&
            inventory.getBlockCount(BlockType.IRON) >= 5) {
            inventory.removeBlock(BlockType.IRON, 5);
            inventory.addTool(Tool.IRON_PICKAXE);
            currentTool = Tool.IRON_PICKAXE;
        }

        // Diamond Pickaxe: 3 diamond and iron pickaxe
        if (!inventory.hasTool(Tool.DIAMOND_PICKAXE) &&
            inventory.hasTool(Tool.IRON_PICKAXE) &&
            inventory.getBlockCount(BlockType.DIAMOND) >= 3) {
            inventory.removeBlock(BlockType.DIAMOND, 3);
            inventory.addTool(Tool.DIAMOND_PICKAXE);
            currentTool = Tool.DIAMOND_PICKAXE;
        }
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Tool getCurrentTool() {
        return currentTool;
    }

    public Vector3 getLookingAt() {
        return lookingAt;
    }

    public BlockType getSelectedBlockType() {
        return selectedBlockType;
    }

    public float getBreakingProgress() {
        return breakingProgress;
    }

    public boolean isSwimming() {
        return isSwimming;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public ViewMode getViewMode() {
        return viewMode;
    }
}
