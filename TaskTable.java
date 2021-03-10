import java.util.HashMap;

public class TaskTable {
    private HashMap<Long, NormalConnection> taskTable;

    public TaskTable() {
        this.taskTable = new HashMap<>();
    }

    /**
     * Called by long-running `NormalConnection` threads.
     * @param ID
     */
    public synchronized void addTask(long ID, NormalConnection connection) {
        taskTable.put(ID, connection);
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

        // If a get/put task is finished, but has not been removed from this TaskTable,
        // then this effectively does nothing.
        taskTable.get(ID).terminate();
        return true;
    }
}
