package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dnautils.DNAUtils;
import de.cebitec.mgx.dto.dto.GeneDTO;
import de.cebitec.mgx.dto.dto.GeneDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dtoadapter.GeneDTOFactory;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.Gene;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
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
    TaskHolder taskHolder;

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
    public GeneDTOList byAssembly(@PathParam("id") Long id) {
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
        try {
            Gene gene = mgx.getGeneDAO().getById(id);
            Contig contig = mgx.getContigDAO().getById(gene.getContigId());
            Bin bin = mgx.getBinDAO().getById(contig.getBinId());
            File assemblyDir = new File(mgx.getProjectAssemblyDirectory(), String.valueOf(bin.getAssemblyId()));
            File binFasta = new File(assemblyDir, String.valueOf(bin.getId()) + ".fna");
            String geneSeq;
            try (IndexedFastaSequenceFile ifsf = new IndexedFastaSequenceFile(binFasta)) {
                ReferenceSequence seq;
                if (gene.getStart() < gene.getStop()) {
                    // htsjdk uses 1-based positions
                    seq = ifsf.getSubsequenceAt(contig.getName(), gene.getStart() + 1, gene.getStop() + 1);
                    geneSeq = new String(seq.getBases());
                } else {
                    seq = ifsf.getSubsequenceAt(contig.getName(), gene.getStop() + 1, gene.getStart() + 1);
                    geneSeq = DNAUtils.reverseComplement(new String(seq.getBases()));
                }
                if (seq == null || seq.length() == 0) {
                    throw new MGXWebException("No sequence found for contig " + contig.getName());
                }
            }
            return SequenceDTO.newBuilder()
                    .setId(id)
                    .setName(contig.getName() + "_" + String.valueOf(id))
                    .setSequence(geneSeq)
                    .build();

        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
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
}
