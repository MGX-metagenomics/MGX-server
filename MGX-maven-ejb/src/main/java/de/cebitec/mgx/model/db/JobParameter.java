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

    @Override
    public Long getId() {
        return id;
    }
    
    public void setId(Long jp_id) {
        id = jp_id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Long getNodeId() {
        return node_id;
    }

    public void setNodeId(Long node_id) {
        this.node_id = node_id;
    }

    public String getParameterName() {
        return param_name;
    }

    public void setParameterName(String param_name) {
        this.param_name = param_name;
    }

    public String getParameterValue() {
        return param_value;
    }

    public void setParameterValue(String param_value) {
        this.param_value = param_value;
    }
    
    
    @Transient
    private String user_name;
    @Transient
    private String user_desc;
    @Transient
    String displayName;
    @Transient
    String className;
    @Transient
    Map<String, String> choices;
    @Transient
    String configitem_name;
    @Transient
    String configitem_value;
    @Transient
    String type;
    @Transient
    boolean optional;
    @Transient
    String default_value = "";

    public String getUserDescription() {
        return user_desc;
    }

    public void setUserDescription(String user_desc) {
        this.user_desc = user_desc;
    }

    public String getUserName() {
        return user_name;
    }

    public void setUserName(String user_name) {
        this.user_name = user_name;
    }

    public String getConfigItemName() {
        return configitem_name;
    }

    public void setConfigItemName(String configitem_name) {
        this.configitem_name = configitem_name;
    }

    public void setDisplayName(String lDisplayName) {
        displayName = lDisplayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setClassName(String lClassName) {
        className = lClassName;
    }

    public String getClassName() {
        return className;
    }

    public void setChoices(Map<String, String> lChoices) {
        choices = lChoices;
    }

    public Map<String, String> getChoices() {
        return choices;
    }

    public String getConfigItemValue() {
        return configitem_value;
    }

    public void setConfigItemValue(String configitem_value) {
        this.configitem_value = configitem_value;
    }

    public String getDefaultValue() {
        return default_value;
    }

    public void setDefaultValue(String default_value) {
        this.default_value = default_value;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
