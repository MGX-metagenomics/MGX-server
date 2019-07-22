package de.cebitec.mgx.model.db;

import de.cebitec.mgx.common.JobState;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author sjaenick
 */
public class Job extends Identifiable {

    private static final long serialVersionUID = 1L;
    //
    protected long[] seqrun_ids;
    //
    protected long tool_id;
    protected String created_by;
    //
    protected Date startDate = null;
    //
    protected Date finishDate = null;
    //
    protected Collection<JobParameter> params;
    
    private int jobstate;

    public JobState getStatus() {
        return JobState.values()[jobstate];
    }

    public Job setStatus(JobState status) {
        jobstate = status.getValue();
        return this;
    }

    public long getToolId() {
        return tool_id;
    }

    public Job setToolId(long tool) {
        this.tool_id = tool;
        return this;
    }

    public Collection<JobParameter> getParameters() {
        return params;
    }

    public Job setParameters(Collection<JobParameter> params) {
        this.params = params;
        return this;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public Job setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Job setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public long[] getSeqrunIds() {
        return seqrun_ids;
    }

    public Job setSeqrunIds(long[] seqruns) {
        this.seqrun_ids = seqruns;
        return this;
    }

    public String getCreator() {
        return created_by;
    }

    public Job setCreator(String created_by) {
        this.created_by = created_by;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Job)) {
            return false;
        }
        Job other = (Job) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }

}
