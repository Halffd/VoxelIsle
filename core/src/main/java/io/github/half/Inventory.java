package io.github.half;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;

public class Inventory {
    private ObjectIntMap<BlockType> blocks;
    private ObjectSet<Tool> tools;
    private Array<BlockType> hotbar;

    public Inventory() {
        blocks = new ObjectIntMap<>();
        tools = new ObjectSet<>();
        hotbar = new Array<>(9);

        // Initialize hotbar with placeholder blocks
        hotbar.add(BlockType.DIRT);
        hotbar.add(BlockType.STONE);
        hotbar.add(BlockType.SAND);
        hotbar.add(BlockType.GRASS);
        hotbar.add(BlockType.WOOD);

        // Add some starter blocks
        blocks.put(BlockType.DIRT, 10);
    }

    public void addBlock(BlockType blockType) {
        if (blockType == BlockType.AIR || blockType == BlockType.WATER) {
            return; // Can't collect these blocks
        }
        blocks.put(blockType, blocks.get(blockType, 0) + 1);
    }

    public void removeBlock(BlockType blockType) {
        removeBlock(blockType, 1);
    }

    public void removeBlock(BlockType blockType, int count) {
        int current = blocks.get(blockType, 0);
        if (current >= count) {
            blocks.put(blockType, current - count);
        }
    }

    public int getBlockCount(BlockType blockType) {
        return blocks.get(blockType, 0);
    }

    public BlockType getBlockTypeAt(int index) {
        if (index >= 0 && index < hotbar.size) {
            return hotbar.get(index);
        }
        return BlockType.DIRT; // Default
    }

    public void addTool(Tool tool) {
        tools.add(tool);
    }

    public boolean hasTool(Tool tool) {
        return tools.contains(tool);
    }

    public ObjectIntMap<BlockType> getAllBlocks() {
        return blocks;
    }

    public ObjectSet<Tool> getAllTools() {
        return tools;
    }
}
