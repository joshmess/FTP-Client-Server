import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Synchronized output stream for ftp messages.
 */
public class MessageOutputStream extends ObjectOutputStream {
    public MessageOutputStream(OutputStream outputStream) throws IOException {
        super(outputStream);
    }

    /**
     * Locks and writes message to output stream.
     * @param message a serialized message
     * @throws IOException
     */
    public synchronized void writeMessage(Message message) throws IOException {
        writeObject(message);
    }
}
