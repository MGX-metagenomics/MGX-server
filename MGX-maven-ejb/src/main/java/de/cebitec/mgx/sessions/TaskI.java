package de.cebitec.mgx.sessions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public abstract class TaskI implements Runnable, PropertyChangeListener {

    public enum State {

        INIT(0),
        PROCESSING(1),
        FAILED(2),
        FINISHED(3);
        private final int code;

        private State(int c) {
            code = c;
        }

        public int getValue() {
            return code;
        }
    }
    private final PropertyChangeSupport pcs;
    protected final String projName;
    protected Connection conn;
    private String statusMessage = "";
    protected long timeStamp;
    private State state;
    private boolean isSubTask = true;

    public TaskI(String pName, Connection c) {
        projName = pName;
        conn = c;
        timeStamp = System.currentTimeMillis();
        state = State.INIT;
        pcs = new PropertyChangeSupport(this);
    }

    void setMainTask() {
        isSubTask = false;
    }

    long lastAccessed() {
        return timeStamp;
    }

    public void cancel() {
        try {
            if (conn != null && !isSubTask) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TaskI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void close() {
        try {
            if (conn != null && !isSubTask) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TaskI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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

    protected void setStatus(State newState, String msg) {
        if (state.equals(State.FAILED) && !newState.equals(State.FAILED)) {
            Logger.getLogger(TaskI.class.getName()).log(Level.SEVERE, "Invalid task state transition, failed -> {0}", newState);
            return;
        }

        if (!state.equals(newState)) {
            // for finished subtasks, replace finished state be processing state
            if (newState.equals(State.FINISHED) && isSubTask) {
                newState = State.PROCESSING;
            }
            state = newState;
            if (isSubTask) {
                pcs.firePropertyChange("state", null, state);
            }
        }
        if (!statusMessage.equals(msg)) {
            statusMessage = msg;
            if (isSubTask) {
                pcs.firePropertyChange("message", null, statusMessage);
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // status updates of child tasks are promoted to the parent task
        // using property changes. 

        switch (evt.getPropertyName()) {
            case "state":
                State newState = (State) evt.getNewValue();
                if (State.FINISHED.equals(newState)) {
                    // child tasks must NOT set finished state
                    return;
                }
                setStatus(newState, statusMessage);
                break;
            case "message":
                setStatus(state, (String) evt.getNewValue());
                break;
        }
        pcs.firePropertyChange(evt); // forward event
    }
}
