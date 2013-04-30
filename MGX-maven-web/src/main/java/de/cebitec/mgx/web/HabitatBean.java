package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.HabitatDTO;
import de.cebitec.mgx.dto.dto.HabitatDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.HabitatDTOFactory;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.sessions.TaskI;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Path("Habitat")
@Stateless
public class HabitatBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong create(HabitatDTO dto) {
        Habitat h = HabitatDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getHabitatDAO().create(h);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public Response update(HabitatDTO dto) {
        Habitat h = HabitatDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getHabitatDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public HabitatDTO fetch(@PathParam("id") Long id) {
        Habitat obj = null;
        try {
            obj = mgx.getHabitatDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return HabitatDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public HabitatDTOList fetchall() {
        return HabitatDTOFactory.getInstance().toDTOList(mgx.getHabitatDAO().getAll());
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId = taskHolder.addTask(new DeleteHabitat(mgx.getConnection(), id, mgx.getProjectName()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    private final class DeleteHabitat extends TaskI {

        private final Connection conn;
        private final long id;

        public DeleteHabitat(Connection conn, long id, String projName) {
            super(projName);
            this.conn = conn;
            this.id = id;
        }

        @Override
        public void cancel() {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(JobBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void close() {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(JobBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            try {
                setStatus(TaskI.State.PROCESSING, "Deleting habitat");
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM habitat WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
                conn.close();
                state = TaskI.State.FINISHED;
            } catch (Exception e) {
                setStatus(TaskI.State.FAILED, e.getMessage());
            }
            setStatus(TaskI.State.FINISHED, "Complete");
        }
    }
}
