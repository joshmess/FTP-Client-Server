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
    private volatile boolean isCmdRunning;

    NormalConnection(Socket clientSocket, FileLocks fileLocks, TaskTable taskTable) {
        System.out.println("[Normal Port]: New connection.");

        this.clientSocket = clientSocket;
        this.fileLocks = fileLocks;
        this.taskTable = taskTable;
        this.pwd = new File(".");
        exitThread = false;
        isCmdRunning = true;

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
        isCmdRunning = true;
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

        // Response is written to outputStream after command is processed.
        String response = "";
        if (command == TaskType.LS) {
            File[] fileList = pwd.listFiles();
            Arrays.sort(fileList);

            for (File file: fileList) {
                response += file.getName() + " ";
            }
        } else if (command == TaskType.PWD) {
            try {
                response = pwd.getCanonicalPath();
            } catch (IOException e) {
                response = "[ERROR] Server unable to get present working directory.";
            }
        } else if (command == TaskType.MKDIR) {
            File file = new File(pwd, fileName);
            if (file.mkdir()) {
                response = "Directory created.";
            } else {
                response = "[ERROR] Server unable to make directory.";
            }
        } else if (command == TaskType.CD) {
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

                if (child.exists()) {
                    if (child.isDirectory()) {
                        pwd = child;
                        response = "Changing working directory to " + fileName;
                    } else {
                        response = fileName + " is not a directory!";
                    }
                } else {
                    response = "Directory not found.";
                }
            }
        } else if (command == TaskType.GET) {
            File getFile = new File(pwd, fileName);

            // Let client know if file exits
            try {
                if (getFile.exists()) {
                    outputStream.writeBoolean(true);
                    outputStream.flush();
                } else {
                    // Client will end transfer as well.
                    outputStream.writeBoolean(false);
                    outputStream.flush();
                    return true;
                }
            } catch (IOException e) {
                System.out.println("[ERROR] Unable to send response to client. Closing connection...");
                return false;
            }

            // Lock file, reattempts every second
            while (!fileLocks.addLock(getFile)) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) { }
            }

            long getID = createID();
            taskTable.addTask(getID, this);

            try {
                outputStream.writeLong(getID);
                outputStream.flush();

                int bytes = 0;
                FileInputStream fileInputStream = new FileInputStream(getFile);

                // Send file length
                outputStream.writeLong(getFile.length());
                outputStream.flush();

                // Send file chunks
                byte[] buffer = new byte[1000];
                while ((bytes = fileInputStream.read(buffer)) != -1) {
                    if (isCmdRunning) {
                        outputStream.writeBoolean(true);
                        outputStream.write(buffer, 0, bytes);
                        outputStream.flush();
                    } else {
                        // If terminated, let client know all transient packets delivered.
                        outputStream.writeBoolean(false);
                        outputStream.flush();
                        break;
                    }
                }

                // Cleanup
                fileInputStream.close();
                fileLocks.removeLock(getFile);
                taskTable.removeTask(getID);
            } catch (IOException e) {
                fileLocks.removeLock(getFile);
                taskTable.removeTask(getID);
            }
        } else if (command == TaskType.PUT) {
            File putFile = new File(pwd, fileName);
            long putID = createID();
            taskTable.addTask(putID, this);

            // Delete local version of file if already present
            if (putFile.exists()) {
                putFile.delete();
            }

            try {
                outputStream.writeLong(putID);
                outputStream.flush();

                // Lock file, reattempts every 2 seconds
                while (!fileLocks.addLock(putFile)) {
                    try {
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }

                int bytes = 0;
                FileOutputStream fileOutputStream = new FileOutputStream(putFile);

                // Read file length
                long length = inputStream.readLong();

                byte[] buffer = new byte[1000];
                while (length > 0 && isCmdRunning
                        && (bytes = inputStream.read(buffer, 0, (int) Math.min(buffer.length, length))) != -1) {
                    fileOutputStream.write(buffer, 0, bytes);
                    length -= bytes;
                }

                // Cleanup
                fileOutputStream.close();
                if (!isCmdRunning) {
                    putFile.delete();
                }
                fileLocks.removeLock(putFile);
                taskTable.removeTask(putID);
            } catch (IOException f) {
                fileLocks.removeLock(putFile);
                taskTable.removeTask(putID);
            }
        } else if (command == TaskType.DELETE) {
            File deleteFile = new File(pwd, fileName);

            if (deleteFile.exists()) {
                if (deleteFile.isDirectory()) {
                    if (deleteFile.list().length != 0) {
                        response = "Failed to delete directory, not empty.";
                    } else if (deleteFile.delete()) {
                        response = "Directory " + fileName + " deleted.";
                    } else {
                        response = "Failed to delete directory " + fileName;
                    }
                } else {
                    // Lock file, reattempts every second
                    while (!fileLocks.addLock(deleteFile)) {
                        try {
                            Thread.currentThread().sleep(1000);
                        } catch (InterruptedException e) { }
                    }

                    if (deleteFile.delete()) {

                        response = "File " + fileName + " deleted.";
                    } else {
                        response = "Failed to delete file " + fileName;
                    }
                    fileLocks.removeLock(deleteFile);
                }
            } else {
                response = "File not found.";
            }
        }

        // Write response if command did not quit.
        if (command != TaskType.GET || command != TaskType.PUT) {
            writeResponse(response);
        }
        return true;
    }

    /**
     * Sets terminate flag for this connections currently running get/put task.
     */
    public void terminate() {
        this.isCmdRunning = false;
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
            return;
        }

        System.out.println("[Normal Port]: Connection closed.");
    }
}
