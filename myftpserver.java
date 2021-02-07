import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/*
 * CSCI 4780 Project 1: Simple FTP Server File
 * Authors: Josh Messitte, Alex Holmes, Robert Urquhart
 * This class implements the server side of a simple file transport program.
 * Spring 2021
 */
public class myftpserver {

	public static void main(String[] args) {
		// Check included arguments
		if (args.length == 0){
			System.out.println("[ERR] Include port number argument");
			System.exit(0);
		}

		try {

			// Listen for connections
			int port = Integer.parseInt(args[0]);
			ServerSocket server_sock = new ServerSocket(port);
			System.out.println("Server Listening...");
			Socket client_sock = server_sock.accept();
			System.out.println("Connection Established.");

			DataInputStream dis = new DataInputStream(client_sock.getInputStream());
			DataOutputStream dos = new DataOutputStream(client_sock.getOutputStream());

			// Maintain the current working directory
			File pwd = new File(".");
			String cmd = "";

			// Accept client commands
			while(!cmd.equals("quit")) {
				cmd = (String)dis.readUTF();

				if (cmd.equals("ls")) {
				    /*
					 * List all files in pwd
				     */
					String files = "";
					File[] file_list = pwd.listFiles();
					Arrays.sort(file_list);

					for(File f: file_list) {
						files += f.getName() + " ";
					}

					dos.writeUTF(files);
					dos.flush();
				} else if (cmd.equals("pwd")) {
					/*
					 * Get the name of the current directory where the server resides.
					 */
					dos.writeUTF(pwd.getCanonicalPath());
					dos.flush();
				} else if(cmd.indexOf("mkdir") != -1) {
					/*
					 * Creates a directory in the present working directory.
					 */
					String dirname = cmd.substring(cmd.indexOf(" ") + 1);
					File f = new File(pwd, dirname);
					f.mkdir();

					dos.writeUTF("");
					dos.flush();
				} else if(cmd.indexOf("get")!= -1) {
					/*
					 * Send specified file back to client via DataOutputStream.
					 */
					boolean exists = false;
					String filename = cmd.substring(cmd.indexOf(" ") + 1);
					File file = new File(pwd, filename);

					if (file.exists()) {
						dos.writeUTF("FOUND");

						// Copy file here
						FileInputStream fis = new FileInputStream(file);
						int bytes = 0;
						// Send file size
						dos.writeLong(file.length());
						// Send file in chunks
						byte[] buffer = new byte[4*1024];
						while ((bytes=fis.read(buffer))!=-1){
							dos.write(buffer, 0, bytes);
							dos.flush();
						}
						fis.close();
					} else {
						dos.writeUTF("UNFOUND");
					}

					dos.flush();
				}
				else if (cmd.indexOf("cd") != -1){
					/*
					 * Change the present working directory.
					 */
					String dirname = cmd.substring(cmd.indexOf(" ") + 1);

					if(dirname.equals("..")) {
						// Navigate to parent directory
						File parent = pwd.getParentFile();

						// If already in root directory, will getParentFile() returns null.
						if (parent != null) {
							pwd = parent;
							dos.writeUTF("changing working directory to "+ dirname);
							dos.flush();
						}else{
							dos.writeUTF("Already in root directory.");
							dos.flush();
						}
					} else {
						// TODO No such file message for client
						// Navigate into specified directory
						File child = new File(pwd, dirname);

						if (child.exists()) {
							pwd = child;
							dos.writeUTF("changing working directory to "+ dirname);
							dos.flush();
						}else{
							dos.writeUTF("Directory not found.");
							dos.flush();
						}
						
					}
				} else if (cmd.indexOf("put ") != -1){
					/*
					 * Accept file from client via DataInputStream.
					 */
					String filename = cmd.substring(cmd.indexOf(" ") + 1);
					System.out.println(pwd.getCanonicalPath() + filename);

					FileOutputStream fos = new FileOutputStream(new File(pwd, filename));
					int bytes = 0;
					long size = dis.readLong();   
					byte[] buffer = new byte[4*1024];
					while (size > 0 && (bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
						fos.write(buffer,0,bytes);
						size -= bytes;     
					}

					fos.close();
				} else if(cmd.indexOf("delete")!=-1) {
					/*
					 * Remove specified file from the server's directory.
					 */
					String filename = cmd.substring(cmd.indexOf(" ")+1);
					File file = new File(pwd, filename);

					if (file.exists()) {
						dos.writeUTF("FOUND");
						if(file.delete()) {
							System.out.println("File " + filename + " deleted.");
						} else {
							System.out.println("Failed to delete file " + filename);
						}
					} else {
						dos.writeUTF("UNFOUND");
					}

					dos.flush();
				} else {
					/*
					 * Command not recognized
					 */
					dos.writeUTF("");
					dos.flush();
				}
			}

			server_sock.close();  
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

}