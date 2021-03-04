import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class FileLocks {
    Set<File> fileLocks;

    public FileLocks() {
        fileLocks = new HashSet<>();
    }

    public synchronized boolean addLock(File file) {
        return fileLocks.add(file);
    }

    public synchronized boolean removeLock(File file) {
        return fileLocks.remove(file);
    }
}
