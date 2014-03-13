import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Collections;

public class PreferredNeighborUnchokeTask extends TimerTask {

    public PreferredNeighborUnchokeTask() {
    }
    
    /** Version 2: This method is called by peerProcess according to a given
     * interval. This method will find the k preferred neighbors. For each of 
     * k preferred neighbors, it will run through the list of peers and select
     * the highest data rate peer, and remove it from the list. This method
     * assumes that k < total number of peers. */
     public void run() {
        ArrayList<RemotePeerInfo> peerList = null;
        Collections.copy(peerList, peerProcess.peerList);
        //The empty preferred list below will contain the preferred neighbors 
        //after this method is executed. 
        ArrayList<RemotePeerInfo> preferredList = new ArrayList<RemotePeerInfo>();
        
        //first check if we have less peers than # of allowable preferred neighbors
        if (peerList.size() <= peerProcess.NumberOfPreferredNeighbors) {
            //if so, just make everyone a preferred neighbor
            Collections.copy(preferredList, peerList);
        }
        //otherwise, check for k best
        else {
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
                preferredList.add(best);
                peerList.remove(best);
            }
        }
        
        //Now clear data counters for all neighbors
        for (int x = 0; x < peerProcess.peerList.size(); x++)
            peerProcess.rpiToPeerHandler.get(peerProcess.peerList.get(x)).clearDataCounter();
        
        //TODO: Send choke/unchoke messages (make sure to log these? or does the peerHandler do that?)
        
     }

}
