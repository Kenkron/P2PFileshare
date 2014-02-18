import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class peerProcess {
	public static int peerID;
	public static ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();
	public static ArrayList<Socket> peerSocketList = new ArrayList<Socket>();
	public static Server server;
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Improper format. Use 'java peerProcess peerID'");
			System.exit(0);
		}
		peerID = Integer.valueOf(args[0]);
		
		readCommonInfo();
		readPeerInfo();
		
		server = new Server(peerList, peerSocketList);
		new Thread(server).start();
		for(RemotePeerInfo rpi : peerList) {
			try {
				Socket s = new Socket(rpi.peerAddress, new Integer(rpi.peerPort));
				if(addSocketToList(s)) {
					new PeerHandler(s).start();
				}
			}
			catch(NumberFormatException e) {
				//if the port passed in isn't an integer
				e.printStackTrace();
			}
			catch(UnknownHostException e) {
				//Host not Found (peer hasn't been started yet)
				//e.printStackTrace();
			}
			catch(IOException e) {
				//can't create the socket (peer hasn't been started probably)
				//e.printStackTrace();
			}
		}

		//Implement ChokeHandler here (not as a separate thread)
	}
	
	//TODO
	public static void readCommonInfo() {
		//read and parse the file ./Common.cfg
		//set numberOfPreferredNeighbors
		//set unchokingInterval
		//set optimisticUnchokingInterval
		//set fileName
		//set fileSize (from Common.cfg, not calculated from fileName)
		//set pieceSize
	}
	
	//TODO: can we just copy and modify StartRemotePeers.getConfiguration() 
	public static void readPeerInfo() {
		//sort peerList? ie: RemotePeerInfo implements Comparable, Collections.sort()
		//should self be included in the peerList?
		
		//read and parse the file ./PeerInfo.cfg
		try {
			String st;
			BufferedReader br = new BufferedReader(new FileReader("./PeerInfo.cfg"));
			while ((st = br.readLine()) != null) {
			    //read each column for what it is
				String[] tokens = st.split("\\s+");
			    RemotePeerInfo newRPI = 
			                        new RemotePeerInfo(tokens[0], tokens[1],
			                                           tokens[2]);
			    //call setHasValue() if necessary
			    if (tokens[3].equals("1"))
			        newRPI.setHasFile(true);			    
			    //add to peerList
			    peerList.add(newRPI);
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}
	
	/**
	 * Add socket <i>s</i> to peerSocketList if it doesn't currently exist.
	 * @return true if added to peerSocketList, otherwise return false
	 */
	public static synchronized boolean addSocketToList(Socket s) {
		boolean exists = false;
		for(Socket currentSocket : peerSocketList) {
			if(s.getInetAddress().toString().equals(currentSocket.getInetAddress().toString())) {
				exists = true;
			}
		}
		if(!exists) {
			peerSocketList.add(s);
		}
		return !exists;
	}

}
