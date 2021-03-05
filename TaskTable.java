import java.util.HashMap;

public class TaskTable {
    private volatile HashMap<Long, Boolean> taskTable;

    public TaskTable() {
        this.taskTable = new HashMap<>();
    }

    /**
     * Called by long-running `NormalConnection` threads.
     * @param ID
     */
    public synchronized void addTask(long ID) {
        taskTable.put(ID, true);
    }

    /**
     * Called by long-running `NormalConnection` threads after completion or termination.
     * @param ID
     */
    public synchronized void removeTask(long ID) {
        taskTable.remove(ID);
    }

    /**
     * Sets a long-running task thread to terminate.
     * @param ID
     */
    public synchronized boolean terminateTask(long ID) {
        if (!taskTable.containsKey(ID)) {
            return false;
        }

        taskTable.put(ID, false);
        return true;
    }

    /**
     * Returns status of a long-running task.
     * @param ID
     * @return false if the task has been terminated
     */
    public synchronized boolean isRunning(long ID) {
        return taskTable.get(ID);
    }
}
