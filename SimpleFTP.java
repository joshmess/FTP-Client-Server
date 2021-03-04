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

	public SimpleFTP(String machine, int nPort, int tPort) {
		this.tPort = tPort;
		this.machine = machine;
		this.sc = new Scanner(System.in);
		this.fileLocks = new FileLocks();
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

	private boolean terminate(long ID) {
	    try {
			// Terminate client socket
			Socket tSocket = new Socket(machine, tPort);
			DataInputStream dataInputStream = new DataInputStream(tSocket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(tSocket.getOutputStream());

			// Send command ID to server
			// Receive true if command ID was valid
			dataOutputStream.writeLong(ID);
			return dataInputStream.readBoolean();
		} catch(IOException e) {
			System.out.println("[ERROR] Unable to connect to server terminate port. Aborting terminate command...");
			return false;
		}
	}

	private void get(String fileName) {
		writeCommand(TaskType.GET, fileName);

	}

	private void getThread(String fileName) {

	}

	private void put(String fileName) {
		writeCommand(TaskType.PUT, fileName);

	}

	private void putThread (String fileName) {

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
		System.out.println(PROMPT);
		String cmd = sc.nextLine().trim();
		System.out.println();

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
				writeCommand(TaskType.CD, cmd.substring(cmd.indexOf(" ") + 1));
				System.out.println(readResponse());
			} else if (cmd.startsWith("get")) {
				final String fileName = cmd.substring(cmd.indexOf(" ") + 1);
				if (cmd.endsWith("&")) {
					Runnable task = () -> {
						getThread(fileName);
					};

					new Thread(task).start();
				} else {
				    get(fileName);
				}
			} else if (cmd.startsWith("put")) {
				final String fileName = cmd.substring(cmd.indexOf(" ") + 1);
				if (cmd.endsWith("&")) {
					Runnable task = () -> {
						putThread(fileName);
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
			System.out.println(PROMPT);
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
			System.out.println("[ERROR] Please include three arguments, machine name, normal port number, and terminate port number");
		}

		String machine = args[0];
		int nPort = Integer.parseInt(args[1]);
		int tPort = Integer.parseInt(args[2]);

		new SimpleFTP(machine, nPort, tPort).run();
	}
}
