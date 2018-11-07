package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * Created by Ian on 17/10/2016.
 */
@Path("/etl")
@Api(value = "/etl")
public class ETLFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(ETLFacade.class);

    private final ETLManager etlManager;

    /**
     * Constructor that provides a properties loader.
     *
     * @param properties the propertiesLoader.
     */
    @Inject
    public ETLFacade(PropertiesLoader properties, ETLManager manager) {
        super(properties, null);
        this.etlManager = manager;
    }

    @POST
    @Path("/set_version_alias/{alias}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setLiveVersion(@PathParam("alias") final String alias, @PathParam("version") final String version) {

        try {
            etlManager.setNamedVersion(alias, version);
            log.info("Finished processing ETL request");
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Failed to set alias version:" + e.getMessage());
            log.info("Finished processing ETL request");
            return Response.serverError().entity(e.getMessage()).build();
        }

    }

    @POST
    @Path("/new_version_alert/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newVersionAlert(@PathParam("version") final String newVersion) {
        etlManager.notifyNewVersion(newVersion);
        log.info("Finished processing ETL request");
        return Response.ok().build();
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response statusCheck() {
        return Response.ok().entity("{\"code\" : 200}").build();
    }



    //***************************
    // ADMIN FACADE
    //***************************

//    /**
//     * This method will allow the live version served by the site to be changed.
//     *
//     * @param request
//     *            - to help determine access rights.
//     * @param version
//     *            - version to use as updated version of content store.
//     * @return Success shown by returning the new liveSHA or failed message "Invalid version selected".
//     */
//    @POST
//    @Path("/live_version/{version}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public synchronized Response changeLiveVersion(@Context final HttpServletRequest request,
//            @PathParam("version") final String version) {
//
//        try {
//            if (isUserAnAdmin(request)) {
//                IContentManager contentPersistenceManager = contentVersionController.getContentManager();
//                String newVersion;
//                if (!contentPersistenceManager.isValidVersion(version)) {
//                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid version selected: "
//                            + version);
//                    log.warn(error.getErrorMessage());
//                    return error.toResponse();
//                }
//
//                if (!contentPersistenceManager.getCachedVersionList().contains(version)) {
//                    newVersion = contentVersionController.triggerSyncJob(version).get();
//                } else {
//                    newVersion = version;
//                }
//
//                Collection<String> availableVersions = contentPersistenceManager.getCachedVersionList();
//
//                if (!availableVersions.contains(version)) {
//                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid version selected: "
//                            + version);
//                    log.warn(error.getErrorMessage());
//                    return error.toResponse();
//                }
//
//                contentVersionController.setLiveVersion(newVersion);
//                log.info("Live version of the site changed to: " + newVersion + " by user: "
//                        + this.userManager.getCurrentRegisteredUser(request).getEmail());
//
//                return Response.ok().build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN,
//                        "You must be logged in as an admin to access this function.").toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        } catch (InterruptedException e) {
//            log.error("ExecutorException during version change.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while trying to terminate a process.", e)
//                    .toResponse();
//        } catch (ExecutionException e) {
//            log.error("ExecutorException during version change.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
//        }
//    }
//
//    /**
//     * This method will try to bring the live version that Segue is using to host content up-to-date with the latest in
//     * the database.
//     *
//     * @param request
//     *            - to enable security checking.
//     * @return a response to indicate the synchronise job has triggered.
//     */
//    @POST
//    @Path("/synchronise_datastores")
//    public synchronized Response synchroniseDataStores(@Context final HttpServletRequest request) {
//        try {
//            // check if we are authorized to do this operation.
//            // no authorisation required in DEV mode, but in PROD we need to be
//            // an admin.
//            if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT)
//                    .equals(Constants.EnvironmentType.PROD.name())
//                    || isUserAnAdmin(request)) {
//                log.info("Informed of content change; " + "so triggering new synchronisation job.");
//                contentVersionController.triggerSyncJob().get();
//                return Response.ok("success - job started").build();
//            } else {
//                log.warn("Unable to trigger synch job as not an admin or this server is set to the PROD environment.");
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            log.warn("Unable to trigger synch job as not logged in.");
//            return SegueErrorResponse.getNotLoggedInResponse();
//        } catch (InterruptedException e) {
//            log.error("ExecutorException during synchronise datastores operation.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while trying to terminate a process.", e)
//                    .toResponse();
//        } catch (ExecutionException e) {
//            log.error("ExecutorException during synchronise datastores operation.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
//        }
//    }
//
//    /**
//     * This method is only intended to be used on development / staging servers.
//     *
//     * It will try to bring the live version that Segue is using to host content up-to-date with the latest in the
//     * database.
//     *
//     * @param request
//     *            - to enable security checking.
//     * @return a response to indicate the synchronise job has triggered.
//     */
//    @POST
//    @Path("/new_version_alert")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response versionChangeNotification(@Context final HttpServletRequest request) {
//        // check if we are authorized to do this operation.
//        // no authorisation required in DEV mode, but in PROD we need to be
//        // an admin.
//        try {
//            if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT)
//                    .equals(Constants.EnvironmentType.PROD.name())
//                    || this.isUserAnAdmin(request)) {
//                log.info("Informed of content change; so triggering new async synchronisation job.");
//                // on this occasion we don't want to wait for a response.
//                contentVersionController.triggerSyncJob();
//                return Response.ok().build();
//            } else {
//                log.warn("Unable to trigger synch job as this segue environment is "
//                        + "configured in PROD mode unless you are an ADMIN.");
//                return new SegueErrorResponse(Status.FORBIDDEN,
//                        "You must be an administrator to use this function on the PROD environment.").toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            return new SegueErrorResponse(Status.UNAUTHORIZED,
//                    "You must be logged in to use this function on a PROD environment.").toResponse();
//        }
//    }
//
//    /**
//     * This method will delete all cached data from the CMS and any search indices.
//     *
//     * @param request
//     *            - containing user session information.
//     *
//     * @return the latest version id that will be cached if content is requested.
//     */
//    @POST
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/clear_caches")
//    public synchronized Response clearCaches(@Context final HttpServletRequest request) {
//        try {
//            if (isUserAnAdmin(request)) {
//                IContentManager contentPersistenceManager = contentVersionController.getContentManager();
//                RegisteredUserDTO currentRegisteredUser = userManager.getCurrentRegisteredUser(request);
//
//                log.info(String.format("Admin user: (%s) triggered cache clears...", currentRegisteredUser.getEmail()));
//                contentPersistenceManager.clearCache();
//
//                ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>().put("result",
//                        "success").build();
//
//                return Response.ok(response).build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        }
//    }
//
//    /**
//     * This method will show a string representation of all jobs in the to index queue.
//     *
//     * @param request
//     *            - containing user session information.
//     *
//     * @return the latest queue information
//     */
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/content_index_queue")
//    public synchronized Response getCurrentIndexQueue(@Context final HttpServletRequest request) {
//        try {
//            if (isUserStaff(request)) {
//                ImmutableMap<String, Object> response = new ImmutableMap.Builder<String, Object>().put("queue",
//                        contentVersionController.getToIndexQueue()).build();
//
//                return Response.ok(response).build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        }
//    }
//
//    /**
//     * This method will delete all jobs not yet started in the indexer queue.
//     *
//     * @param request
//     *            - containing user session information.
//     *
//     * @return the new queue.
//     */
//    @DELETE
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/content_index_queue")
//    public synchronized Response deleteAllInCurrentIndexQueue(@Context final HttpServletRequest request) {
//        try {
//            if (isUserAnAdmin(request)) {
//                RegisteredUserDTO u = userManager.getCurrentRegisteredUser(request);
//                log.info(String.format("Admin user (%s) requested to empty indexer queue.", u.getEmail()));
//
//                contentVersionController.cleanUpTheIndexQueue();
//
//                ImmutableMap<String, Object> response = new ImmutableMap.Builder<String, Object>().put("queue",
//                        contentVersionController.getToIndexQueue()).build();
//
//                return Response.ok(response).build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        }
//    }
}
