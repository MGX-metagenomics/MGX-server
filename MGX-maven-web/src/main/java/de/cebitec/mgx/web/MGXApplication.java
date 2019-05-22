/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.web;

import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author sj
 */
public class MGXApplication extends ResourceConfig {

    public MGXApplication() {
        super.packages("de.cebitec.mgx.web", 
                "de.cebitec.web.exception", 
                "de.cebitec.mgx.protobuf.serializer");

        super.register(UriProjectFilter.class);
        super.register(GPMSRoleFilter.class);
    }
}
