import java.util.ArrayList;


public class peerProcess {
	public static int peerID;
	public static ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();
	public static Server server;
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Improper format. Use 'java peerProcess peerID'");
			System.exit(0);
		}
		peerID = Integer.valueOf(args[0]);
		
		readCommonInfo();
		readPeerInfo();
		
		server = new Server(peerList);
		new Thread(server).start();
		//attempt to connect to all of the other peers
		//Should we pass the sockets to the Server thread to manage?
		//handle duplicate Sockets: if A connects to B as B is connecting to A, there will be a synchronization problem
		//how do you handle which socket to discard (it should be the same one on each peer)?
		
		//Question: is there user input into this program?
		//ie: Press q to quit (or would you just have to hit the X button)?
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
		//read and parse the file ./PeerInfo.cfg
		//create a RemotePeerInfo object for each line
		//call setHasValue() if necessary
		//add to peerList
		//sort peerList? ie: RemotePeerInfo implements Comparable, Collections.sort()
		//should self be included in the peerList?
	}

}
