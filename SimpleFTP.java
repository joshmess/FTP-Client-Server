import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class SimpleFTP {

	private final String PROMPT = "myftp> ";

	private String machine;
	private Socket nSocket;
	private MessageInputStream inputStream;
	private MessageOutputStream outputStream;
	private Scanner sc;
	private int tPort;

	public SimpleFTP(String machine, int nPort, int tPort) {
		this.tPort = tPort;
		this.machine = machine;
		this.sc = new Scanner(System.in);
		try {
			// Normal client socket
			nSocket = new Socket(machine, nPort);
			inputStream = new MessageInputStream(nSocket.getInputStream());
			outputStream = new MessageOutputStream(nSocket.getOutputStream());
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

	private void writeCommand(TaskType taskType) {
		CommandMessage message = new CommandMessage(taskType);
		try {
			outputStream.writeMessage(message);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
		}
	}

	private void writeCommand(TaskType taskType, String fileName) {
		CommandMessage message = new CommandMessage(taskType, fileName);
		try {
			outputStream.writeMessage(message);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to send " + taskType.name() + " command.");
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

			} else if (cmd.equals("pwd")) {
				writeCommand(TaskType.PWD);

			} else if (cmd.startsWith("mkdir")) {
				writeCommand(TaskType.MKDIR, cmd.substring(cmd.indexOf(" ") + 1));

			} else if (cmd.startsWith("cd")) {
				writeCommand(TaskType.CD, cmd.substring(cmd.indexOf(" ") + 1));

			} else if (cmd.startsWith("get")) {
				if (cmd.endsWith("&")) {

				} else {
					writeCommand(TaskType.GET, cmd.substring(cmd.indexOf(" ") + 1));

				}
			} else if (cmd.startsWith("put")) {
				if (cmd.endsWith("&")) {

				} else {
					writeCommand(TaskType.PUT, cmd.substring(cmd.indexOf(" ") + 1));
				}
			} else if (cmd.startsWith("delete")) {
				writeCommand(TaskType.DELETE, cmd.substring(cmd.indexOf(" ") + 1));

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
