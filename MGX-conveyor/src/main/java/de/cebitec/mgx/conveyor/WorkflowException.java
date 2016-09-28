/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import de.cebitec.mgx.core.MGXException;

/**
 *
 * @author sj
 */
public class WorkflowException extends MGXException {

    public WorkflowException(Throwable cause) {
        super(cause);
    }

    public WorkflowException(String msg) {
        super(msg);
    }

    public WorkflowException(String msg, Object... args) {
        super(msg, args);
    }
    
}
