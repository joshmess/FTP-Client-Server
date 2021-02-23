import java.net.Socket;

public class Task {
    public enum TaskType {
        GET,
        PUT,
    }

    private static long idCounter = 0;

    private long ID;
    private TaskType taskType;
    private Socket clientSocket;
    private volatile boolean isTerminated;

    public Task(TaskType taskType, Socket clientSocket) {
        this.ID = createID();
        this.clientSocket = clientSocket;
        this.taskType = taskType;
        isTerminated = false;
    }

    /**
     * Executes the task in the calling thread.
     */
    public void execute() {
        // Periodically checks every 1000 bytes transferred if task is terminated
        long bytesTransferred = 0;

        while (!isTerminated) {

        }
    }

    public long getID() {
        return ID;
    }

    public void terminate() {
        this.isTerminated = true;
    }

    /**
     * Creates a unique long ID per JVM run.
     * @return a unique ID
     */
    private static synchronized long createID() {
        return idCounter++;
    }
}

