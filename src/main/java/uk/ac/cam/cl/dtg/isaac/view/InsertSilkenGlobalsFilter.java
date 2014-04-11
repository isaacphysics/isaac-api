package uk.ac.cam.cl.dtg.isaac.view;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.app.IsaacController;
import uk.ac.cam.cl.dtg.isaac.app.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.database.SegueGuiceConfigurationModule;

@WebFilter("/*")
public class InsertSilkenGlobalsFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule());
		IsaacController isaacController = injector.getInstance(IsaacController.class);
		
		HttpServletRequest req = (HttpServletRequest) request;
		req.setAttribute("globals",
				isaacController.getSoyGlobalMap(req));
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

}
