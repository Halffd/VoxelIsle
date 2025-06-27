package io.github.half.wfc;

import io.github.half.BlockType;
import java.util.*;

public class WFCSolver {
    private final Set<Constraint> constraints;
    private final Random random;
    private Map<Position, WFCCell> cells;
    private Queue<Position> propagationQueue;
    private WorldContext context;

    public WFCSolver(Set<Constraint> constraints, long seed) {
        this.constraints = new TreeSet<>((a, b) -> Integer.compare(b.priority(), a.priority()));
        this.constraints.addAll(constraints);
        this.random = new Random(seed);
        this.cells = new HashMap<>();
        this.propagationQueue = new LinkedList<>();
    }

    public boolean solve(WorldContext context, Set<Position> positions) {
        this.context = context;
        initializeCells(positions);

        while (hasUncollapsedCells()) {
            // Find cell with lowest entropy (most constrained)
            WFCCell cellToCollapse = findLowestEntropyCell();
            if (cellToCollapse == null) break;

            // Collapse it to a specific value
            if (!collapseCell(cellToCollapse)) {
                return false; // Contradiction occurred
            }

            // Propagate constraints
            if (!propagateConstraints(cellToCollapse.getPosition())) {
                return false; // Propagation failed
            }
        }

        // Apply results to world context
        applyResults();
        return true;
    }

    private void initializeCells(Set<Position> positions) {
        cells.clear();
        propagationQueue.clear();

        for (Position pos : positions) {
            WFCCell cell = new WFCCell(pos);
            cells.put(pos, cell);

            // Apply initial constraints
            applyInitialConstraints(cell);
        }
    }

    private void applyInitialConstraints(WFCCell cell) {
        for (Constraint constraint : constraints) {
            Set<BlockType> allowed = constraint.getAllowedTypes(cell.getPosition(), context);
            if (!cell.constrain(allowed)) {
                System.err.println("Initial constraint contradiction at " + cell.getPosition());
            }
        }
    }

    private boolean hasUncollapsedCells() {
        return cells.values().stream().anyMatch(cell -> !cell.isCollapsed());
    }

    private WFCCell findLowestEntropyCell() {
        return cells.values().stream()
            .filter(cell -> !cell.isCollapsed())
            .min(Comparator.comparing(WFCCell::getEntropy)
                .thenComparing(cell -> random.nextFloat()))
            .orElse(null);
    }

    private boolean collapseCell(WFCCell cell) {
        Set<BlockType> possible = cell.getPossibleTypes();
        if (possible.isEmpty()) return false;

        // Weighted random selection based on constraints
        BlockType chosen = weightedSelection(possible, cell.getPosition());
        cell.collapse(chosen);

        // Queue neighbors for propagation
        for (Direction dir : Direction.values()) {
            Position neighborPos = cell.getPosition().add(dir);
            if (cells.containsKey(neighborPos)) {
                propagationQueue.offer(neighborPos);
            }
        }

        return true;
    }

    private BlockType weightedSelection(Set<BlockType> possible, Position pos) {
        // Apply biases based on position and context
        Map<BlockType, Float> weights = new HashMap<>();

        for (BlockType type : possible) {
            float weight = 1.0f;

            // Apply contextual weighting
            weight *= getContextualWeight(type, pos);
            weights.put(type, weight);
        }

        // Weighted random selection
        float totalWeight = weights.values().stream().reduce(0f, Float::sum);
        float random = this.random.nextFloat() * totalWeight;

        float currentWeight = 0f;
        for (Map.Entry<BlockType, Float> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (random <= currentWeight) {
                return entry.getKey();
            }
        }

        return possible.iterator().next(); // Fallback
    }

    private float getContextualWeight(BlockType type, Position pos) {
        float weight = 1.0f;

        // Favor certain blocks at certain heights
        switch (type) {
            case WATER:
                weight = pos.y <= 32 ? 2.0f : 0.1f;
                break;
            case GRASS:
                weight = pos.y > 32 && pos.y < 50 ? 1.5f : 0.5f;
                break;
            case STONE:
                weight = pos.y < 40 ? 1.2f : 0.8f;
                break;
            case AIR:
                weight = pos.y > 35 ? 1.1f : 0.3f;
                break;
        }

        return weight;
    }

    private boolean propagateConstraints(Position startPos) {
        propagationQueue.offer(startPos);

        while (!propagationQueue.isEmpty()) {
            Position pos = propagationQueue.poll();
            WFCCell cell = cells.get(pos);
            if (cell == null) continue;

            // Check all neighbors
            for (Direction dir : Direction.values()) {
                Position neighborPos = pos.add(dir);
                WFCCell neighbor = cells.get(neighborPos);
                if (neighbor == null || neighbor.isCollapsed()) continue;

                // Apply constraints between these cells
                if (updateNeighborConstraints(cell, neighbor, dir)) {
                    propagationQueue.offer(neighborPos);
                }

                if (neighbor.getPossibleTypes().isEmpty()) {
                    return false; // Contradiction
                }
            }
        }

        return true;
    }

    private boolean updateNeighborConstraints(WFCCell cell, WFCCell neighbor, Direction direction) {
        Set<BlockType> allowedForNeighbor = new HashSet<>();

        for (BlockType neighborType : neighbor.getPossibleTypes()) {
            boolean canExist = true;

            // Check all constraints
            for (BlockType cellType : cell.getPossibleTypes()) {
                boolean satisfiesConstraints = true;

                for (Constraint constraint : constraints) {
                    if (!constraint.isValid(neighbor.getPosition(), neighborType, context)) {
                        satisfiesConstraints = false;
                        break;
                    }
                }

                if (satisfiesConstraints) {
                    allowedForNeighbor.add(neighborType);
                    break;
                }
            }
        }

        return neighbor.constrain(allowedForNeighbor);
    }

    private void applyResults() {
        for (WFCCell cell : cells.values()) {
            if (cell.isCollapsed()) {
                context.setBlockAt(cell.getPosition(), cell.getCollapsedType());
            }
        }
    }
}
