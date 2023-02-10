/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationservice;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.cebitec.mgx.annotationservice.exception.MGXServiceException;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Job;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
@Provider
@Priority(100)
@RequestScoped
public class APIKeyValidator implements ContainerRequestFilter {

    @Inject
    @MGX
    MGXController mgx;

    private static final Logger LOG = Logger.getLogger(APIKeyValidator.class.getName());

    private static Cache<CacheKey, Boolean> cache = null;

    public APIKeyValidator() {
        if (cache == null) {
            cache = CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();
        }
    }

    @Override
    public void filter(ContainerRequestContext cr) {

        if (mgx == null) {
            LOG.log(Level.INFO, "No MGX instance, API key access disabled.");
            throw new MGXServiceException(Response.Status.UNAUTHORIZED, "Server error");
        }

        String apiKey = cr.getHeaders().getFirst("apiKey");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new WebApplicationException("No API key provided.", Response.Status.UNAUTHORIZED);
        }

        CacheKey key = new CacheKey(apiKey, mgx.getProjectName());
        if (Objects.equals(cache.getIfPresent(key), Boolean.TRUE)) {
            // cache hit, api key is valid
            return;
        }

        // cache miss, obtain data from projects DB
        try {
            Job job = mgx.getJobDAO().getByApiKey(apiKey);
            if (job == null) {
                throw new MGXServiceException(Response.Status.UNAUTHORIZED, "API key not valid");
            }

            // validate job state
            JobState status = job.getStatus();
            if (!(status == JobState.QUEUED || status == JobState.RUNNING)) {
                throw new MGXServiceException(Response.Status.UNAUTHORIZED, "Invalid job state: " + status.toString());
            }

            cache.put(key, Boolean.TRUE);
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(Response.Status.UNAUTHORIZED, ex.getMessage());
        }
    }

    private final static class CacheKey {

        private final String apiKey;
        private final String projectName;

        public CacheKey(String apiKey, String projectName) {
            this.apiKey = apiKey;
            this.projectName = projectName;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.apiKey);
            hash = 59 * hash + Objects.hashCode(this.projectName);
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
            final CacheKey other = (CacheKey) obj;
            if (!Objects.equals(this.apiKey, other.apiKey)) {
                return false;
            }
            if (!Objects.equals(this.projectName, other.projectName)) {
                return false;
            }
            return true;
        }

    }

}
