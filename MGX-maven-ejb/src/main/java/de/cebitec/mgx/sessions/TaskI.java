package de.cebitec.mgx.sessions;

/**
 *
 * @author sjaenick
 */
public abstract class TaskI implements Runnable {

    public enum State {

        INIT,
        PROCESSING,
        FAILED,
        FINISHED;
    }
    private final String projName;
    protected long timeStamp;
    protected State state;

    public TaskI(String projName) {
        this.projName = projName;
        timeStamp = System.currentTimeMillis();
        state = State.INIT;
    }

    public long lastAccessed() {
        return timeStamp;
    }

    public abstract void cancel();

    public abstract void close();

    public String getProjectName() {
        return projName;
    }
    
    public State getState() {
        return state;
    }
}
