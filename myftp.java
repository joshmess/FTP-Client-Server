import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.math.*;

public class myftp {

	public static void main(String[] args) {

		Scanner in = new Scanner(System.in);

		try {
			if(args.length != 2){
				System.out.println("[ERR] Please include two arguments, machine name and port number");
			}
			String machine = args[0];
			int port = Integer.parseInt(args[1]);
			Socket client_sock = new Socket(machine,port);
			DataOutputStream dout=new DataOutputStream(client_sock.getOutputStream()); 
			DataInputStream din=new DataInputStream(client_sock.getInputStream());  
			String cmd = "";
			String prompt = "myftp>";
			while(!cmd.equals("quit")) {
				System.out.print(prompt + " ");
				cmd = in.nextLine();
				System.out.println();
				if(cmd.equals("ls") || cmd.equals("pwd") || cmd.indexOf("mkdir") == 0 || cmd.indexOf("cd") == 0) {
					dout.writeUTF(cmd);  
					dout.flush();  
					System.out.println(din.readUTF());
				}



				else if(cmd.indexOf("get") != -1) {
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
					}
					else {
						System.out.println("[ERR] File not found.");

					}
				}


				else if(cmd.indexOf("put") != -1) {			//check if the file is in the local directory
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
						System.out.println("File "+filename+" deleted");
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
