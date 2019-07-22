/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationservice;

import de.cebitec.mgx.annotationservice.exception.MGXServiceException;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

/**
 *
 * @author sj
 */
public class APIKeyValidator implements ReaderInterceptor {

    @Inject
    @MGX
    MGXController mgx;

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext ric) throws IOException, WebApplicationException {
        String apiKey = ric.getHeaders().getFirst("apiKey");
        Logger.getLogger(APIKeyValidator.class.getName()).log(Level.INFO, "intercepting for {0}", mgx.getProjectName());
        try {
            mgx.getJobDAO().getByApiKey(apiKey);
        } catch (MGXException ex) {
            Logger.getLogger(APIKeyValidator.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXServiceException(Response.Status.FORBIDDEN, ex.getMessage());
        }
        return ric.proceed();
    }

}
