package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.AttributeCorrelation;
import de.cebitec.mgx.dto.dto.AttributeCorrelation.Builder;
import de.cebitec.mgx.dto.dto.AttributeCount;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.dto.dto.AttributeDistribution;
import de.cebitec.mgx.dto.dto.CorrelatedAttributeCount;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.MGXStringList;
import de.cebitec.mgx.dto.dto.SearchRequestDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.AttributeDTOFactory;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.sessions.ResultHolder;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.LimitingIterator;
import de.cebitec.mgx.util.Pair;
import de.cebitec.mgx.util.Triple;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.*;
import java.util.Map.Entry;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Attribute")
public class AttributeBean {

    @Inject
    @MGX
    MGXController mgx;
    //
    @EJB
    ResultHolder resultHolder;
    @EJB
    TaskHolder taskHolder;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(AttributeDTO dto) {
        try {
//            AttributeType attrType = mgx.getAttributeTypeDAO().getById(dto.getAttributeTypeId());
//            Job job = mgx.getJobDAO().getById(dto.getJobid());

            Attribute attr = AttributeDTOFactory.getInstance().toDB(dto);
            attr.setJobId(dto.getJobid());
            attr.setAttributeTypeId(dto.getAttributeTypeId());

            if (dto.hasParentId()) {
                //Attribute parent = mgx.getAttributeDAO().getById(dto.getParentId());
                attr.setParentId(dto.getParentId());
            }

            long id = mgx.getAttributeDAO().create(attr);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public AttributeDTO fetch(@PathParam("id") Long id) {
        Attribute obj = null;
        try {
            obj = mgx.getAttributeDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return AttributeDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("BySeqSun/{runid}")
    @Produces("application/x-protobuf")
    public AttributeDTOList BySeqRun(@PathParam("runid") Long runid) {
        AutoCloseableIterator<Attribute> iter = null;
        try {
            iter = mgx.getAttributeDAO().bySeqRun(runid);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return AttributeDTOFactory.getInstance().toDTOList(iter);
    }

    @GET
    @Path("ByJob/{jid}")
    @Produces("application/x-protobuf")
    public AttributeDTOList ByJob(@PathParam("jid") Long jid) {
        AutoCloseableIterator<Attribute> iter = null;
        try {
            iter = mgx.getAttributeDAO().byJob(jid);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return AttributeDTOFactory.getInstance().toDTOList(iter);
    }

    @GET
    @Path("getDistribution/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getDistribution(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {

        List<Triple<Attribute, Long, Long>> dist;
        try {
            dist = mgx.getAttributeDAO().getDistribution(attrTypeId, jobId);
            return convert(dist);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("getFilteredDistribution/{filterAttrId}/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getFilteredDistribution(@PathParam("filterAttrId") Long filterAttrId, @PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {

        List<Triple<Attribute, Long, Long>> dist;
        try {
            dist = mgx.getAttributeDAO().getFilteredDistribution(filterAttrId, attrTypeId, jobId);
            return convert(dist);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("getHierarchy/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getHierarchy(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {

        Map<Attribute, Long> dist;
        try {

            // validate attribute type strucure
            AttributeType attrType = mgx.getAttributeTypeDAO().getById(attrTypeId);
            if (attrType.getStructure() != AttributeType.STRUCTURE_HIERARCHICAL) {
                throw new MGXException("Attribute type " + attrType.getName() + " is not an hierarchical attribute type.");
            }

            // TODO: check job state in sql query and remove this 
            Job job = mgx.getJobDAO().getById(jobId);
            if (job == null || job.getStatus() != JobState.FINISHED) {
                throw new MGXWebException("Non-existing job or job in invalid state");
            }
            dist = mgx.getAttributeDAO().getHierarchy(attrTypeId, jobId);
            return convert(dist);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

    }

    @GET
    @Path("getCorrelation/{attrTypeId}/{jobId}/{attrType2Id}/{job2Id}")
    @Produces("application/x-protobuf")
    public AttributeCorrelation getCorrelation(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId, @PathParam("attrType2Id") Long attrType2Id, @PathParam("job2Id") Long job2Id) {

        Map<Pair<Attribute, Attribute>, Integer> ret;
        try {
//            Job job = mgx.getJobDAO().getById(jobId);
//            if (job == null || job.getStatus() != JobState.FINISHED) {
//                throw new MGXWebException("Non-existing job or job in invalid state");
//            }
//            Job job2 = mgx.getJobDAO().getById(job2Id);
//            if (job2 == null || job2.getStatus() != JobState.FINISHED) {
//                throw new MGXWebException("Non-existing job or job in invalid state");
//            }
            AutoCloseableIterator<Job> jobIter = mgx.getJobDAO().getByIds(jobId, job2Id);
            while (jobIter.hasNext()) {
                Job job = jobIter.next();
                if (job == null || job.getStatus() != JobState.FINISHED) {
                    throw new MGXWebException("Non-existing job or job in invalid state");
                }
            }

            ret = mgx.getAttributeDAO().getCorrelation(attrTypeId, jobId, attrType2Id, job2Id);
            return convertCorrelation(ret);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @PUT
    @Path("find")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXStringList find(SearchRequestDTO req) {
        AutoCloseableIterator<String> iter = null;
        try {
            iter = mgx.getAttributeDAO().find(req.getTerm(), req.getSeqrunIdList());
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        MGXStringList.Builder dtos = MGXStringList.newBuilder();
        while (iter != null && iter.hasNext()) {
            dtos.addString(MGXString.newBuilder().setValue(iter.next()).build());
        }

        return dtos.build();
    }

    @PUT
    @Path("search")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public SequenceDTOList search(SearchRequestDTO req) {
        AutoCloseableIterator<Sequence> ret = null;
        try {
            ret = mgx.getAttributeDAO().search(req.getTerm(), req.getExact(), req.getSeqrunIdList());
        } catch (MGXException ex) {
            mgx.log(ex);
            throw new MGXWebException(ex.getMessage());
        }
        LimitingIterator<Sequence> liter = new LimitingIterator<>(50000, ret);
        SequenceDTOList dtos = SequenceDTOFactory.getInstance().toDTOList(liter);

        if (!dtos.getComplete()) {
            // save iterator for continuation
            UUID uuid = UUID.fromString(dtos.getUuid());
            resultHolder.add(uuid, liter);
        }

        return dtos;
    }

    @GET
    @Path("continueSearch/{uuid}")
    //@Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public SequenceDTOList continueSearch(@PathParam("uuid") String uuid) {
        UUID tmp = UUID.fromString(uuid);
        LimitingIterator<Sequence> acit = resultHolder.get(tmp);
        if (acit == null) {
            throw new MGXWebException("Unknown UUID.");
        }
        acit.advanceOverLimit();
        SequenceDTOList dtos = SequenceDTOFactory.getInstance().toDTOList(acit, uuid);
        if (dtos.getComplete()) {
            try {
                resultHolder.close(tmp);
            } catch (MGXException ex) {
                mgx.log(ex.getMessage());
                throw new MGXWebException(ex.getMessage());
            }
        }
        mgx.log(dtos.getSeqCount() + " additional results");
        return dtos;
    }

    private AttributeDistribution convert(Map<Attribute, Long> dist) throws MGXException {

        TLongSet aTypes = new TLongHashSet();

        AttributeDistribution.Builder b = AttributeDistribution.newBuilder();

        for (Map.Entry<Attribute, Long> me : dist.entrySet()) {
            Attribute attr = me.getKey();
            AttributeDTO attrDTO = AttributeDTOFactory.getInstance().toDTO(attr);
            Long count = me.getValue();
            AttributeCount attrcnt = AttributeCount.newBuilder().setAttribute(attrDTO).setCount(count).build();

            aTypes.add(attr.getAttributeTypeId());

            b.addAttributeCounts(attrcnt);
        }

        AutoCloseableIterator<AttributeType> iter = mgx.getAttributeTypeDAO().getByIds(aTypes.toArray());
        while (iter != null && iter.hasNext()) {
            b.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(iter.next()));
        }

        return b.build();
    }

    private AttributeDistribution convert(List<Triple<Attribute, Long, Long>> dist) throws MGXException {
        // attribute, parent id, count

        TLongSet aTypes = new TLongHashSet();

        AttributeDistribution.Builder b = AttributeDistribution.newBuilder();

        if (!dist.isEmpty()) {

            for (Triple<Attribute, Long, Long> t : dist) {
                Attribute attr = t.getFirst();
                Long parentId = t.getSecond();
                Long count = t.getThird();

                AttributeDTO attrDTO = AttributeDTOFactory.getInstance().toDTO(attr, parentId);
                AttributeCount attrcnt = AttributeCount.newBuilder().setAttribute(attrDTO).setCount(count).build();

                aTypes.add(attr.getAttributeTypeId());

                b.addAttributeCounts(attrcnt);
            }

            AutoCloseableIterator<AttributeType> iter = mgx.getAttributeTypeDAO().getByIds(aTypes.toArray());
            while (iter != null && iter.hasNext()) {
                b.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(iter.next()));
            }
        }

        return b.build();
    }

    private AttributeCorrelation convertCorrelation(Map<Pair<Attribute, Attribute>, Integer> ret) throws MGXException {

        TLongSet aTypes = new TLongHashSet();
        //Set<Long> aTypes = new HashSet<>();
        Builder ac = AttributeCorrelation.newBuilder();

        for (Entry<Pair<Attribute, Attribute>, Integer> e : ret.entrySet()) {
            Attribute first = e.getKey().getFirst();
            Attribute second = e.getKey().getSecond();
            Integer count = e.getValue();

            CorrelatedAttributeCount cac = CorrelatedAttributeCount.newBuilder()
                    .setRestrictedAttribute(AttributeDTOFactory.getInstance().toDTO(first))
                    .setAttribute(AttributeDTOFactory.getInstance().toDTO(second))
                    .setCount(count)
                    .build();
            ac.addEntry(cac);

            aTypes.add(first.getAttributeTypeId());
            aTypes.add(second.getAttributeTypeId());
        }

        AutoCloseableIterator<AttributeType> iter = mgx.getAttributeTypeDAO().getByIds(aTypes.toArray());
        while (iter != null && iter.hasNext()) {
            ac.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(iter.next()));
        }

        return ac.build();
    }
}
