import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class ThreadPool implements Runnable {
    private Integer numThreads;
    private ArrayList<Worker> pool;
    private volatile Queue<Task> tasks;
    private volatile HashMap<Long, Task> terminateMap;
    private Thread callingThread;

    ThreadPool(Integer numThreads, Thread callingThread) {
        if (numThreads == null) {
            // ThreadPool has minimum of 2 threads
            this.numThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2);
        } else {
            this.numThreads = numThreads;
        }
        pool = new ArrayList<>();
        tasks = new ArrayDeque<>();
        terminateMap = new HashMap<>();
        this.callingThread = callingThread;
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

    @Override
    public void run() {
        for (int i = 0; i < numThreads; i++) {
            pool.add(new Worker(i, this));
        }
        // Calling thread waits until ThreadPool is initialized before accepting connections
        callingThread.notify();
    }
}
