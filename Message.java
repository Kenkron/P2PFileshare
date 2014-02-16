import java.io.ByteArrayInputStream;

public class Message {

	public enum MessageType {}
	
	int length;
	
	Byte[] data;

	static ByteArrayInputStream createByteStr() {
		byte[] buf = new byte[0];
		return new ByteArrayInputStream(buf);
	}
	
	static void decodeByteStr(ByteArrayInputStream byteStr) {
		
	}

}