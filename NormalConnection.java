import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Normal FTP connection thread.
 */
class NormalConnection implements Runnable {
    private static long idCounter = 0;

    private File pwd;
    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private FileLocks fileLocks;
    private TaskTable taskTable;
    private volatile boolean exitThread;

    NormalConnection(Socket clientSocket, File pwd, FileLocks fileLocks, TaskTable taskTable) {
        System.out.println("[Normal Port]: New connection.");

        this.clientSocket = clientSocket;
        this.fileLocks = fileLocks;
        this.taskTable = taskTable;
        this.pwd = pwd;
        exitThread = false;

        try {
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("[ERROR]: Unable to create i/o streams with client on" +
                    "TERMINATE PORT! Closing connection.");
            exitThread = true;

            // Closes input stream if output stream throws exception when created
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException err) { }
            }
        }
    }

    private boolean processCommand() {

        if ()

        return true;
    }

    /**
     * Called by `TaskTable` to terminate a long-running task.
     */
    public void terminateTask() {
        this.exitThread = true;
    }

    /**
     * Creates a unique `long` ID per JVM run.
     * @return a unique ID
     */
    private static synchronized long createID() {
        return idCounter++;
    }

    public void run() {
        if (!exitThread) {
            // Loops until quit command is received
            while (processCommand()) {}

            // Only close streams if no error creating them in the constructor to
            // avoid NullPointerException.
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                System.err.println("[ERROR] Exception encountered while closing TerminateConnection i/o stream.");
            }
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Exception encountered while closing NormalConnection socket.");
        }
    }
}
