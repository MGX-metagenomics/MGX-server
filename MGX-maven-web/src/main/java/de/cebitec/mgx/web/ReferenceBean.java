/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dtoadapter.DNAExtractDTOFactory;
import de.cebitec.mgx.dtoadapter.ReferenceDTOFactory;
import de.cebitec.mgx.model.dao.deleteworkers.DeleteDNAExtract;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
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
 * @author belmann
 */
@Stateless
@Path("Reference")
public class ReferenceBean {

    @Inject
    @MGX
    MGXController mgx;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public dto.MGXLong create(dto.ReferenceDTO dto) {
        Long Reference_id = null;
        try {
            //Sample s = mgx.getSampleDAO().getById(dto.getSampleId());
            Reference ref = ReferenceDTOFactory.getInstance().toDB(dto);

            Reference_id = mgx.getReferenceDAO().create(ref);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(Reference_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    public Response update(dto.ReferenceDTO dto) {

        Reference reference = ReferenceDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getReferenceDAO().update(reference);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public dto.ReferenceDTO fetch(@PathParam("id") Long id) {
        Reference obj = null;
        try {
            obj = (Reference) mgx.getReferenceDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ReferenceDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public dto.ReferenceDTOList fetchall() {
        return ReferenceDTOFactory.getInstance().toDTOList(mgx.getReferenceDAO().getAll());
    }

    @GET
    @Path("installGlobalTool/{global_id}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public long installGlobalReference(long globalId) {
        Reference globalRef = null;
        try {
            globalRef = mgx.getGlobal().getReferenceDAO().getById(globalId);
            // copy sequence data
            //copyFile(globalRef.getFile(), mgx.getProjectDirectory() + "/reference/" + globalRef.getName() + ".fas");
        } catch (MGXException ex) {
            Logger.getLogger(ReferenceBean.class.getName()).log(Level.SEVERE, null, ex);
        }

        Reference newRef = new Reference();
        // attribute von globalRef rueberkopieren: name/length/..
        newRef.setFile(mgx.getProjectDirectory() + "/reference/" + globalRef.getName() + ".fas");
        newRef.setName(globalRef.getName());
        newRef.setFile(globalRef.getFile());
        newRef.setId(globalRef.getId());
        newRef.setLength(globalRef.getLength());


        for (Region r : globalRef.getRegions()) {
            Region newReg = new Region();
            newReg.setDescription(r.getDescription());
            newReg.setId(r.getId());
            newReg.setReference(newRef);
            newReg.setStart(r.getStart());
            newReg.setStop(r.getStop());
            newRef.getRegions().add(newReg);
        }
        try {
            mgx.getReferenceDAO().create(newRef);
        } catch (MGXException ex) {
            Logger.getLogger(ReferenceBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newRef.getId();

    }
}
