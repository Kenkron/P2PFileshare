import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PeerHandler {
	private Socket socket = null;
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
				while(true) {
					ois.read();
					//TODO: HANDLE INCOMING MESSAGES
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
