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
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
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

            // Some commands send a file name String
            if (command == TaskType.GET
                    || command == TaskType.PUT
                    || command == TaskType.DELETE
                    || command == TaskType.MKDIR
                    || command == TaskType.CD) {
                fileName = (String) inputStream.readObject();
            }
        } catch (IOException e) {
            try {
                // If filename is still in stream, empties entire stream
                // for future command input being read.
                inputStream.skipBytes(inputStream.available());
            } catch (IOException f) { }
            return true;
        } catch (ClassNotFoundException e) { }

        if (command == TaskType.QUIT) {
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
                    response = "[ERROR] Unable to get present working directory.";
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
                    File child = new File(pwd, fileName);
                    if (!child.isDirectory()) {
                        writeResponse(fileName + " is not a directory!");
                        return true;
                    }

                    if (child.exists()) {
                        pwd = child;
                        response = "Changing working directory to " + fileName;
                    } else {
                        response = "Directory not found.";
                    }
                }
                break;
            case GET:
                final File getFile = new File(pwd, fileName);

                // Let client know if file exits
                try {
                    if (getFile.exists()) {
                        outputStream.writeBoolean(true);
                    } else {
                        outputStream.writeBoolean(false);
                        break;
                    }
                } catch (IOException e) {
                    // TODO
                }

                final long getID = createID();
                taskTable.addTask(getID);

                Runnable getTask = () -> {
                    try {
                        outputStream.writeLong(getID);

                        // Lock file, reattempts every 2 seconds
                        while (!fileLocks.addLock(getFile)) {
                            try {
                                Thread.currentThread().sleep(2000);
                            } catch (InterruptedException e) { }
                        }

                        int bytes = 0;
                        FileInputStream fileInputStream = new FileInputStream(getFile);

                        // Send file length
                        outputStream.writeLong(getFile.length());

                        // Send file chunks
                        byte[] buffer = new byte[1000];
                        while ((bytes = fileInputStream.read(buffer)) != 1 && taskTable.isRunning(getID)) {
                            outputStream.write(buffer, 0, bytes);
                            outputStream.flush();
                        }

                        // Cleanup
                        fileInputStream.close();
                        fileLocks.removeLock(getFile);
                        taskTable.removeTask(getID);
                    } catch (IOException e) {
                        // TODO
                    }

                };

                new Thread(getTask).start();
                break;
            case PUT:
                final File putFile = new File(pwd, fileName);
                final long putID = createID();
                taskTable.addTask(putID);

                // Delete local version of file if already present
                if (putFile.exists()) {
                    putFile.delete();
                }

                Runnable putTask = () -> {
                    try {
                        outputStream.writeLong(putID);

                        // Lock file, reattempts every 2 seconds
                        while (!fileLocks.addLock(putFile)) {
                            try {
                                Thread.currentThread().sleep(2000);
                            } catch (InterruptedException e) { }
                        }

                        int bytes = 0;
                        FileOutputStream fileOutputStream = new FileOutputStream(putFile);

                        // Read file length
                        long length = inputStream.readLong();

                        byte[] buffer = new byte[1000];
                        while (length > 0 && taskTable.isRunning(putID)
                                && (bytes = inputStream.read(buffer, 0, (int) Math.min(buffer.length, length))) != 1) {
                            fileOutputStream.write(buffer, 0, bytes);
                            length -= bytes;
                        }

                        if (!taskTable.isRunning(putID)) {
                            inputStream.skipBytes(inputStream.available());
                            putFile.delete();
                        }

                        // Cleanup
                        fileOutputStream.close();
                        fileLocks.removeLock(putFile);
                        taskTable.removeTask(putID);
                    } catch (IOException f) {
                        // TODO
                    }
                };

                new Thread(putTask).start();
                break;
            case DELETE:

                break;
        }

        // Write response if command did not quit.
        writeResponse(response);
        return true;
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
            System.out.println("[NORMAL PORT]: Connection closed.");
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Exception encountered while closing NormalConnection socket.");
        }
    }
}
