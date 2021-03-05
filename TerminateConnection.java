import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A port connection for client to terminate a running task in the `ThreadPool`. The
 * client should immediately send a `long` task ID. The server will respond with a
 * `boolean` `true` if the task ID was valid (i.e. the task has not yet completed)
 * This does not guarantee that the process was terminated before its completion.
 */
public class TerminateConnection implements Runnable {
    private Socket clientSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private TaskTable taskTable;
    private boolean exitThread;

    TerminateConnection(Socket clientSocket, TaskTable taskTable) {
        System.out.println("[Terminate Port]: New connection.");

        this.clientSocket = clientSocket;
        this.taskTable = taskTable;
        exitThread = false;

        try {
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("[ERROR]: Unable to create i/o streams with client on" +
                    "TERMINATE PORT! Closing connection.");
            exitThread = true;

            // Closes input stream if output stream throws exception when created
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException err) { }
            }
        }
    }

    /**
     * Reads a `long` task ID from client and sends a `boolean` `true` if task ID was
     * valid.
     */
    private void terminateCommand() {
        try {
            long commandID = dataInputStream.readLong();

            // Terminate task and send success to client.
            dataOutputStream.writeBoolean(taskTable.terminateTask(commandID));
            dataOutputStream.flush();
        } catch (IOException e) {
            System.err.println("[ERROR] Unable to read stream from terminate port connection. Aborting connection...");
            return;
        }
    }

    @Override
    public void run() {
        if (!exitThread) {
            terminateCommand();

            // Only close data streams if no error creating them in the constructor to
            // avoid NullPointerException.
            try {
                dataInputStream.close();
                dataOutputStream.close();
            } catch (IOException e) {
                System.err.println("[ERROR] Exception encountered while closing TerminateConnection i/o stream.");
            }
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Exception encountered while closing TerminateConnection socket.");
        }
    }
}
