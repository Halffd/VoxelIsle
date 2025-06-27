package io.github.half.wfc;

import io.github.half.BlockType;

public interface WorldContext {
    BlockType getBlockAt(Position pos);
    void setBlockAt(Position pos, BlockType blockType);
    boolean isGenerated(Position pos);
    float getHeightAt(int x, int z);
    float getTemperatureAt(Position pos);
    float getHumidityAt(Position pos);
}
