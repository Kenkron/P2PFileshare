
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.FileReader;


public class peerProcess {
	public static int peerID;
	public static int myPeerPort;
	public static ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();
	public static ArrayList<PeerHandler> peerHandlerList = new ArrayList<PeerHandler>();
	public static Server server;
	public static Thread serverThread;
	/**the preferred number of active piers given by Common.cfg. Defaults to 2.*/
	public static int NumberOfPreferredNeightbors=2;
	/**the delay (in milliseconds) between preferred neighbor unchoking.
	 * given by Common.cfg.  Defaults to 1.*/
	public static int UnchokingInterval=1;
	/**the delay (in milliseconds) between switching out the optimistically-unchoked neighbor
	 * given by Common.cfg.  Defaults to 1.*/
	public static int OptimisticUnchokingInterval=1;
	/**the name of the file for transmission. given by Common.cfg*/
	public static String FileName=null;
	/**the size of the file for transmission. given by Common.cfg*/
	public static int FileSize=0;
	/**The size of the pieces sent given by Common.cfg.*/
	public static int PieceSize=0;
	
	public static void main(String[] args) throws InterruptedException {
		if(args.length != 1) {
			System.out.println("Improper format. Use 'java peerProcess peerID'");
			System.exit(0);
		}
		peerID = Integer.valueOf(args[0]);
		Logger.setupLogger(peerID);
		
		//read the CommonInfo file
		try{
			readCommonInfo();
		} catch (FileNotFoundException exception){
			Logger.err("Error, Common.cfg could not be found.");
			System.exit(1);
		}
		if (FileName==null||FileSize==0||PieceSize==0){
			Logger.err("Error, Common.cfg must specify FileName, FileSize, and PieceSize");
			System.exit(1);
		}
		
		Logger.debug(1, "Config Files Loaded");
		
		readPeerInfo();
		
		startServerConnectToPeers();

		serverThread.join();

		Logger.closeLogger();
	}
	
	/**read and parse the file ./Common.cfg
	 * @throws FileNotFoundException */
	public static void readCommonInfo() throws FileNotFoundException {
		
		//read the file with a scanner
		File common = new File("./Common.cfg");
		Scanner scanner = new Scanner(common);
		String line;
		
		//for each line
		while ( scanner.hasNext() ){
			
			//get the words on the line
			line = scanner.nextLine();
			String[] segments = line.split(" ");
			
			//if there are two words
			if (segments.length==2){
				
				//try to match them to known name:value combinations
				String name=segments[0];
				String value=segments[1];
				
				if (name.equals("NumberOfPreferredNeighbors")){
					NumberOfPreferredNeightbors=Integer.parseInt(value);
				} else if (name.equals("UnchokingInterval")){
					UnchokingInterval=Integer.parseInt(value);
				} else if (name.equals("OptimisticUnchokingInterval")){
					OptimisticUnchokingInterval=Integer.parseInt(value);
				} else if (name.equals("FileName")){
					FileName=value;
				} else if (name.equals("FileSize")){
					FileSize=Integer.parseInt(value);
				}else if (name.equals("PieceSize")){
					PieceSize=Integer.parseInt(value);
				}
			}
		}
		//always close the scanner
		scanner.close();
	}
	
	//TODO: can we just copy and modify StartRemotePeers.getConfiguration() 
	public static void readPeerInfo() {
		//sort peerList? ie: RemotePeerInfo implements Comparable, Collections.sort()
		//peerList does not contain self
		
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
			    
			    if(Integer.valueOf(newRPI.peerId) == peerID) {
			    	myPeerPort = Integer.valueOf(newRPI.peerPort);
			    }
			    else {
			    	//add to peerList
			    	peerList.add(newRPI);
			    }
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public static void startServerConnectToPeers() {
		server = new Server(peerList, peerHandlerList, myPeerPort);
		serverThread=new Thread(server);
		serverThread.start();
		for(RemotePeerInfo rpi : peerList) {
			try {
				Socket s = new Socket(rpi.peerAddress, Integer.valueOf(rpi.peerPort));
				PeerHandler ph = new PeerHandler(s);
				if(addPeerHandlerToList(ph)) {
					ph.start();
					Logger.connectedTo(Integer.valueOf(rpi.peerId));
				}
			}
			catch(UnknownHostException e) {
				//Host not Found (peer hasn't been started yet), don't print anything
			}
			catch(IOException e) {
				//can't create the socket (peer hasn't been started probably), don't print anything
			}
		}
		Logger.debug(1, "Initial Peers Found: "+peerHandlerList.size());
	}
	
	/**
	 * Add socket <i>s</i> to peerSocketList if it doesn't currently exist.
	 * @return <code>true</code> if added to <code>peerSocketList</code>, otherwise return <code>false</code>
	 */
	public static synchronized boolean addPeerHandlerToList(PeerHandler ph) {
		boolean exists = false;
		for(PeerHandler currentPeer : peerHandlerList) {
			Socket currentSocket = currentPeer.socket;
			if(ph.socket.getInetAddress().toString().equals(currentSocket.getInetAddress().toString())) {
				exists = true;
			}
		}
		if(!exists) {
			peerHandlerList.add(ph);
		}
		return !exists;
	}

}
