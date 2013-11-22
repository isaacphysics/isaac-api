package uk.ac.cam.cl.dtg.teaching;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.template.soy.tofu.SoyTofuException;
import com.papercut.silken.SilkenServlet;
import com.papercut.silken.TemplateRenderer;

@WebServlet(//
name = "pages",//
urlPatterns = {
		"/home", 
		"/register", 
		"/about-us", 
		"/learn", 
		"/topics/*", 
		"/questions/*", 
		"/concepts/*",
		"/discussion",
		"/real-world",
		"/applying",
		"/challenge",
		"/why-physics",
		"/contact-us"},
loadOnStartup = 1,//
initParams = { @WebInitParam(name = "disableCaching", value = "true") }//
)
public class PageServlet extends HttpServlet {
	
	private static final Logger log = LoggerFactory.getLogger(PageServlet.class);
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException
	{
		PrintWriter out = res.getWriter();
		res.setContentType("text/html");
		res.setCharacterEncoding("UTF-8");
		
		TemplateRenderer renderer = SilkenServlet.getTemplateRenderer();

		String cContent = "";
		try {
			ImmutableMap<String,String> ij = RutherfordController.getSoyGlobalMap(req);
			String uri = req.getRequestURI().substring(((String)ij.get("contextPath")).length());
			RutherfordController rc = new RutherfordController();
			
			if (uri.startsWith("/learn"))
			{
				cContent = renderer.render("rutherford.pages.learn", rc.getTopics(), ij, Locale.ENGLISH);
			}
			else if (uri.startsWith("/topics"))
			{
				Matcher m = Pattern.compile("/topics/(.+)/level\\-(\\d+)").matcher(uri);
				m.find();
				cContent = renderer.render("rutherford.pages.topic", rc.getTopicWithLevel(m.group(1), m.group(2)), ij, Locale.ENGLISH);
			}
			else if(uri.startsWith("/questions"))
			{
				Matcher m = Pattern.compile("/questions/(.+)").matcher(uri);
				m.find();
				cContent = renderer.render("rutherford.pages.question", rc.getQuestion(req, m.group(1)), ij, Locale.ENGLISH);
			}
			else if(uri.startsWith("/concepts"))
			{
				Matcher m = Pattern.compile("/concepts/(.+)").matcher(uri);
				m.find();
				cContent = renderer.render("rutherford.pages.concept", rc.getConcept(req, m.group(1)), ij, Locale.ENGLISH);
			}
			else if (uri.startsWith("/home"))
			{
				cContent = renderer.render("rutherford.pages.home", null, ij, Locale.ENGLISH);
			}
			else
			{
				cContent = renderer.render("rutherford.pages." + uri.substring(1).replace("-","_"), null, ij, Locale.ENGLISH);
			}
			
		} catch (SoyTofuException e) {
			cContent = "<i>No content available.</i>";
			log.error("Error applying soy template", e);
		}
		
		
		
		String cLayout = "";
		try {
			cLayout = renderer.render("rutherford.main", ImmutableMap.of("content", cContent), RutherfordController.getSoyGlobalMap(req), Locale.ENGLISH);
		} catch (SoyTofuException e) {
			cLayout = "<i>No content available.</i>";
			log.error("Error applying soy template", e);
		}
		
		out.print(cLayout);
		out.close();

	}

	
}
