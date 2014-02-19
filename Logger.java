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

	public static void setupLogger(int newPeerID) {
		peerID = newPeerID;
		try {
			File f = new File("log_peer_" + peerID + ".log");
			if(!f.exists()) {
				f.createNewFile();
			}
			fileWriter = new FileWriter(f.getAbsoluteFile(), true);//Should this be appending?
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

	public static void connectedTo(int otherPeerID) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " makes a connection to Peer " + otherPeerID + ".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void connectedFrom(int otherPeerID) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " is connected from Peer " + otherPeerID + ".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void changedPreferredNeighbors(ArrayList<Integer> neighborIDs) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " has the preferred neighbors ");
			for(int i = 0;i<neighborIDs.size();i++) {
				writer.write(neighborIDs.get(i));
				if(i < neighborIDs.size() - 1) {
					writer.write(", ");
				}
			}
			writer.write(".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void changedOptimisticallyUnchokedNeighbor(int otherPeerID) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " has optimistically-unchoked neighbor " + otherPeerID + ".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void chokedBy(int otherPeerID) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " is choked by " + otherPeerID + ".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void unchokedBy(int otherPeerID) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " is unchoked by " + otherPeerID + ".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void receivedHave(int otherPeerID, int pieceIndex) {
		Date now = new Date();
		try {
			writer = new BufferedWriter(fileWriter);
			writer.write(dateFormat.format(now) + ": Peer " + peerID + " received a 'have' message from " + otherPeerID + " for the piece " + pieceIndex + ".\n");
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

}