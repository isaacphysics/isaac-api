package uk.ac.cam.cl.dtg.isaac.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
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

import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A resource that displays a list of available endpoints (which is helpful to
 * see for debugging purposes).
 * 
 * @author Patrick Stegmann
 *         https://gist.github.com/wonderb0lt/10731371#file-overviewresource
 *         -java Retrieved 27/05/2014
 * @author Stephen Cummins - modified to work with new version of resteasy
 *         27/05/2014
 */
@Path("/")
public class APIOverviewResource {
	private static final Logger log = LoggerFactory
			.getLogger(APIOverviewResource.class);

	/**
	 * POJO to represent Method information collected using reflection and rest
	 * easy registry.
	 * 
	 */
	public static final class MethodDescription implements Serializable {
		private static final long serialVersionUID = 4395400223398707629L;
		private String method;
		private String fullPath;
		private String produces;
		private String consumes;
		private boolean deprecated;
		
		/**
		 * Default Constructor.
		 */
		public MethodDescription() {

		}
		
		/**
		 * Full Constructor.
		 * @param method - 
		 * @param fullPath -
		 * @param produces -
		 * @param consumes -
		 * @param deprecated -
		 */
		public MethodDescription(String method, String fullPath,
				String produces, String consumes, boolean deprecated) {
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((consumes == null) ? 0 : consumes.hashCode());
			result = prime * result + (deprecated ? 1231 : 1237);
			result = prime * result + ((fullPath == null) ? 0 : fullPath.hashCode());
			result = prime * result + ((method == null) ? 0 : method.hashCode());
			result = prime * result + ((produces == null) ? 0 : produces.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof MethodDescription)) {
				return false;
			}
			MethodDescription other = (MethodDescription) obj;
			if (consumes == null) {
				if (other.consumes != null) {
					return false;
				}
			} else if (!consumes.equals(other.consumes)) {
				return false;
			}
			if (deprecated != other.deprecated) {
				return false;
			}
			if (fullPath == null) {
				if (other.fullPath != null) {
					return false;
				}
			} else if (!fullPath.equals(other.fullPath)) {
				return false;
			}
			if (method == null) {
				if (other.method != null) {
					return false;
				}
			} else if (!method.equals(other.method)) {
				return false;
			}
			if (produces == null) {
				if (other.produces != null) {
					return false;
				}
			} else if (!produces.equals(other.produces)) {
				return false;
			}
			return true;
		}
	}

	/**
	 * POJO to represent Resource information collected using reflection and
	 * rest easy registry.
	 * 
	 */
	public static final class ResourceDescription implements Serializable {
		private static final long serialVersionUID = 4692040940508432363L;
		private String basePath;
		private List<MethodDescription> calls;
		
		/**
		 * Default Constructor.
		 */
		public ResourceDescription() {

		}

		public ResourceDescription(String basePath) {
			this.basePath = basePath;
			this.calls = Lists.newArrayList();
		}

		public void addMethod(String path, ResourceInvoker resourceInvoker) {
			if (resourceInvoker instanceof ResourceMethodInvoker) {
				ResourceMethodInvoker method = (ResourceMethodInvoker) resourceInvoker;

				String produces = mostPreferredOrNull(Arrays.asList(method
						.getProduces()));
				String consumes = mostPreferredOrNull(Arrays.asList(method
						.getConsumes()));

				for (String verb : method.getHttpMethods()) {
					MethodDescription md = new MethodDescription(verb, path, produces,
							consumes, method.getMethod().isAnnotationPresent(
									Deprecated.class));
					
					if (!calls.contains(md)) {
						calls.add(md);	
					} else {
						// This will happen when the same method signature is
						// registered with more than one method invoker. It
						// could be a sign of something wrong especially if you
						// think they are supposed to be singletons.
						log.error("Duplicate interceptor detected. Check your application register. "
								+ md.getMethod() + " " + md.getFullPath());
					}
				}
			}
		}

		private static String mostPreferredOrNull(List<MediaType> preferred) {
			if (preferred.isEmpty()) {
				return null;
			} else {
				return preferred.get(0).toString();
			}
		}

		public static List<ResourceDescription> fromBoundResourceInvokers(
				Set<Map.Entry<String, List<ResourceInvoker>>> bound) {
			Map<String, ResourceDescription> descriptions = Maps.newHashMap();

			for (Map.Entry<String, List<ResourceInvoker>> entry : bound) {
				ResourceInvoker aMethod = entry.getValue().get(0);
				String basePath = aMethod.getMethod().getDeclaringClass()
						.getAnnotation(Path.class).value();
				String methodEndpoint = aMethod.getMethod()
						.getAnnotation(Path.class).value();

				if (!descriptions.containsKey(basePath)) {
					descriptions.put(basePath,
							new ResourceDescription(basePath));
				}

				for (ResourceInvoker invoker : entry.getValue()) {
					descriptions.get(basePath).addMethod(
							basePath + methodEndpoint, invoker);
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
	public List<ResourceDescription> getAvailableEndpoints(
			@Context Dispatcher dispatcher) {
		ResourceMethodRegistry registry = (ResourceMethodRegistry) dispatcher
				.getRegistry();

		List<ResourceDescription> descriptions = ResourceDescription
				.fromBoundResourceInvokers(registry.getBounded().entrySet());
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
	public Response getAvailableEndpointsHtml(
			@Context final HttpServletRequest request, 
			@Context final Dispatcher dispatcher) {
		log.info("Requesting endpoint list from api using HTML view.");
		
		Injector injector = Guice.createInjector(
				new SegueGuiceConfigurationModule());
		PropertiesLoader properties = injector.getInstance(PropertiesLoader.class);
		
		StringBuilder sb = new StringBuilder();
		ResourceMethodRegistry registry = (ResourceMethodRegistry) dispatcher
				.getRegistry();
		List<ResourceDescription> descriptions = ResourceDescription
				.fromBoundResourceInvokers(registry.getBounded().entrySet());

		sb.append("<h1>").append("REST interface overview").append("</h1>");

		for (ResourceDescription resource : descriptions) {
			sb.append("<h2>").append(resource.getBasePath()).append("</h2>");
			sb.append("<ul>");
			List<MethodDescription> methodsList = resource.getCalls();

			// sort the list to make it easier for me to find things
			Collections.sort(methodsList, new Comparator<MethodDescription>() {

				@Override
				public int compare(final MethodDescription o1, final MethodDescription o2) {
					return o1.getFullPath().compareTo(o2.getFullPath());
				}

			});

			for (MethodDescription method : methodsList) {

				sb.append("<li> ");
				if (method.isDeprecated()) {
					sb.append(" Deprecated ");
					sb.append("<del>");
				}
				sb.append(method.getMethod()).append(" ");

				sb.append("<strong> <a href='" + "http://" 
						+ properties.getProperty(uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME) 
						+ "/api/"
						+ method.getFullPath().substring(1) + "'>").append(method.getFullPath())
						.append("</a></strong>");

				sb.append("<ul>");

				if (method.getConsumes() != null) {
					sb.append("<li>").append("Consumes: ")
							.append(method.getConsumes()).append("</li>");
				}

				if (method.getProduces() != null) {
					sb.append("<li>").append("Produces: ")
							.append(method.getProduces()).append("</li>");
				}

				if (method.isDeprecated()) {
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