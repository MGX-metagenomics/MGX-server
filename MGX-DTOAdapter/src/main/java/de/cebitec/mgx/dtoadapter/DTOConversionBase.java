package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.model.db.Identifiable;
import java.util.Date;

/**
 *
 * @author sjaenick
 */
public abstract class DTOConversionBase<T extends Identifiable, U> {
    
    public abstract U toDTO(T a);

    public abstract T toDB(U dto);

    protected static Long toUnixTimeStamp(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime() / 1000L;
    }

    protected static Date toDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Date(timestamp);
    }
}
