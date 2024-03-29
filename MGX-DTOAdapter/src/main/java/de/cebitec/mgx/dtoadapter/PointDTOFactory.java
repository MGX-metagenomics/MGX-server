package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.PointDTO;
import de.cebitec.mgx.dto.dto.PointDTOList;
import de.cebitec.mgx.statistics.data.Point;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sj
 */
public class PointDTOFactory extends DTOConversionBase<Point, PointDTO, PointDTOList> {

    static {
        instance = new PointDTOFactory();
    }
    protected final static PointDTOFactory instance;

    private PointDTOFactory() {
    }

    public static PointDTOFactory getInstance() {
        return instance;
    }

    @Override
    public PointDTO toDTO(Point a) {
        PointDTO.Builder dto = PointDTO.newBuilder()
                .setX(a.getX())
                .setY(a.getY());
        if (a.getName() != null) {
            dto.setName(a.getName());
        }
        return dto.build();
    }

    @Override
    public Point toDB(PointDTO dto) {
        if (!dto.getName().isEmpty()) {
            return new Point(dto.getX(), dto.getY(), dto.getName());
        } else {
            return new Point(dto.getX(), dto.getY());
        }
    }

    @Override
    public PointDTOList toDTOList(AutoCloseableIterator<Point> acit) {
        PointDTOList.Builder b = PointDTOList.newBuilder();
        try (AutoCloseableIterator<Point> iter = acit) {
            while (iter.hasNext()) {
                b.addPoint(toDTO(iter.next()));
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        return b.build();
    }
}
