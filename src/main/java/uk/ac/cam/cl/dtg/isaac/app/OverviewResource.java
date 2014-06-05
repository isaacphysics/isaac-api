package uk.ac.cam.cl.dtg.isaac.app;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * A resource that displays a list of available endpoints (which is helpful to see for debugging purposes).
 * 
 * @author Patrick Stegmann https://gist.github.com/wonderb0lt/10731371#file-overviewresource-java Retrieved 27/05/2014
 * @author Stephen Cummins - modified to work with new version of resteasy 27/05/2014
 */
@Path("/")
public class OverviewResource {
	private static final Logger log = LoggerFactory.getLogger(OverviewResource.class);

	/**
     * POJO to represent Method information collected using reflection and rest easy registry
     *
     */
	public static final class MethodDescription implements Serializable {
		private static final long serialVersionUID = 4395400223398707629L;
		private String method;
        private String fullPath;
        private String produces;
        private String consumes;
        private boolean deprecated;

        public MethodDescription(){
        	
        }
        
        public MethodDescription(String method, String fullPath, String produces, String consumes, boolean deprecated) {
            super();
            this.method = method;
            this.fullPath = fullPath;
            this.produces = produces;
            this.consumes = consumes;
            this.deprecated = deprecated;
        }

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public String getFullPath() {
			return fullPath;
		}

		public void setFullPath(String fullPath) {
			this.fullPath = fullPath;
		}

		public String getProduces() {
			return produces;
		}

		public void setProduces(String produces) {
			this.produces = produces;
		}

		public String getConsumes() {
			return consumes;
		}

		public void setConsumes(String consumes) {
			this.consumes = consumes;
		}

		public boolean isDeprecated() {
			return deprecated;
		}

		public void setDeprecated(boolean deprecated) {
			this.deprecated = deprecated;
		}

    }
	
	/**
     * POJO to represent Resource information collected using reflection and rest easy registry
     *
     */
    public static final class ResourceDescription implements Serializable {
		private static final long serialVersionUID = 4692040940508432363L;
		private String basePath;
		private List<MethodDescription> calls;
    	
		public ResourceDescription(){
			
		}
		
        public ResourceDescription(String basePath) {
            this.basePath = basePath;
            this.calls = Lists.newArrayList();
        }

        public void addMethod(String path, ResourceInvoker resourceInvoker) {
            if(resourceInvoker instanceof ResourceMethodInvoker){
            	ResourceMethodInvoker method = (ResourceMethodInvoker) resourceInvoker;
            	
            	String produces = mostPreferredOrNull(Arrays.asList(method.getProduces()));
                String consumes = mostPreferredOrNull(Arrays.asList(method.getConsumes()));

                for (String verb : method.getHttpMethods()) {
                    calls.add(new MethodDescription(verb, path, produces, consumes, method.getMethod().isAnnotationPresent(Deprecated.class)));
                }
            }
        }

        private static String mostPreferredOrNull(List<MediaType> preferred) {
            if (preferred.isEmpty()) {
                return null;
            }
            else {
                return preferred.get(0).toString();
            }
        }

        public static List<ResourceDescription> fromBoundResourceInvokers(Set<Map.Entry<String, List<ResourceInvoker>>> bound) {
            Map<String, ResourceDescription> descriptions = Maps.newHashMap();

            for (Map.Entry<String, List<ResourceInvoker>> entry : bound) {
            	ResourceInvoker aMethod = entry.getValue().get(0);
                String basePath = aMethod.getMethod().getDeclaringClass().getAnnotation(Path.class).value();
                String methodEndpoint = aMethod.getMethod().getAnnotation(Path.class).value();

                if (!descriptions.containsKey(basePath)) {
                    descriptions.put(basePath, new ResourceDescription(basePath));
                }

                for (ResourceInvoker invoker : entry.getValue()) {
                    descriptions.get(basePath).addMethod(basePath + methodEndpoint, invoker);
                }
            }

            return Lists.newArrayList(descriptions.values());
        }

		public String getBasePath() {
			return basePath;
		}

		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}

		public List<MethodDescription> getCalls() {
			return calls;
		}

		public void setCalls(List<MethodDescription> calls) {
			this.calls = calls;
		}
    }

    /**
     * List all available end points
     * 
     * @param dispatcher
     * @return JSON response for all available endpoints
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Cache
    public List<ResourceDescription> getAvailableEndpoints(@Context Dispatcher dispatcher) {
        ResourceMethodRegistry registry = (ResourceMethodRegistry) dispatcher.getRegistry();
        
        List<ResourceDescription> descriptions = ResourceDescription.fromBoundResourceInvokers(registry.getBounded().entrySet());
        log.info("Requesting endpoint list from api using json view.");
        return descriptions;
    }

    /**
     * List all available endpoints as HTML
     * 
     * @param dispatcher
     * @return a page listing the available endpoints.
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Cache
    public Response getAvailableEndpointsHtml(@Context Dispatcher dispatcher) {
        log.info("Requesting endpoint list from api using HTML view.");
        
        StringBuilder sb = new StringBuilder();
        ResourceMethodRegistry registry = (ResourceMethodRegistry) dispatcher.getRegistry();
        List<ResourceDescription> descriptions = ResourceDescription.fromBoundResourceInvokers(registry.getBounded().entrySet());

        sb.append("<h1>").append("REST interface overview").append("</h1>");

        for (ResourceDescription resource : descriptions) {
            sb.append("<h2>").append(resource.getBasePath()).append("</h2>");
            sb.append("<ul>");
            List<MethodDescription> methodsList = resource.getCalls();

            // sort the list to make it easier for me to find things
            Collections.sort(methodsList, new Comparator<MethodDescription>(){

				@Override
				public int compare(MethodDescription o1, MethodDescription o2) {
					return o1.getFullPath().compareTo(o2.getFullPath());
				}
            	
            });
            
            for (MethodDescription method : methodsList) {          	

            	sb.append("<li> ");
            	if(method.isDeprecated()){
            		sb.append(" Deprecated ");
            		sb.append("<del>");
            	}
            	sb.append(method.getMethod()).append(" ");
            	
                sb.append("<strong>").append(method.getFullPath()).append("</strong>");

                sb.append("<ul>");

                if (method.getConsumes() != null) {
                    sb.append("<li>").append("Consumes: ").append(method.getConsumes()).append("</li>");
                }

                if (method.getProduces() != null) {
                    sb.append("<li>").append("Produces: ").append(method.getProduces()).append("</li>");
                }

            	if(method.isDeprecated()){
            		sb.append("</del>");
            	}
                sb.append("</li>");
                sb.append("</ul>");
            }

            sb.append("</ul>");
        }


        
        return Response.ok(sb.toString()).build();

    }
}