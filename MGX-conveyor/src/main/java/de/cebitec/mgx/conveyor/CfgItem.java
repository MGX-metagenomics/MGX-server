/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author sj
 */
public class CfgItem implements Cloneable {

    private String name;
    private String type;
    private String description;
    private boolean optional;
    private String defaultValue;
    private String value;
    //
    private String userName;
    private String userDescription;
    //
    private Collection<Choice> choices = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        
        // TODO validate!
        this.value = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public void setUserDescription(String userDescription) {
        this.userDescription = userDescription;
    }

    @Override
    public CfgItem clone() throws CloneNotSupportedException {
        return (CfgItem) super.clone();
    }

    public void addChoice(Choice c) {
        if (choices == null) {
            choices = new ArrayList<>();
        }
        choices.add(c);
    }
    
    public Collection<Choice> getChoices() {
        return choices == null ? Collections.<Choice>emptyList() : Collections.unmodifiableCollection(choices);
    }

}
