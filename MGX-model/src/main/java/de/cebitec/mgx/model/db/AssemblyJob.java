package de.cebitec.mgx.model.db;

import de.cebitec.mgx.common.JobState;
import java.util.Date;

/**
 *
 * @author sjaenick
 */
public class AssemblyJob extends Identifiable {

    private static final long serialVersionUID = 1L;
    //
    protected Long[] seqruns;
    //
    //protected long tool_id;
    protected String created_by;
    //
    protected Date startDate = null;
    //
    protected Date finishDate = null;
    //
    private int jobstate;
    //
    protected String apiKey;

    public JobState getStatus() {
        return JobState.values()[jobstate];
    }

    public AssemblyJob setStatus(JobState status) {
        jobstate = status.getValue();
        return this;
    }

//    public long getToolId() {
//        return tool_id;
//    }
//
//    public AssemblyJob setToolId(long tool) {
//        this.tool_id = tool;
//        return this;
//    }

    public Date getFinishDate() {
        return finishDate;
    }

    public AssemblyJob setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public AssemblyJob setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Long[] getSeqrunIds() {
        return seqruns;
    }

    public AssemblyJob setSeqruns(Long[] seqruns) {
        this.seqruns = seqruns;
        return this;
    }

    public String getCreator() {
        return created_by;
    }

    public AssemblyJob setCreator(String created_by) {
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
        if (!(object instanceof AssemblyJob)) {
            return false;
        }
        AssemblyJob other = (AssemblyJob) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }

}
