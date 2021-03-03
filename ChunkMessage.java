/**
 * A serializable file chunk message that is used because commands and file chunks may be
 * sent on the same i/o stream and must be differentiated.
 */
public class ChunkMessage implements Message {
    private CommandMessage command;
    private char[] buffer;

    public ChunkMessage(CommandMessage command, char[] buffer) {
        this.command = command;
        this.buffer = buffer;
    }

    public char[] getBuffer() {
        return buffer;
    }

    public CommandMessage getCommand() {
        return command;
    }
}
