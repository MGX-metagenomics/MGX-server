/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import de.cebitec.mgx.core.MGXException;
import java.io.Serial;

/**
 *
 * @author sj
 */
public class WorkflowException extends MGXException {

    @Serial
    private static final long serialVersionUID = 1L;

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
