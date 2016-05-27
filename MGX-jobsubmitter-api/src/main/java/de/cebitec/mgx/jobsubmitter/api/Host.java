/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.jobsubmitter.api;

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

}
