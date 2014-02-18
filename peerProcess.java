import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class peerProcess {
	public static int peerID;
	public static ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();
	public static Server server;
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
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Improper format. Use 'java peerProcess peerID'");
			System.exit(0);
		}
		peerID = Integer.valueOf(args[0]);
		
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
		//read and parse the file ./PeerInfo.cfg
		//create a RemotePeerInfo object for each line
		//call setHasValue() if necessary
		//add to peerList
		//sort peerList? ie: RemotePeerInfo implements Comparable, Collections.sort()
		//should self be included in the peerList?
	}

}
