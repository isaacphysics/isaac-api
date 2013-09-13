package uk.ac.cam.cl.dtg.teaching;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Path("/example")
public class ExampleController {

	@GET
	@Path("/list")
	@Produces("application/json")
	public Map<String,?> commitDB(){
		return ImmutableMap.of("items", ImmutableList.of("apples","oranges","pears"));
	}
		
}
