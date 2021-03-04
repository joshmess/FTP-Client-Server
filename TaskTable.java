import java.util.HashMap;

public class TaskTable {
    private volatile HashMap<Long, NormalConnection> taskTable;

    public TaskTable() {
        this.taskTable = new HashMap<>();
    }

    /**
     * Called by long-running `NormalConnection` threads.
     * @param ID
     * @param connection
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
     * @return
     */
    public synchronized boolean terminateTask(long ID) {
        NormalConnection connection = taskTable.get(ID);
        if (connection == null) {
            return false;
        }
        connection.terminateTask();
        return true;
    }
}
