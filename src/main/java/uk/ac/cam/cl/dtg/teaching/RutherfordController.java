package uk.ac.cam.cl.dtg.teaching;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.template.soy.tofu.SoyTofuException;
import com.papercut.silken.SilkenServlet;
import com.papercut.silken.TemplateRenderer;

@Path("/")
public class RutherfordController {

	// Every question has a map including title and videos. Each question also has a template named by question.id, which is the key here.
	private ImmutableMap questions = ImmutableMap.builder()
			.put("a_toboggan", ImmutableMap.of(
					"title", "A Toboggan",
					"physicsConcepts", ImmutableList.of("newton_2")))
			.put("vector_vs_scalar", ImmutableMap.of(
					"title", "Vector vs. Scalar",
					"mathsConcepts", ImmutableList.of("vectors")))
			.put("head_on_collision", ImmutableMap.of(
					"title", "Head-on Collision",
					"physicsConcepts", ImmutableList.of("c_of_m", "c_of_e", "collisions", "momentum")))
			.put("what_goes_up", ImmutableMap.of(
					"title", "What Goes Up...",
					"physicsConcepts", ImmutableList.of("c_of_e", "potential_energy", "work")))
			.put("on_ice", ImmutableMap.of(
					"title", "On Ice",
					"physicsConcepts", ImmutableList.of("newton_2", "eq_motion"),
					"mathsConcepts", ImmutableList.of("vectors", "calculus")))
			.put("example_question", ImmutableMap.of(
					"title", "Example Question",
					"physicsConcepts", ImmutableList.of("newton_2", "other_physics_concept"),
					"mathsConcepts", ImmutableList.of("vectors", "other_maths_concept"),
					"problemVideos", ImmutableList.of("/videos/problemOne"))).build();
					                                 
	
	private ImmutableMap concepts = ImmutableMap.builder()
			.put("newton_2", ImmutableMap.builder()
					.put("title", "Newton's Second Law")
					.put("video", "/videos/newton_2")
					.put("type", "physics")
					.put("relatedPhysicsConcepts", ImmutableList.of("newton_1", "newton_3"))
					.put("relatedMathsConcepts", ImmutableList.of("vectors", "calculus"))
					.put("questions", ImmutableList.of("a_toboggan")).build())
			.put("vectors", ImmutableMap.builder()
					.put("title", "Vectors")
					.put("video", "/videos/vectors")
					.put("type", "maths")
					.put("relatedPhysicsConcepts", ImmutableList.of("newton_1", "newton_3"))
					.put("relatedMathsConcepts", ImmutableList.of("calculus"))
					.put("questions", ImmutableList.of("vector_vs_scalar")).build())
			.put("calculus", ImmutableMap.builder()
					.put("title", "Calculus")
					.put("video", "/videos/vectors")
					.put("type", "maths")
					.put("relatedPhysicsConcepts", ImmutableList.of("newton_1", "newton_3"))
					.put("relatedMathsConcepts", ImmutableList.of("vectors"))
					.put("questions", ImmutableList.of()).build())
			.put("c_of_m", ImmutableMap.builder()
					.put("title", "Conservation of Momentum")
					.put("video", "/videos/c_of_m")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision")).build())
			.put("c_of_e", ImmutableMap.builder()
					.put("title", "Conservation of Energy")
					.put("video", "/videos/c_of_e")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision", "what_goes_up")).build())
			.put("collisions", ImmutableMap.builder()
					.put("title", "Collisions")
					.put("video", "/videos/c_of_e")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision", "what_goes_up")).build())
			.put("momentum", ImmutableMap.builder()
					.put("title", "Momentum")
					.put("video", "/videos/c_of_e")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision", "what_goes_up")).build())
			.put("potential_energy", ImmutableMap.builder()
					.put("title", "Potential Energy")
					.put("video", "/videos/c_of_e")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision", "what_goes_up")).build())
			.put("work", ImmutableMap.builder()
					.put("title", "Work")
					.put("video", "/videos/c_of_e")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision", "what_goes_up")).build())
			.put("eq_motion", ImmutableMap.builder()
					.put("title", "Equations of Motion")
					.put("video", "/videos/c_of_e")
					.put("type", "physics")
					.put("questions", ImmutableList.of("head_on_collision", "what_goes_up")).build())
			.build();
	
	private ImmutableMap topicQuestions = ImmutableMap.of(
			"dynamics", ImmutableMap.of(
					"as-1", ImmutableMap.of(
								"questions", ImmutableList.of(
										"a_toboggan", 
										"vector_vs_scalar",
										"head_on_collision",
										"what_goes_up"/*,
										"on_ice"*/),
								"concepts", ImmutableList.of(
										"newton_2",
										"vectors",
										"calculus",
										"c_of_m",
										"c_of_e",
										"collisions",
										"momentum",
										"potential_energy",
										"work",
										"eq_motion"))));
	

	
	@GET
	@Path("topics/{topic}/{level}")
	@Produces("application/json")
	public Map<String,?> getTopicWithLevel(@PathParam("topic") String topic, @PathParam("level") String level){
		
		ImmutableList questions = ((ImmutableList)((ImmutableMap)((ImmutableMap)topicQuestions.get(topic)).get(level)).get("questions"));
		ImmutableList concepts = ((ImmutableList)((ImmutableMap)((ImmutableMap)topicQuestions.get(topic)).get(level)).get("concepts"));

		return ImmutableMap.of("topic", ImmutableMap.builder().put("name", topic)
				                                              .put("level", level)
				                                              .put("questions", questions)
				                                              .put("concepts", concepts).build(),
				               "questionDb", this.questions,
				               "conceptDb", this.concepts);
	}
	
	@GET
	@Path("topics/{topic}")
	@Produces("application/json")
	public Map<String,?> getTopic(@PathParam("topic") String topic){
		String level = "as-1";
		return getTopicWithLevel(topic, level);
	}
	
	@GET
	@Path("questions/{question}")
	@Produces("application/json")
	public Map<String, ?> getQuestion(@PathParam("question") String question)
	{
		TemplateRenderer renderer = SilkenServlet.getTemplateRenderer();
		
		String qContent = "";
		try
		{
			qContent = renderer.render("rutherford.questions." + question, null);
		} catch (SoyTofuException e)
		{
			e.printStackTrace();
			qContent = "<i>No content available.</i>";
		}
		
		
		return ImmutableMap.of("question", ImmutableMap.of("id", question,
					                                       "details", questions.get(question),
				                                           "content", qContent),
				               "concepts", concepts);
	}

	@GET
	@Path("concepts/{concept}")
	@Produces("application/json")
	public Map<String, ?> getConcept(@PathParam("concept") String concept)
	{
		TemplateRenderer renderer = SilkenServlet.getTemplateRenderer();
		
		String cContent = "";
		try
		{
			cContent = renderer.render("rutherford.concepts." + concept, null);
		} catch (SoyTofuException e)
		{
			cContent = "<i>No content available.</i>";
		}
		
		return ImmutableMap.of("concept", ImmutableMap.of("id", concept,
					                                      "details", concepts.get(concept),
				                                          "content", cContent),
				               "questions", ((ImmutableMap)concepts.get(concept)) != null ? ((ImmutableMap)concepts.get(concept)).get("questions") : ImmutableList.of(),
				               "questionDb", this.questions);
	}
}
