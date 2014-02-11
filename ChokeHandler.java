import java.util.ArrayList;


public class ChokeHandler implements Runnable {
	ArrayList<RemotePeerInfo> interestedList = new ArrayList<RemotePeerInfo>();
	private final int interval;
	
	public ChokeHandler(int chokeInterval) {
		interval = chokeInterval;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// Handle periodic optimistic unchoking & preferred neighbors
	}
	
	public void choke(RemotePeerInfo rpi) {
		
	}
	
	public void unchoke(RemotePeerInfo rpi) {
		
	}
	
	public void checkForNew() {
		
	}
}
