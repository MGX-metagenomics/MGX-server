package de.cebitec.mgx.sessions;

/**
 *
 * @author sjaenick
 */
public abstract class TaskI implements Runnable {

    public enum State {

        INIT(0),
        PROCESSING(1),
        FAILED(2),
        FINISHED(3);
        private int code;

        private State(int c) {
            code = c;
        }

        public int getValue() {
            return code;
        }
    }
    private final String projName;
    private String statusMessage = "";
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
        timeStamp = System.currentTimeMillis();
        return state;
    }

    public String getStatusMessage() {
        timeStamp = System.currentTimeMillis();
        return statusMessage;
    }

    protected void setStatus(State s, String msg) {
        state = s;
        statusMessage = msg;
    }
}
