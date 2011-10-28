package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Sequence;

/**
 *
 * @author sjaenick
 */
public class SequenceDAO<T extends Sequence> extends DAO<T> {

    @Override
    Class getType() {
        return Sequence.class;
    }
}
