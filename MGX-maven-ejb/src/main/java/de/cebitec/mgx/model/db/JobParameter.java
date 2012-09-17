package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sj
 */
@Entity
@Table(name = "JobParameter")
public class JobParameter implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    protected Job job;
    @Basic
    @NotNull
    @Column(name = "node_id")
    protected Long node_id;
    @Basic
    @NotNull
    @Column(name = "param_name")
    protected String param_name;
    @Basic
    @NotNull
    @Column(name = "param_value")
    protected String param_value;
    @Basic
    @NotNull
    @Column(name = "user_name")
    private String user_name;
    @Basic
    @NotNull
    @Column(name = "user_desc")
    private String user_desc;

    @Override
    public Long getId() {
        return id;
    }

    public JobParameter setId(Long jp_id) {
        id = jp_id;
        return this;
    }

    public Job getJob() {
        return job;
    }

    public JobParameter setJob(Job job) {
        this.job = job;
        return this;
    }

    public long getNodeId() {
        return node_id;
    }

    public JobParameter setNodeId(long node_id) {
        this.node_id = node_id;
        return this;
    }

    public String getParameterName() {
        return param_name;
    }

    public JobParameter setParameterName(String param_name) {
        this.param_name = param_name;
        return this;
    }

    public String getParameterValue() {
        return param_value;
    }

    public JobParameter setParameterValue(String param_value) {
        this.param_value = param_value;
        return this;
    }
    
    @Transient
    String displayName;
    @Transient
    String className;
    @Transient
    Map<String, String> choices;
    @Transient
    String type;
    @Transient
    Boolean optional;
    @Transient
    String default_value = "";

    public String getUserDescription() {
        return user_desc;
    }

    public JobParameter setUserDescription(String user_desc) {
        this.user_desc = user_desc;
        return this;
    }

    public String getUserName() {
        return user_name;
    }

    public JobParameter setUserName(String user_name) {
        this.user_name = user_name;
        return this;
    }

    public JobParameter setDisplayName(String lDisplayName) {
        displayName = lDisplayName;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JobParameter setClassName(String lClassName) {
        className = lClassName;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public JobParameter setChoices(Map<String, String> lChoices) {
        choices = lChoices;
        return this;
    }

    public Map<String, String> getChoices() {
        return choices;
    }

//    public String getConfigItemValue() {
//        return configitem_value;
//    }
//
//    public void setConfigItemValue(String configitem_value) {
//        this.configitem_value = configitem_value;
//    }
    public String getDefaultValue() {
        return default_value;
    }

    public JobParameter setDefaultValue(String default_value) {
        this.default_value = default_value;
        return this;
    }

    public Boolean isOptional() {
        return optional;
    }

    public JobParameter setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public String getType() {
        return type;
    }

    public JobParameter setType(String type) {
        this.type = type;
        return this;
    }
}
