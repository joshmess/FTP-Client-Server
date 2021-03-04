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
		}

		run();
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

	private boolean get(String fileName) {
		CommandMessage message = new CommandMessage(TaskType.GET, fileName);

	}

	private boolean put(String fileName) {

	}

	private void writeCommand(TaskType taskType) {
		CommandMessage message = new CommandMessage(taskType);
		try {
			outputStream.writeObject(message);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
		}
	}

	private void writeCommand(TaskType taskType, String fileName) {
		CommandMessage message = new CommandMessage(taskType, fileName);
		try {
			outputStream.writeObject(message);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
		}
	}

	private void readResponse() {
		String response = null;
	    try {
			response = (String) inputStream.readObject();
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to receive command response.");
		} catch (ClassNotFoundException e) { }

	    if (response != null) {
			System.out.println(response);
		}
	}

	private void run() {
		// Receive command
		System.out.println(PROMPT);
		String cmd = sc.nextLine().trim();
		System.out.println();

		while(!cmd.equals("quit")) {
			if (cmd.equals("ls")) {
			    writeCommand(TaskType.LS);
			    readResponse();
			} else if (cmd.equals("pwd")) {
				writeCommand(TaskType.PWD);
				readResponse();
			} else if (cmd.startsWith("mkdir")) {
				writeCommand(TaskType.MKDIR, cmd.substring(cmd.indexOf(" ") + 1));
				readResponse();
			} else if (cmd.startsWith("cd")) {
				writeCommand(TaskType.CD, cmd.substring(cmd.indexOf(" ") + 1));
				readResponse();
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
			System.out.println(PROMPT);
			cmd = sc.nextLine().trim();
			System.out.println();
		}
	}

	public static void main(String[] args) {
		if(args.length != 3){
			System.out.println("[ERROR] Please include three arguments, machine name, normal port number, and terminate port number");
		}

		String machine = args[0];
		int nPort = Integer.parseInt(args[1]);
		int tPort = Integer.parseInt(args[2]);

		new SimpleFTP(machine, nPort, tPort);
	}
}
