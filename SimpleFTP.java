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
	private int nPort;
	private FileLocks fileLocks;
	private TaskTable taskTable;

	public SimpleFTP(String machine, int nPort, int tPort) {
		this.tPort = tPort;
		this.machine = machine;
		this.nPort=nPort;
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
			// Task may be finished on server and removed from server taskTable, but
			// client may still be running. If not running on either, then task ID is
			// invalid.
			dataOutputStream.writeLong(ID);
			if (dataInputStream.readBoolean() || taskTable.terminateTask(ID)) {
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
			// TODO
		}

		// Delete local version of file if already present
		if (localFile.exists()) {
			localFile.delete();
		}

		// Get command ID
		long ID;
		ObjectInputStream inputStream2;
		Socket socket2;
		try {
			socket2=new Socket(machine, nPort+1);
			inputStream2=new ObjectInputStream(socket2.getInputStream());
			ID = inputStream2.readLong();
			System.out.println("ID: " + ID);
			taskTable.addTask(ID);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to receive from server. Aborting...");
			e.printStackTrace();
			return;
		}

		// Retrieve file chunks and write to file
        try {
        	int bytes = 0;
        	FileOutputStream fileOutputStream = new FileOutputStream(localFile);

        	// Read file length
        	long length = inputStream2.readLong();

        	byte[] buffer = new byte[1000];
        	while (length > 0 && taskTable.isRunning(ID)
					&& (bytes = inputStream2.read(buffer, 0, (int) Math.min(buffer.length, length))) != -1) {
        		fileOutputStream.write(buffer, 0 , bytes);
        		length -= bytes;
			}

			// If terminated empty socket stream
            // TODO Not sure if this is enough... may still receive transient file chunks?
			// TODO Should we delete client file if transfer not completed?
			if (!taskTable.isRunning(ID)) {
				inputStream2.skipBytes(inputStream2.available());
			}

			inputStream2.close();
			socket2.close();
        	fileOutputStream.close();
			System.out.println("Retrieved file: " + fileName);
		} catch (IOException e) {
        	// TODO
		}

        fileLocks.removeLock(localFile);
        taskTable.removeTask(ID);
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

		// Get command ID
		long ID=0;
		try {
			ID=inputStream.readLong();
			System.out.println("ID: "+ID);
	}
		catch (IOException e) {

		}
		try {
			Socket socket2=new Socket(machine, nPort+2);
			ObjectInputStream inputStream2=new ObjectInputStream(socket2.getInputStream());
			ObjectOutputStream outputStream2=new ObjectOutputStream(socket2.getOutputStream());

			// Send file chunks
			try {
				int bytes = 0;
				FileInputStream fileInputStream = new FileInputStream(localFile);

				// Send file length
				outputStream2.writeLong(localFile.length());

				// Send file chunks
				byte[] buffer = new byte[1000];
				while ((bytes = fileInputStream.read(buffer)) != -1) {
					outputStream2.write(buffer, 0, bytes);
					outputStream2.flush();
				}

				inputStream2.close();
				outputStream2.close();
				socket2.close();
				fileInputStream.close();
				System.out.println("Sent file: " + fileName);
			} catch (IOException e) {
				// TODO
			}


			fileLocks.removeLock(localFile);
		}
		catch (IOException e) {

		}
	}

	private void writeCommand(TaskType taskType) {
		try {
			outputStream.writeObject(taskType);
			outputStream.flush();
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
			e.printStackTrace();
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
			e.printStackTrace();
		} catch (ClassNotFoundException e) { }

		return response;
	}
	
	private void run() {
		// Receive command
		System.out.print(PROMPT);
		String cmd = sc.nextLine().trim();
		String previousCMD="";
		System.out.println();

		while(!cmd.equals("quit")) {
			if (previousCMD.startsWith("get") || previousCMD.startsWith("put")) {
				readResponse();
			}
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
				writeCommand(TaskType.CD, cmd.substring(cmd.indexOf(" ") + 1));
				System.out.println(readResponse());
			} else if (cmd.startsWith("get")) {
				final String fileName = cmd.substring(cmd.indexOf(" ") + 1);
				if (cmd.endsWith("&")) {
					Runnable task = () -> {
						get(fileName);
					};

					new Thread(task).start();
				} else {
					get(fileName);
				}
			} else if (cmd.startsWith("put")) {
				final String fileName = cmd.substring(cmd.indexOf(" ") + 1);
				if (cmd.endsWith("&")) {
					Runnable task = () -> {
						put(fileName);
					};

					new Thread(task).start();
				} else {
					put(fileName);
				}
			} else if (cmd.startsWith("delete")) {
				writeCommand(TaskType.DELETE, cmd.substring(cmd.indexOf(" ") + 1));
				readResponse();
			} else if (cmd.startsWith("terminate")) {
				terminate(Long.parseLong(cmd.substring(cmd.indexOf(" ") + 1)));
			} else {
				System.out.println("[ERROR] Command not recognized!");
			}

			// Receive command
			System.out.print(PROMPT);
			previousCMD=cmd;
			cmd = sc.nextLine().trim();
			System.out.println();
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
		}

		String machine = args[0];
		int nPort = Integer.parseInt(args[1]);
		int tPort = Integer.parseInt(args[2]);

		new SimpleFTP(machine, nPort, tPort).run();
	}
}
