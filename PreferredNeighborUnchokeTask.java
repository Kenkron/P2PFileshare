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
        //The peerList below is initialized to all peers, but after this method
        //runs, it will contain "un-preferred" neighbors
        ArrayList<RemotePeerInfo> peerList = new ArrayList<RemotePeerInfo>();
        
        for (PeerHandler handle: peerProcess.peerHandlerList){
        	peerList.add(peerProcess.getRPI(handle));
        }
        
        if (peerList.isEmpty()){
        	return;
        }
        
        //The empty preferred list below will contain the preferred neighbors 
        //after this method is executed. 
        ArrayList<RemotePeerInfo> preferredList = new ArrayList<RemotePeerInfo>();
        
        //first check if we have less peers than # of allowable preferred neighbors
        //if (peerList.size() <= peerProcess.NumberOfPreferredNeighbors) {
            //if so, just make everyone a preferred neighbor
        //	preferredList = new ArrayList<RemotePeerInfo>(peerList);
            //now clear the list of "un-preferred" neighbors
        //    peerList.clear();
        //}
        //otherwise, check for k best
        //else {
            //for each of k preferred neighbors, find highest in list so far.
            for (int i = 0; i < peerProcess.NumberOfPreferredNeighbors; i++) {
                RemotePeerInfo best = null;
                int bestRate = 0;
                //go through each peer to see how it compares to current best
                for (RemotePeerInfo rpi : peerProcess.peerList) {
                    PeerHandler peer = peerProcess.rpiToPeerHandler.get(rpi);
                    if (peer != null && peer.otherPeerIsInterested) {
                        //if it is better...
                        if (peer.getDataRcvd() >= bestRate) {
                            best = rpi;
                            bestRate = peer.getDataRcvd();
                        }
                    }
                }
                //Now place the best neighbor in the preferred list and remove 
                //it from list of peers which are being considered
                if(best != null) {
                	preferredList.add(best);
                	peerList.remove(best);
                }
            }
        //}
        
        //now clear data counters for all neighbors
        for (int x = 0; x < peerProcess.peerList.size(); x++)
            peerProcess.rpiToPeerHandler.get(peerProcess.peerList.get(x)).clearDataCounter();
        
        //unchoke preferred neighbors
        for (int i = 0; i < preferredList.size(); i++)
            peerProcess.rpiToPeerHandler.get(preferredList.get(i)).sendUnchoke();
        
        //choke un-preferred neighbors, except optimistically-unchoked neighbor
        for (int y = 0; y < peerList.size(); y++) {
        	PeerHandler ph = peerProcess.rpiToPeerHandler.get(peerList.get(y));
        	if(ph != peerProcess.currentOptimisticallyUnchokedNeighbor) {
        		ph.sendChoke();
        	}
        }
            
        //Log this
        if(preferredList.size() > 0) {
        	String preferredListString = preferredList.get(0).peerId;
        	for (int z = 1; z < preferredList.size(); z++)
        		preferredListString = preferredListString + ", " + 
        				preferredList.get(z).peerId;
        	Logger.debug(Logger.DEBUG_STANDARD, "Peer " + peerProcess.peerID + " has the preferred " +
        			"neighbors " + preferredListString);
        }
     }
}