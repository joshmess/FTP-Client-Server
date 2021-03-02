import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class ThreadPool {
    private Integer numThreads;
    private ArrayList<Worker> pool;
    private volatile Queue<Task> tasks;
    private volatile HashMap<Long, Task> terminateMap;
    // TODO Need to fix locks on hashmap and queue. Must share a lock for adding and removing objects from them.

    ThreadPool(Integer numThreads) {
        if (numThreads == null) {
            // ThreadPool has minimum of 2 threads
            this.numThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2);
        } else {
            this.numThreads = numThreads;
        }
        pool = new ArrayList<>();
        tasks = new ArrayDeque<>();
        terminateMap = new HashMap<>();

        // Populate worker pool
        for (int i = 0; i < numThreads; i++) {
            pool.add(new Worker(i, this));
        }
    }

    /**
     * Adds a task to the task queue.
     * @param task a blocking task
     * @return the ID of the added task
     */
    public long addTask(Task task) {
        long ID = task.getID();
        synchronized (this) {
            tasks.add(task);
            terminateMap.put(ID, task);
        }
        return ID;
    }

    /**
     * Returns a blocking task.
     * @return a blocking task or null if queue is empty.
     */
    public Task getTask() {
        synchronized (this) {
            return tasks.poll();
        }
    }

    /**
     * Sets a task to terminate and returns true if a valid task ID was given.
     * @param ID unique task identifier
     * @return true if task ID was valid
     */
    public boolean terminateTask(long ID) {
        synchronized (this) {
            Task task = terminateMap.get(ID);
            if (task != null) {
                task.terminate();
                return true;
            }
            return false;
        }
    }

    /**
     * Remove a task from the terminateMap once task is completed by worker thread. Terminated tasks will continue to
     * run cleanup in the worker thread, and then is removed from the terminateMap.
     * @param task a blocking task
     */
    public void removeTask(Task task) {
        synchronized (this) {
            terminateMap.remove(task.getID());
        }
    }
}
