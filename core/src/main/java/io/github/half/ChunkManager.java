package io.github.half;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkManager {
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 64;
    private static final int RENDER_DISTANCE = 8;
    private static final int GENERATION_THREADS = 2;

    private ObjectMap<String, Chunk> loadedChunks;
    private Queue<ChunkOperation> chunkOperationQueue;
    private Array<ChunkOperation> completedOperations;
    private Vector3 lastPlayerChunk;
    private WorldGenerator worldGenerator;
    protected Model[] blockModels;
    private ExecutorService threadPool;
    private AtomicBoolean isRunning;

    public ChunkManager(Model[] blockModels) {
        this.blockModels = blockModels;
        this.loadedChunks = new ObjectMap<>();
        this.chunkOperationQueue = new Queue<>();
        this.completedOperations = new Array<>();
        this.lastPlayerChunk = new Vector3(-1, -1, -1);
        this.worldGenerator = new WorldGenerator();
        this.threadPool = Executors.newFixedThreadPool(GENERATION_THREADS);
        this.isRunning = new AtomicBoolean(true);

        startWorkerThreads();
    }

    private void startWorkerThreads() {
        // Start worker threads for chunk generation
        for (int i = 0; i < GENERATION_THREADS; i++) {
            threadPool.submit(new ChunkWorker());
        }
    }

    // ChunkManager.java - método update simplificado
    public void update(Vector3 playerPosition) {
        int chunkX = (int) Math.floor(playerPosition.x / CHUNK_SIZE);
        int chunkZ = (int) Math.floor(playerPosition.z / CHUNK_SIZE);

        Vector3 currentChunk = new Vector3(chunkX, 0, chunkZ);

        // Process completed operations first
        synchronized (completedOperations) {
            for (ChunkOperation op : completedOperations) {
                if (op.type == ChunkOperation.Type.GENERATE) {
                    loadedChunks.put(getChunkKey(op.chunk.chunkX, op.chunk.chunkZ), op.chunk);
                }
            }
            completedOperations.clear();
        }

        // Only update chunks if player moved
        if (!currentChunk.equals(lastPlayerChunk)) {
            lastPlayerChunk.set(currentChunk);

            // Simple square loading (volta pro que funciona)
            for (int x = chunkX - RENDER_DISTANCE; x <= chunkX + RENDER_DISTANCE; x++) {
                for (int z = chunkZ - RENDER_DISTANCE; z <= chunkZ + RENDER_DISTANCE; z++) {
                    String chunkKey = getChunkKey(x, z);
                    if (!loadedChunks.containsKey(chunkKey) && !isChunkQueued(x, z)) {
                        queueChunkOperation(new ChunkOperation(
                            ChunkOperation.Type.GENERATE,
                            new Chunk(x, z, worldGenerator, blockModels)));
                    }
                }
            }

            // Unload distant chunks
            Array<String> toRemove = new Array<>();
            for (ObjectMap.Entry<String, Chunk> entry : loadedChunks.entries()) {
                Chunk chunk = entry.value;
                float distance = Vector3.dst(chunk.chunkX, 0, chunk.chunkZ, chunkX, 0, chunkZ);
                if (distance > RENDER_DISTANCE + 2) {
                    toRemove.add(entry.key);
                }
            }

            for (String key : toRemove) {
                loadedChunks.remove(key).dispose();
            }
        }
    }
    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }

    private boolean isChunkQueued(int chunkX, int chunkZ) {
        String key = getChunkKey(chunkX, chunkZ);
        synchronized (chunkOperationQueue) {
            for (ChunkOperation op : chunkOperationQueue) {
                if (op.type == ChunkOperation.Type.GENERATE &&
                    getChunkKey(op.chunk.chunkX, op.chunk.chunkZ).equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void queueChunkOperation(ChunkOperation operation) {
        synchronized (chunkOperationQueue) {
            chunkOperationQueue.addLast(operation);
            chunkOperationQueue.notifyAll();
        }
    }

    public ObjectMap<String, Chunk> getLoadedChunks() {
        // Return a safe copy of loaded chunks to prevent concurrent modification
        synchronized (loadedChunks) {
            return loadedChunks;
        }
    }

    public void queueRebuildMesh(int chunkX, int chunkZ) {
        String key = getChunkKey(chunkX, chunkZ);
        if (loadedChunks.containsKey(key)) {
            queueChunkOperation(new ChunkOperation(
                    ChunkOperation.Type.REBUILD_MESH,
                    loadedChunks.get(key)));
        }
    }

    public void dispose() {
        isRunning.set(false);
        threadPool.shutdown();

        // Wake up any waiting threads
        synchronized (chunkOperationQueue) {
            chunkOperationQueue.notifyAll();
        }

        for (Chunk chunk : loadedChunks.values()) {
            chunk.dispose();
        }
        loadedChunks.clear();
    }

    private class ChunkWorker implements Runnable {
        @Override
        public void run() {
            while (isRunning.get()) {
                ChunkOperation operation = null;

                // Get next operation from queue
                synchronized (chunkOperationQueue) {
                    if (chunkOperationQueue.size == 0) {
                        try {
                            chunkOperationQueue.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    if (chunkOperationQueue.size > 0) {
                        operation = chunkOperationQueue.removeFirst();
                    }
                }

                // Process operation
                if (operation != null) {
                    if (operation.type == ChunkOperation.Type.GENERATE) {
                        operation.chunk.generate();
                    } else if (operation.type == ChunkOperation.Type.REBUILD_MESH) {
                        operation.chunk.createMesh();
                    }

                    // Add to completed operations
                    synchronized (completedOperations) {
                        completedOperations.add(operation);
                    }
                }
            }
        }
    }

    public static class ChunkOperation {
        public enum Type {
            GENERATE,
            REBUILD_MESH
        }

        public final Type type;
        public final Chunk chunk;

        public ChunkOperation(Type type, Chunk chunk) {
            this.type = type;
            this.chunk = chunk;
        }
    }
}
