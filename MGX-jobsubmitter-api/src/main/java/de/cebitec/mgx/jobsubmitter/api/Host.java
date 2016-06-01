/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.jobsubmitter.api;

import java.util.Objects;

/**
 *
 * @author sj
 */
public final class Host {

    private final String name;

    public Host(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Host other = (Host) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

}
