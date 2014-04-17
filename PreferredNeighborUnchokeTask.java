import java.util.TimerTask;
import java.util.ArrayList;

public class PreferredNeighborUnchokeTask extends TimerTask {

    public PreferredNeighborUnchokeTask() {
    }
    
    /** Version 2: This method is called by peerProcess according to a given
     * interval. This method will find the k preferred neighbors. For each of 
     * k preferred neighbors, it will run through the list of peers and select
     * the highest data rate peer, and remove it from the list. This method
     * assumes that k < total number of peers. */
     public void run() {
    	
    	ArrayList<PeerHandler> peerList=null;
    	
    	synchronized (peerProcess.peerHandlerList){ 

			// The peerList below is initialized to all peers
			peerList = new ArrayList<PeerHandler>(peerProcess.peerHandlerList);

			if (peerList.isEmpty()) {
				return;
			}

			// The empty preferred list below will contain the preferred
			// neighbors after this method is executed.
			ArrayList<PeerHandler> preferredList = new ArrayList<PeerHandler>();

			for (int i = 0; i < peerProcess.NumberOfPreferredNeighbors; i++) {
				PeerHandler best = null;
				int bestRate = 0;
				// go through each peer to see how it compares to current best
				for (PeerHandler peer : peerList) {
					if (peer != null && peer.otherPeerIsInterested) {
						// if it is better...
						if (peer.getDataRcvd() >= bestRate) {
							best = peer;
							bestRate = peer.getDataRcvd();
						}
					}
				}
				// Now place the best neighbor in the preferred list and remove
				// it from list of peers which are being considered
				if (best != null) {
					preferredList.add(best);
					peerList.remove(best);
				}
			}

			// now clear data counters for all neighbors
			for (int x = 0; x < peerProcess.peerHandlerList.size(); x++)
				peerProcess.peerHandlerList.get(x).clearDataCounter();

			// unchoke preferred neighbors
			for (int i = 0; i < preferredList.size(); i++)
				preferredList.get(i).sendUnchoke();

			// choke un-preferred neighbors, except optimistically-unchoked
			// neighbor
			for (int y = 0; y < peerList.size(); y++) {
				PeerHandler ph = peerList.get(y);
				
				synchronized(ph.otherPeerIsChoked){
					if (ph != peerProcess.currentOptimisticallyUnchokedNeighbor && !ph.otherPeerIsChoked) {
						ph.sendChoke();
					}
				}
			}

			// Log this
			if (preferredList.size() > 0) {
				ArrayList<Integer> neighborIDs = new ArrayList<Integer>();
				String preferredListString = String.valueOf(preferredList.get(0).otherPeerID);
				neighborIDs.add(preferredList.get(0).otherPeerID);
				
				for (int z = 1; z < preferredList.size(); z++) {
					preferredListString = preferredListString + ", "
							+ String.valueOf(preferredList.get(z).otherPeerID);
					neighborIDs.add(preferredList.get(0).otherPeerID);
				}
				Logger.changedPreferredNeighbors(neighborIDs);
			}

    	}
     }
}
