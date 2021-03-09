import java.io.*;
import java.net.Socket;
import java.util.*;

public class SimpleFTP {

	private final String PROMPT = "myftp> ";

	private String machine;
	private Socket nSocket;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private Scanner sc;
	private int tPort;
	private FileLocks fileLocks;
	private TaskTable taskTable;

	public SimpleFTP(String machine, int nPort, int tPort) {
		this.tPort = tPort;
		this.machine = machine;
		this.sc = new Scanner(System.in);
		this.fileLocks = new FileLocks();
		this.taskTable = new TaskTable();
		try {
			// Normal client socket
			nSocket = new Socket(machine, nPort);
			inputStream = new ObjectInputStream(nSocket.getInputStream());
			outputStream = new ObjectOutputStream(nSocket.getOutputStream());
		} catch(IOException e) {
			System.out.println("[ERROR] Unable to connect to server. Aborting...");
			System.exit(1);
		}
	}

	private void terminate(long ID) {
		try {
			// Terminate client socket
			Socket tSocket = new Socket(machine, tPort);
			DataInputStream dataInputStream = new DataInputStream(tSocket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(tSocket.getOutputStream());

			// Send command ID to server
			dataOutputStream.writeLong(ID);
			if (dataInputStream.readBoolean()) {
				System.out.println("Terminating task.");
			} else {
				System.out.println("Invalid task ID.");
			}
		} catch(IOException e) {
			System.out.println("[ERROR] Unable to connect to server terminate port. " +
					"Aborting terminate command...");
		}
	}

	private void get(String fileName) {
		// Local filename
		File localFile = new File(fileName.substring(fileName.lastIndexOf('/') + 1));

		// Lock file, reattempts every 2 seconds
		while (!fileLocks.addLock(localFile)) {
			try {
				Thread.currentThread().sleep(2000);
			} catch (InterruptedException e) {
			}
		}
		writeCommand(TaskType.GET, fileName);

		// Check if file exists on server
		try {
			if (!inputStream.readBoolean()) {
				System.out.println("File does not exist on server.");

				fileLocks.removeLock(localFile);
				return;
			}
		} catch (IOException e) {
			return;
		}

		// Delete local version of file if already present
		if (localFile.exists()) {
			localFile.delete();
		}

		// Get command ID
		long ID;
		try {
			ID = inputStream.readLong();
			System.out.println("ID: " + ID);
		} catch (IOException e) {
			return;
		}

		// Retrieve file chunks and write to file
        try {
        	int bytes = 0;
        	FileOutputStream fileOutputStream = new FileOutputStream(localFile);

        	// Read file length
        	long length = inputStream.readLong();

        	byte[] buffer = new byte[1000];
        	while (length > 0 && (bytes = inputStream.read(buffer, 0, (int) Math.min(buffer.length, length))) != -1) {
        	    // Cleanup file if transfer was terminated
				if (Arrays.equals(buffer, "terminate".getBytes())) {
					fileOutputStream.close();
					localFile.delete();
					fileLocks.removeLock(localFile);
					return;
				}

        		fileOutputStream.write(buffer, 0 , bytes);
        		length -= bytes;
			}

        	fileOutputStream.close();
        	readResponse();
		} catch (IOException e) {
        	// TODO
		}

        fileLocks.removeLock(localFile);
	}

	private void put(String fileName) {
		File localFile = new File(fileName);
		if (!localFile.exists()) {
			System.out.println("[ERROR] Local file not found.");
			return;
		}

		// Lock file, reattempts every 2 seconds
		while (!fileLocks.addLock(localFile)) {
			try {
				Thread.currentThread().sleep(2000);
			} catch (InterruptedException e) { }
		}
		writeCommand(TaskType.PUT, fileName);

		// Put command ID
		long ID = 0;
		try {
			ID = inputStream.readLong();
			System.out.println("ID: " + ID);
		} catch (IOException e) {
		    // TODO
		}

		// Send file chunks
		try {
			int bytes = 0;
			FileInputStream fileInputStream = new FileInputStream(localFile);

			// Send file length
			outputStream.writeLong(localFile.length());
			outputStream.flush();

			// Send file chunks
			byte[] buffer = new byte[1000];
			while ((bytes = fileInputStream.read(buffer)) != -1
						&& taskTable.isRunning(ID)) {
				outputStream.write(buffer, 0, bytes);
				outputStream.flush();
			}

			if (!taskTable.isRunning(ID)) {
				buffer = "terminate".getBytes();
				outputStream.write(buffer, 0, buffer.length);
				outputStream.flush();
			}

			fileInputStream.close();
			System.out.println("Sent file: " + fileName);
		} catch (IOException e) {
			// TODO
		}

		fileLocks.removeLock(localFile);
		readResponse();
	}

	private void writeCommand(TaskType taskType) {
		try {
			outputStream.writeObject(taskType);
			outputStream.flush();
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
		}
	}

	private void writeCommand(TaskType taskType, String fileName) {
		try {
			outputStream.writeObject(taskType);
			outputStream.writeObject(fileName);
			outputStream.flush();
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
		}
	}

	private String readResponse() {
		String response = "";
		try {
			response = (String) inputStream.readObject();
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to receive command response.");
		} catch (ClassNotFoundException e) { }

		return response;
	}

	private void run() {
		// Receive command
		System.out.print(PROMPT);
		String cmd = sc.nextLine().trim();

		while(!cmd.equals("quit")) {
			if (cmd.equals("ls")) {
				writeCommand(TaskType.LS);
				System.out.println(readResponse());
			} else if (cmd.equals("pwd")) {
				writeCommand(TaskType.PWD);
				System.out.println(readResponse());
			} else if (cmd.startsWith("mkdir")) {
				writeCommand(TaskType.MKDIR, cmd.substring(cmd.indexOf(" ") + 1));
				System.out.println(readResponse());
			} else if (cmd.startsWith("cd")) {
				if (cmd.equals("cd")) {
					System.out.println("Must enter a directory name.");
					break;
				}
				writeCommand(TaskType.CD, cmd.substring(cmd.indexOf(" ") + 1));
				System.out.println(readResponse());
			} else if (cmd.startsWith("get")) {
				if (cmd.endsWith("&")) {
					final String fileName = cmd.substring(cmd.indexOf(" ") + 1, cmd.length() - 2);
					Runnable task = () -> {
						get(fileName);
					};

					new Thread(task).start();
				} else {
					String fileName = cmd.substring(cmd.indexOf(" ") + 1);
					get(fileName);
				}
			} else if (cmd.startsWith("put")) {
				if (cmd.endsWith("&")) {
					final String fileName = cmd.substring(cmd.indexOf(" ") + 1, cmd.length() - 2);
					Runnable task = () -> {
						put(fileName);
					};

					new Thread(task).start();
				} else {
					String fileName = cmd.substring(cmd.indexOf(" ") + 1);
					put(fileName);
				}
			} else if (cmd.startsWith("delete")) {
				writeCommand(TaskType.DELETE, cmd.substring(cmd.indexOf(" ") + 1));
				System.out.println(readResponse());
			} else if (cmd.startsWith("terminate")) {
				terminate(Long.parseLong(cmd.substring(cmd.indexOf(" ") + 1)));
			} else {
				System.out.println("[ERROR] Command not recognized!");
			}

			// Receive command
			System.out.print(PROMPT);
//			previousCMD=cmd;
			cmd = sc.nextLine().trim();
		}

		// Exit server
		writeCommand(TaskType.QUIT);

		// Cleanup
		try {
			nSocket.close();
			inputStream.close();
			outputStream.close();
		} catch (IOException e) { }
	}

	public static void main(String[] args) {
		if(args.length != 3){
			System.out.println("[ERROR] Please include three arguments, machine name, " +
					"normal port number, and terminate port number");
			return;
		}

		String machine = args[0];
		int nPort = Integer.parseInt(args[1]);
		int tPort = Integer.parseInt(args[2]);

		new SimpleFTP(machine, nPort, tPort).run();
	}
}
