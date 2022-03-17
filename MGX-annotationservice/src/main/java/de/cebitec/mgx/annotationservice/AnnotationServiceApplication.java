/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationservice;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;


/**
 *
 * @author sj
 */
@ApplicationPath("/webresources")
public class AnnotationServiceApplication extends Application {

    public AnnotationServiceApplication() {
//        super.packages("de.cebitec.mgx.annotationservice", 
//                "de.cebitec.mgx.annotationservice.exception", 
//                "de.cebitec.mgx.protobuf.serializer");
//
//        super.register(MGXProjectFilter.class);
//        super.register(APIKeyValidator.class);
    }
}
