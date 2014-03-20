import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Logger {
	private static int peerID;
	private static FileWriter fileWriter;
	private static BufferedWriter writer;
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	/**The Level of Detail to provide while debugging*/
	public static int debugLevel=4;
	
	public static void setupLogger(int newPeerID) {
		peerID = newPeerID;
		try {
			File f = new File("log_peer_" + peerID + ".log");
			if(!f.exists()) {
				f.createNewFile();
			}
			fileWriter = new FileWriter(f.getAbsoluteFile(), false);//Should this be appending?
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void closeLogger() {
		try {
			fileWriter.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void log(String message) {
		System.out.println(message);
	}

	public static void err(String message) {
		System.err.println(message);
	}
	
	public static String getFormattedDate() {
		return dateFormat.format(new Date());
	}
	
	private static void logToFile(String s) {
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(s);
			writer.close();
			debug(3,s);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String connectedToString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " makes a connection to Peer " + otherPeerID + ".\n";
	}
	public static void connectedTo(int otherPeerID) {
		logToFile(connectedToString(otherPeerID));
	}
	
	private static String connectedFromString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " is connected from Peer " + otherPeerID + ".\n";
	}
	public static void connectedFrom(int otherPeerID) {
		logToFile(connectedFromString(otherPeerID));
	}
	
	private static String changedPreferredNeighborsString(ArrayList<Integer> neighborIDs) {
		String s = getFormattedDate() + ": Peer " + peerID + " has the preferred neighbors ";
		for(int i = 0;i<neighborIDs.size();i++) {
			s += neighborIDs.get(i);
			if(i < neighborIDs.size() - 1) {
				s += ", ";
			}
		}
		s += ".\n";
		return s;
	}
	public static void changedPreferredNeighbors(ArrayList<Integer> neighborIDs) {
		logToFile(changedPreferredNeighborsString(neighborIDs));
	}
	
	private static String changedOptimisticallyUnchokedNeighborString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " has optimistically-unchoked neighbor " + otherPeerID + ".\n";
	}
	public static void changedOptimisticallyUnchokedNeighbor(int otherPeerID) {
		logToFile(changedOptimisticallyUnchokedNeighborString(otherPeerID));
	}
	
	private static String chokedByString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " is choked by " + otherPeerID + ".\n";
	}
	public static void chokedBy(int otherPeerID) {
		logToFile(chokedByString(otherPeerID));
	}
	
	private static String unchokedByString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " is unchoked by " + otherPeerID + ".\n";
	}
	public static void unchokedBy(int otherPeerID) {
		logToFile(unchokedByString(otherPeerID));
	}
	
	private static String receivedHaveString(int otherPeerID, int pieceIndex) {
		return getFormattedDate() + ": Peer " + peerID + " received a 'have' message from " + otherPeerID + " for the piece " + pieceIndex + ".\n";
	}
	public static void receivedHave(int otherPeerID, int pieceIndex) {
		logToFile(receivedHaveString(otherPeerID, pieceIndex));
	}
	
	private static String receivedInterestedString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " received an 'interested' message from " + otherPeerID + ".\n";
	}
	public static void receivedInterested(int otherPeerID) {
		logToFile(receivedInterestedString(otherPeerID));
	}
	
	private static String receivedNotInterestedString(int otherPeerID) {
		return getFormattedDate() + ": Peer " + peerID + " received a 'not interested' message from " + otherPeerID + ".\n";
	}
	public static void receivedNotInterested(int otherPeerID) {
		logToFile(receivedNotInterestedString(otherPeerID));
	}
	
	private static String downloadedPieceString(int otherPeerID, int pieceIndex) {
		return getFormattedDate() + ": Peer " + peerID + " has downloaded the piece " + pieceIndex + " from " + otherPeerID + "." +
				"Now the number of pieces it has is " + peerProcess.myCopy.getSegmentsOwnedCount() +  ".\n"; 
	}
	public static void downloadedPiece(int otherPeerID, int pieceIndex) {
		logToFile(downloadedPieceString(otherPeerID, pieceIndex));
	}
	
	private static String downloadCompleteString() {
		return getFormattedDate() + ": Peer " + peerID + " has downloaded the complete file";
	}
	public void downloadComplete() {
		logToFile(downloadCompleteString());
	}
	
	/**Shows a debugging method.  Level indicates the detail provided in this message.
	 * The lower the number, the more important the message is deemed*/
	public static void debug(int level, String s){
		if (level<=debugLevel){
			System.out.println(s);
		}
	}
}