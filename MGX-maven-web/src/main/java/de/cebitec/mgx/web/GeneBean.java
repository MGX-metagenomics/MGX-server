package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.GeneByAttributeDownloadProvider;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.dto.dto.GeneDTO;
import de.cebitec.mgx.dto.dto.GeneDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.GeneDTOFactory;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.model.db.Gene;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;
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
@Path("Gene")
@Stateless
public class GeneBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    DownloadSessions downSessions;
    @EJB
    TaskHolder taskHolder;
    @EJB
    Executor executor;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(GeneDTO dto) {
        Gene x = GeneDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getGeneDAO().create(x);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(GeneDTO dto) {
        Gene h = GeneDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getGeneDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public GeneDTO fetch(@PathParam("id") Long id) {
        Gene obj = null;
        try {
            obj = mgx.getGeneDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return GeneDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public GeneDTOList fetchall() {
        try {
            return GeneDTOFactory.getInstance().toDTOList(mgx.getGeneDAO().getAll());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("byBin/{id}")
    @Produces("application/x-protobuf")
    public GeneDTOList byBin(@PathParam("id") Long id) {
        AutoCloseableIterator<Gene> bins;
        try {
            bins = mgx.getGeneDAO().byBin(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return GeneDTOFactory.getInstance().toDTOList(bins);
    }

    @GET
    @Path("byContig/{id}")
    @Produces("application/x-protobuf")
    public GeneDTOList byContig(@PathParam("id") Long id) {
        AutoCloseableIterator<Gene> bins;
        try {
            bins = mgx.getGeneDAO().byContig(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return GeneDTOFactory.getInstance().toDTOList(bins);
    }

    @GET
    @Path("getDNASequence/{id}")
    @Produces("application/x-protobuf")
    public SequenceDTO getDNASequence(@PathParam("id") Long id) {
        Sequence obj;
        try {
            obj = mgx.getGeneDAO().getDNASequence(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SequenceDTOFactory.getInstance().toDTO(obj);
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId;
        try {
            taskId = taskHolder.addTask(mgx.getGeneDAO().delete(id));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();

    }

    @PUT
    @Path("initDownloadforAttributes")
    @Produces("application/x-protobuf")
    public MGXString initDownloadforAttributes(AttributeDTOList attrdtos) {

        if (attrdtos.getAttributeCount() == 0) {
            throw new MGXWebException("No attributes provided.");
        }

        GeneByAttributeDownloadProvider provider = null;
        try {
            long[] attributeIDs = new long[attrdtos.getAttributeCount()];
            int i = 0;
            for (AttributeDTO dto : attrdtos.getAttributeList()) {
                attributeIDs[i++] = dto.getId();
            }

            mgx.log("Creating attribute-based gene download session for " + mgx.getProjectName());

            provider = new GeneByAttributeDownloadProvider(mgx.getDataSource(), mgx.getProjectName(),
                    attributeIDs, mgx.getProjectAssemblyDirectory());
        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeDownload/{uuid}")
    public Response closeDownload(@PathParam("uuid") UUID session_id) {
        try {
            downSessions.closeSession(session_id);
            mgx.log("Download finished for " + mgx.getProjectName());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetchSequences/{uuid}")
    @Consumes("application/x-protobuf")
    public SequenceDTOList fetchSequences(@PathParam("uuid") UUID session_id) {
        try {
            DownloadProviderI<SequenceDTOList> session = downSessions.getSession(session_id);
            SequenceDTOList ret = session.fetch();
            if (session instanceof Runnable) {
                // submit for async background prefetch
                executor.execute((Runnable) session);
            }
            return ret;
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
    }
}
