import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

public class Task {
    public enum TaskType {
        GET,
        PUT,
    }

    private static long idCounter = 0;

    private TaskType taskType;
    private long ID;
    private volatile boolean isTerminated;

    public Task(TaskType taskType) {
        this.ID = createID();
        isTerminated = false;
        this.taskType = taskType;
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

