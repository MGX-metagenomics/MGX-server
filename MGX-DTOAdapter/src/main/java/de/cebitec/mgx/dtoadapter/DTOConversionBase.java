package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public abstract class DTOConversionBase<T, U, V> {

    private final Logger logger = Logger.getLogger(getClass().getPackage().getName());

    public abstract U toDTO(T a);

    public abstract T toDB(U dto);

    public abstract V toDTOList(AutoCloseableIterator<T> list);

    public final void log(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }
        logger.log(Level.INFO, "{0}", new Object[]{msg});
    }

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
        return new Date(1000L * timestamp);
    }
}
