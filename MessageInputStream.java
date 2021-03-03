import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class MessageInputStream extends ObjectInputStream {
    public MessageInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
    }
    
    public synchronized Message readMessage() throws IOException {
           return readObject();
}

    // TODO
}
