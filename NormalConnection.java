import java.io.*;
import java.net.Socket;
import java.util.Arrays;

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

    NormalConnection(Socket clientSocket, FileLocks fileLocks, TaskTable taskTable) {
        System.out.println("[Normal Port]: New connection.");

        this.clientSocket = clientSocket;
        this.fileLocks = fileLocks;
        this.taskTable = taskTable;
        this.pwd = new File(".");
        exitThread = false;

        try {
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("[ERROR]: Unable to create i/o streams with client on" +
                    "NORMAL PORT! Closing connection.");
            exitThread = true;

            // Closes input stream if output stream throws exception when created
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException err) { }
            }
        }
    }

    private void writeResponse(String response) {
        try {
            outputStream.writeObject(response);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("[ERROR] Unable to send response to client.");
        }
    }

    private boolean processCommand() {
        TaskType command = null;
        String fileName = null;
        try {
            command = (TaskType) inputStream.readObject();

            if (inputStream.available() != 0) {
                fileName = (String) inputStream.readObject();
            }
        } catch (IOException e) {
            // TODO handle non-empty input stream
            return true;
        } catch (ClassNotFoundException e) { }

        if (command == TaskType.QUIT) {
            taskTable.terminateAll();
            return false;
        }

        String response = "";
        switch (command) {
            case LS:
                File[] fileList = pwd.listFiles();
                Arrays.sort(fileList);

                for (File file: fileList) {
                    response += file.getName() + " ";
                }
                break;
            case PWD:
                try {
                    response = pwd.getCanonicalPath();
                } catch (IOException e) {
                    writeResponse("[ERROR] Unable to get present working directory.");
                }
                break;
            case MKDIR:
                File file = new File(pwd, fileName);
                file.mkdir();
                if (file.mkdir()) {
                    response = "";
                } else {
                    response = "[ERROR] Unable to make directory.";
                }
                break;
            case CD:
                if (fileName.equals("..")) {
                    File parent = pwd.getParentFile();

                    // If already in root directory, getParentFile() returns null
                    if (parent != null) {
                        pwd = parent;
                        response = "Changing working directory to " + fileName;
                    } else {
                        response = "Already in root directory.";
                    }
                } else {
                    // Navigate into specified directory
                    // TODO CHECK IF IS DIRECTORY
                    File child = new File(pwd, fileName);

                    if (child.exists()) {
                        pwd = child;

                    } else {

                    }
                }
                break;
            case GET:
                break;
            case PUT:
                break;
            case DELETE:
                break;
            default:
                return false;
        }

        // Write response if command did not quit.
        writeResponse(response);
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

    /**
     * Start connection thread.
     */
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
