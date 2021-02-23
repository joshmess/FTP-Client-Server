import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class SimpleFTP {

	public static void main(String[] args) {
		if(args.length != 3){
			System.err.println("[ERR] Please include three arguments, machine name, normal port number, and terminate port number");
		}

		String machine = args[0];
		int nPort = Integer.parseInt(args[1]);
		int tPort = Integer.parseInt(args[2]);
		Scanner in = new Scanner(System.in);

		try {
			// Normal client socket
			Socket nSocket = new Socket(machine, nPort);
			DataOutputStream nDataOut = new DataOutputStream(nSocket.getOutputStream());
			DataInputStream nDataIn  = new DataInputStream(nSocket.getInputStream());

			// Terminate client socket
            Socket tSocket = new Socket(machine, tPort);
			DataInputStream tDataIn = new DataInputStream(tSocket.getInputStream());

			String cmd = "";
			final String prompt = "myftp>";

			while(!cmd.equals("quit")) {
				System.out.print(prompt + " ");
				cmd = in.nextLine();
				System.out.println();
				if(cmd.equals("ls") || cmd.equals("pwd") || cmd.indexOf("mkdir") == 0 || cmd.indexOf("cd") == 0) {
					nDataOut.writeUTF(cmd);
					nDataOut.flush();
					System.out.println(nDataIn.readUTF());
				} else if(cmd.indexOf("get") != -1) {
					dout.writeUTF(cmd);
					dout.flush();
					String response = din.readUTF();
					if(response.equals("FOUND")) {
						//create file
						String newf = cmd.substring(cmd.indexOf(" ")+1);
						int bytes = 0;
						FileOutputStream fos = new FileOutputStream(newf);

						long size = din.readLong();
						byte[] buffer = new byte[4*1024];
						while (size > 0 && (bytes = din.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
							fos.write(buffer,0,bytes);
							size -= bytes;
						}
						fos.close();
						System.out.println("File "+newf+" downloaded from remote directory.");
					}
					else {
						System.out.println("[ERR] File not found.");

					}
				}


				else if(cmd.indexOf("put ") != -1) {			//check if the file is in the local directory
					String filename=cmd.substring(cmd.indexOf(" ")+1);
					File dir=new File(".");
					File[] file_list=dir.listFiles();
					boolean exists=false;
					for(File f: file_list) {
						if(filename.equals(f.getName())) {

							exists=true;
							dout.writeUTF(cmd);
							dout.flush();
							int bytes=0;
							File file = new File(filename);
							FileInputStream fis = new FileInputStream(file);
							//send file size
							dout.writeLong(file.length());
							//send file in chunks
							byte[] buffer = new byte[4*1024];
							while ((bytes=fis.read(buffer))!=-1){
								dout.write(buffer,0,bytes);
								dout.flush();
							}
							fis.close();
							System.out.println("File "+filename+" added to remote directory.");
						}
					}
					if(!exists) {
						System.out.println("[ERR] File not found in local directory.");
					}

				}


				//delete works fine now
				else if(cmd.indexOf("delete")!=-1) { //need to check if the file is found

					dout.writeUTF(cmd);
					dout.flush();
					String response=din.readUTF();
					String filename=cmd.substring(cmd.indexOf(" ")+1);
					if(response.equals("FOUND")){	//file was found in remote directory
						System.out.println("File "+filename+" deleted from remote directory.");
					}
					else {							//file not found in the remote directory
						System.out.println("[ERR] File not found.");
					}

				}
				else if(cmd.equals("quit")==false) {
					System.out.println("[ERR] Unrecognized command!");
				}
			}
			dout.close();
			client_sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
