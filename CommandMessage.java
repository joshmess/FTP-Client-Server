import java.io.File;
import java.io.Serializable;

/**
 * A serializable command message that is used because commands and file chunks may be
 * sent on the same i/o stream and must be differentiated.
 */
public class CommandMessage implements Serializable {
    private TaskType command;
    private String fileName;

    public CommandMessage(TaskType command) {
        this.command = command;
    }

    public CommandMessage(TaskType command, String fileName) {
        this(command);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile(File pwd) {
        return new File(pwd, fileName);
    }
}
