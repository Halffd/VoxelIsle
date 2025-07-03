package io.github.half.wfc.constraints;

import io.github.half.BlockType;
import io.github.half.wfc.Constraint;
import io.github.half.wfc.Position;
import io.github.half.wfc.WorldContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class BiomeConstraint extends Constraint {

    private final Map<Biome, Set<BlockType>> biomeBlockTypes;
    private final int priority;

    public BiomeConstraint(int priority) {
        this.priority = priority;
        this.biomeBlockTypes = new HashMap<>();
        setupBiomes();
    }

    private void setupBiomes() {
        // Ocean Biome
        biomeBlockTypes.put(Biome.OCEAN, new HashSet<>(Arrays.asList(
                BlockType.WATER, BlockType.SAND, BlockType.GRAVEL, BlockType.CLAY, BlockType.AIR
        )));

        // Plains Biome
        biomeBlockTypes.put(Biome.PLAINS, new HashSet<>(Arrays.asList(
                BlockType.GRASS, BlockType.DIRT, BlockType.STONE, BlockType.AIR
        )));

        // Forest Biome
        biomeBlockTypes.put(Biome.FOREST, new HashSet<>(Arrays.asList(
                BlockType.GRASS, BlockType.DIRT, BlockType.STONE, BlockType.WOOD, BlockType.LEAVES, BlockType.AIR
        )));

        // Desert Biome
        biomeBlockTypes.put(Biome.DESERT, new HashSet<>(Arrays.asList(
                BlockType.SAND, BlockType.SANDSTONE, BlockType.CACTUS, BlockType.AIR
        )));

        // Mountain Biome
        biomeBlockTypes.put(Biome.MOUNTAIN, new HashSet<>(Arrays.asList(
                BlockType.STONE, BlockType.COAL_ORE, BlockType.IRON_ORE, BlockType.AIR
        )));
    }

    @Override
    public Set<BlockType> getAllowedTypes(Position position, WorldContext context) {
        Biome biome = determineBiome(position, context);
        return biomeBlockTypes.getOrDefault(biome, new HashSet<>(Arrays.asList(BlockType.values())));
    }

    @Override
    public boolean isValid(Position position, BlockType blockType, WorldContext context) {
        Biome biome = determineBiome(position, context);
        Set<BlockType> allowed = biomeBlockTypes.get(biome);
        return allowed == null || allowed.contains(blockType);
    }

    private Biome determineBiome(Position position, WorldContext context) {
        // Simple biome determination based on height and humidity/temperature
        // This can be expanded with Perlin noise for more complex biomes

        float height = context.getHeightAt(position.x, position.z);
        float temperature = context.getTemperatureAt(position);
        float humidity = context.getHumidityAt(position);

        if (position.y <= 32) { // Below sea level
            return Biome.OCEAN;
        } else if (temperature > 70 && humidity < 30) {
            return Biome.DESERT;
        } else if (height > 50) {
            return Biome.MOUNTAIN;
        } else if (humidity > 60) {
            return Biome.FOREST;
        } else {
            return Biome.PLAINS;
        }
    }

    @Override
    public int priority() {
        return priority;
    }

    public enum Biome {
        OCEAN, PLAINS, FOREST, DESERT, MOUNTAIN
    }
}
