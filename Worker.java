public class Worker implements Runnable {
    private int ID;
    private ThreadPool parent;
    private Task task;

    public Worker(int ID, ThreadPool parent) {
        this.ID = ID;
        this.parent = parent;
    }

    public int getID() {
        return ID;
    }

    @Override
    public void run() {
        while (true) {
            task = parent.getTask();
            if (task != null) {
                task.execute();
            }
            parent.removeTask(task);
        }
    }
}
