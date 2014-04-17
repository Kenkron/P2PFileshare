import java.util.Random;
import java.util.TimerTask;
import java.util.ArrayList;

public class UnresponsiveUnchokeTask extends TimerTask {

	private static Random randomizer = new Random(System.currentTimeMillis());
	private int peerID;
	public UnresponsiveUnchokeTask(int peerID) {
		super();
		this.peerID = peerID;
	}

	/**
	 * This method is called by peerProcess according to a given interval. This
	 * method will find 1 optimistic neighbor randomly from a list of all
	 * unchoked, but interested neighbors
	 */
	public void run() {

		synchronized (peerProcess.peerHandlerList) {

			// The peerList below is initialized to all peers
			ArrayList<PeerHandler> peerList = new ArrayList<PeerHandler>(peerProcess.peerHandlerList);

			if (peerList.isEmpty()) {
				return;
			}

			// The empty preferred list below will contain the possible
			// neighbors to choose from later
			ArrayList<PeerHandler> possibleList = new ArrayList<PeerHandler>();

			// go through each peer to see how if it can be added to possible
			for (PeerHandler peer : peerList) {
				synchronized(peer.otherPeerIsChoked) {
					if (peer != null && peer.otherPeerIsInterested && peer.otherPeerIsChoked) {
						possibleList.add(peer);
					}
				}
			}

			if (possibleList.size() < 1) {
				peerProcess.currentOptimisticallyUnchokedNeighbor = null;
				return;
			}

			int randomIndex = randomizer.nextInt(possibleList.size());
			PeerHandler ph = possibleList.get(randomIndex);

			Logger.debug(Logger.DEBUG_LOGFILE, "Unresponsive peer " + peerID + ", unchoking peer " + ph.otherPeerID);
			ph.sendUnchoke();
		}
	}
}
