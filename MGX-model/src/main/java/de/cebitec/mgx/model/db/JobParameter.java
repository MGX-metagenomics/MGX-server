package de.cebitec.mgx.model.db;

import java.util.Map;

/**
 *
 * @author sj
 */
public class JobParameter extends Identifiable {

    private static final long serialVersionUID = 1L;
    protected long job;
    protected Long node_id;
    protected String param_name;
    protected String param_value;
    private String user_name;
    private String user_desc;

    public long getJobId() {
        return job;
    }

    public JobParameter setJobId(long job) {
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
    
    String displayName;
    String className;
    Map<String, String> choices;
    String type;
    Boolean optional;
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
