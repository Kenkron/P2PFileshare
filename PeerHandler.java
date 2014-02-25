import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PeerHandler {
	public Socket socket = null;
	private OutputStream oos = null;
	private InputHandler ih = null;

	public PeerHandler(Socket s) {
		this.socket = s;
		try {
			oos = s.getOutputStream();
			ih = new InputHandler(s);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendHandshake() {
		
	}

	/**
	 * Start the InputHandler
	 */
	public void start() {
		ih.start();
	}
	
	class InputHandler extends Thread {
		private InputStream ois = null;

		public InputHandler(Socket s) {
			try {
				ois = s.getInputStream();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				int next=0;
				while(next>=0) {
					next = ois.read();
					Logger.debug(4, "PeerHandler: port "+socket.getPort()+" recieved "+next);
					//TODO: HANDLE INCOMING MESSAGES
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
