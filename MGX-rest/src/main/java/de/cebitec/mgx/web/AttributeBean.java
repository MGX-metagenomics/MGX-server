package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
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
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.sessions.ResultHolder;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.util.LimitingIterator;
import de.cebitec.mgx.util.Pair;
import de.cebitec.mgx.util.Triple;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.*;
import java.util.Map.Entry;

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
            attr.setJobId(dto.getJobId());
            attr.setAttributeTypeId(dto.getAttributeTypeId());

            if (dto.getParentId() != 0) {
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
        Result<Attribute> obj = mgx.getAttributeDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return AttributeDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("BySeqSun/{runid}")
    @Produces("application/x-protobuf")
    public AttributeDTOList BySeqRun(@PathParam("runid") Long runid) {
        Result<AutoCloseableIterator<Attribute>> res = mgx.getAttributeDAO().bySeqRun(runid);
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }
        return AttributeDTOFactory.getInstance().toDTOList(res.getValue());
    }

    @GET
    @Path("ByJob/{jid}")
    @Produces("application/x-protobuf")
    public AttributeDTOList ByJob(@PathParam("jid") Long jid) {
        Result<AutoCloseableIterator<Attribute>> res = mgx.getAttributeDAO().byJob(jid);
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }
        return AttributeDTOFactory.getInstance().toDTOList(res.getValue());
    }

    @GET
    @Path("getDistribution/{attrTypeId}/{jobId}/{runId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getDistribution(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId, @PathParam("runId") Long runId) {
        Result<List<Triple<Attribute, Long, Long>>> dist = mgx.getAttributeDAO().getDistribution(attrTypeId, jobId, runId);
        if (dist.isError()) {
            throw new MGXWebException(dist.getError());
        }
        return convert(dist.getValue());
    }

    @GET
    @Path("getFilteredDistribution/{filterAttrId}/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getFilteredDistribution(@PathParam("filterAttrId") Long filterAttrId, @PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {

        Result<List<Triple<Attribute, Long, Long>>> dist = mgx.getAttributeDAO().getFilteredDistribution(filterAttrId, attrTypeId, jobId);
        if (dist.isError()) {
            throw new MGXWebException(dist.getError());
        }
        return convert(dist.getValue());
    }

    @GET
    @Path("getHierarchy/{attrTypeId}/{jobId}/{runId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getHierarchy(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId, @PathParam("runId") Long runId) {

        Map<Attribute, Long> dist;

        // validate attribute type strucure
        Result<AttributeType> attrType = mgx.getAttributeTypeDAO().getById(attrTypeId);
        if (attrType.isError()) {
            throw new MGXWebException(attrType.getError());
        }

        AttributeType at = attrType.getValue();
        if (at.getStructure() != AttributeType.STRUCTURE_HIERARCHICAL) {
            throw new MGXWebException("Attribute type " + at.getName() + " is not an hierarchical attribute type.");
        }

        // TODO: check job state in sql query and remove this 
        Result<Job> job = mgx.getJobDAO().getById(jobId);
        if (job.isError() || job.getValue().getStatus() != JobState.FINISHED) {
            throw new MGXWebException("Non-existing job or job in invalid state");
        }

        Result<Map<Attribute, Long>> hierarchy = mgx.getAttributeDAO().getHierarchy(attrTypeId, jobId, runId);
        if (hierarchy.isError()) {
            throw new MGXWebException(hierarchy.getError());
        }
        return convert(hierarchy.getValue());

    }

    @GET
    @Path("getCorrelation/{attrTypeId}/{jobId}/{attrType2Id}/{job2Id}")
    @Produces("application/x-protobuf")
    public AttributeCorrelation getCorrelation(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId, @PathParam("attrType2Id") Long attrType2Id, @PathParam("job2Id") Long job2Id) {

        try {
            Result<AutoCloseableIterator<Job>> jobIter = mgx.getJobDAO().getByIds(jobId, job2Id);
            if (jobIter.isError()) {
                throw new MGXWebException(jobIter.getError());
            }
            try ( AutoCloseableIterator<Job> iter = jobIter.getValue()) {
                while (iter.hasNext()) {
                    Job job = iter.next();
                    if (job == null || job.getStatus() != JobState.FINISHED) {
                        throw new MGXWebException("Non-existing job or job in invalid state");
                    }
                }
            }
            Result<Map<Pair<Attribute, Attribute>, Integer>> correlation = mgx.getAttributeDAO().getCorrelation(attrTypeId, jobId, attrType2Id, job2Id);
            if (correlation.isError()) {
                throw new MGXWebException(correlation.getError());
            }
            return convertCorrelation(correlation.getValue());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @PUT
    @Path("find")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXStringList find(SearchRequestDTO req) {
        Result<DBIterator<String>> iterres = mgx.getAttributeDAO().find(req.getTerm(), req.getSeqrunId());
        if (iterres.isError()) {
            throw new MGXWebException(iterres.getError());
        }
        MGXStringList.Builder dtos = MGXStringList.newBuilder();

        try ( DBIterator<String> iter = iterres.getValue()) {
            while (iter.hasNext()) {
                dtos.addString(MGXString.newBuilder().setValue(iter.next()).build());
            }
        }
        return dtos.build();
    }

    @PUT
    @Path("search")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public SequenceDTOList search(SearchRequestDTO req) {
        Result<DBIterator<Sequence>> ret = mgx.getAttributeDAO().search(req.getTerm(), req.getExact(), req.getSeqrunId());
        if (ret.isError()) {
            throw new MGXWebException(ret.getError());
        }

        LimitingIterator<Sequence> liter = new LimitingIterator<>(50000, ret.getValue());
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

    private AttributeDistribution convert(Map<Attribute, Long> dist) {

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

        Result<AutoCloseableIterator<AttributeType>> iterres = mgx.getAttributeTypeDAO().getByIds(aTypes.toArray());
        if (iterres.isError()) {
            throw new MGXWebException(iterres.getError());
        }
        try ( AutoCloseableIterator<AttributeType> iter = iterres.getValue()) {
            while (iter.hasNext()) {
                b.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(iter.next()));
            }
        }

        return b.build();
    }

    private AttributeDistribution convert(List<Triple<Attribute, Long, Long>> dist) {
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

            Result<AutoCloseableIterator<AttributeType>> iterres = mgx.getAttributeTypeDAO().getByIds(aTypes.toArray());
            if (iterres.isError()) {
                throw new MGXWebException(iterres.getError());
            }
            try ( AutoCloseableIterator<AttributeType> iter = iterres.getValue()) {
                while (iter.hasNext()) {
                    b.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(iter.next()));
                }
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

        Result<AutoCloseableIterator<AttributeType>> iterres = mgx.getAttributeTypeDAO().getByIds(aTypes.toArray());
        if (iterres.isError()) {
            throw new MGXWebException(iterres.getError());
        }
        try ( AutoCloseableIterator<AttributeType> iter = iterres.getValue()) {
            while (iter.hasNext()) {
                ac.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(iter.next()));
            }
        }

        return ac.build();
    }
}
