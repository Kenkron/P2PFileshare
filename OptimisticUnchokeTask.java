import java.util.Random;
import java.util.TimerTask;
import java.util.ArrayList;

public class OptimisticUnchokeTask extends TimerTask {

	private static Random randomizer = new Random(System.currentTimeMillis());

	/**
	 * This method is called by peerProcess according to a given interval. This
	 * method will find 1 optimistic neighbor randomly from a list of all
	 * unchoked, but interested neighbors
	 */
	public void run() {

		synchronized (peerProcess.peerHandlerList) {

			// The peerList below is initialized to all peers
			ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();

			for (PeerHandler handle : peerProcess.peerHandlerList) {
				peerList.add(peerProcess.getRPI(handle));
			}

			if (peerList.isEmpty()) {
				return;
			}

			// The empty preferred list below will contain the possible
			// neighbors
			// to choose from later
			ArrayList<RemotePeerInfo> possibleList = new ArrayList<RemotePeerInfo>();

			// go through each peer to see how if it can be added to possible
			for (RemotePeerInfo rpi : peerProcess.peerList) {
				PeerHandler peer = peerProcess.rpiToPeerHandler.get(rpi);
				if (peer != null && peer.otherPeerIsInterested
						&& peer.otherPeerIsChoked) {
					possibleList.add(rpi);
				}
			}

			if (possibleList.size() < 1) {
				peerProcess.currentOptimisticallyUnchokedNeighbor = null;
				return;
			}

			int randomIndex = randomizer.nextInt(possibleList.size());
			RemotePeerInfo choice = possibleList.get(randomIndex);

			PeerHandler ph = peerProcess.rpiToPeerHandler.get(choice);
			peerProcess.currentOptimisticallyUnchokedNeighbor = ph;
			ph.sendUnchoke();
		}
	}
}
