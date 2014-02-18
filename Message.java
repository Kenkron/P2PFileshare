import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
//import com.sun.xml.internal.messaging.saaj.util.ByteArrayOutputStream;

public class Message{

    public enum MessageType{
    	CHOKE, UNCHOKE, INTERESTED, NOT_INTERESTED, HAVE, BITFIELD, REQUEST, PIECE
    }

    MessageType type;

    int length;
    
    ByteBuffer data;

    public Message(MessageType type){
	this.type=type;
    }

    public static byte[] encode(boolean bitmap[]) throws IOException{
    	ByteArrayOutputStream out = new ByteArrayOutputStream(bitmap.length/8);
    	writeBooleans(out,bitmap);
    	return out.toByteArray();
    }
    
    private static void writeBooleans(OutputStream out, boolean[] ar) throws IOException {
        for (int i = 0; i < ar.length; i += 8) {
            int b = 0;
            for (int j = Math.min(i + 7, ar.length-1); j >= i; j--) {
                b = (b << 1) | (ar[j] ? 1 : 0);
            }
            out.write(b);
        }
    }

    private static void readBooleans(InputStream in, boolean[] ar) throws IOException {
        for (int i = 0; i < ar.length; i += 8) {
            int b = in.read();
            if (b < 0) throw new EOFException();
            for (int j = i; j < i + 8 && j < ar.length; j++) {
                ar[j] = (b & 1) != 0;
                b >>>= 1;
            }
        }
    }
}
