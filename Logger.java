public class Logger implements Runnable {
		
		public void run() {}
		
		public static void log(String message) {
			System.out.println(message);
		}
		
		public static void err(String message) {
			System.err.println(message);
		}
		
}