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
public class NodeType {
    
    private final String className;
    private String displayName;
    private String description;
    private Collection<CfgItem> items;
    private String name;

    public NodeType(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    
    public String getClassName() {
        return className;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public void addItem(CfgItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }
    
    public Collection<CfgItem> getConfigItems() {
        return items == null ? Collections.<CfgItem>emptyList() : Collections.<CfgItem>unmodifiableCollection(items);
    }
    
}
