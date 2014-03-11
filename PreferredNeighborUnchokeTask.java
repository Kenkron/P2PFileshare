import java.util.TimerTask;

public class PreferredNeighborUnchokeTask extends TimerTask {

    public PreferredNeighborUnchokeTask() {
    }

	/** This method is called by peerProcess according to a given 
	 * interval. This method will find the k preferred neighbors. */
    public void run() {
		// Find the k fastest, interested neighbors
		int[] dataRates = new int[peerProcess.NumberOfPreferredNeighbors];
		RemotePeerInfo[] preferredNeighbors = new RemotePeerInfo[peerProcess.NumberOfPreferredNeighbors];
        for (RemotePeerInfo rpi : peerProcess.peerList) {
			//Get the corresponding peerHandler for this RPI
			PeerHandler peer = peerProcess.rpiToPeerHandler.get(rpi); 
			if (peer.otherPeerIsInterested) {
				//Check if this peer has better (or equal?) rates than 
				//current preferred neighbors
				for (int i = 0; i < peerProcess.NumberOfPreferredNeighbors; i++) {
					if (peer.getDataRcvd() >= dataRates[i]) {
						dataRates[i] = peer.getDataRcvd();
						preferredNeighbors[i] = rpi;
					}
				}
			}
			peer.clearDataCounter();
		}
		
		//TODO: Send choke/unchoke messages (make sure to log these? or does the peerHandler do that?)
    }
}
