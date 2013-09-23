
function loadContent(uri) {
	
	// Catch URLS that can be rendered without a round-trip to the server
	switch(uri)
	{
	case "/get-started":
		soy.renderElement($("#content")[0], rutherford.pages.get_started);
		return;
	case "/learn":
		soy.renderElement($("#content")[0], rutherford.pages.learn);
		return;
	case "/discussion":
		soy.renderElement($("#content")[0], rutherford.pages.discussion);
		return;
	case "/about-us":
		soy.renderElement($("#content")[0], rutherford.pages.about_us);
		return;
	case "/real-world":
		soy.renderElement($("#content")[0], rutherford.pages.real_world);
		return;
	case "/applying":
		soy.renderElement($("#content")[0], rutherford.pages.applying);
		return;
	case "/challenge":
		soy.renderElement($("#content")[0], rutherford.pages.challenge);
		return;
	case "/why-physics":
		soy.renderElement($("#content")[0], rutherford.pages.why_physics);
		return;
	}
	
	
	// We need to request the page from the server. Do that.
	
	var template = null;
	
	if (uri.indexOf("/topics/") == 0)
		template = rutherford.pages.topic;
	
	if (uri.indexOf("/questions/") == 0)
		template = rutherford.pages.question;
	
	if (uri.indexOf("/concepts/") == 0)
		template = rutherford.pages.concept;
	
	if (template)
	{
		$.get(contextPath + "/api" + uri, function(json) {
			soy.renderElement($("#content")[0], template, json);
			MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
		});
	}
	else
	{
		console.error("Template not found for uri", uri);
	}
	
	
}

function click_a(e)
{
	var uri = $(e.target).data("contentUri");
	
	if (uri != undefined)
	{
		console.log("Loading URI", uri);
		
		loadContent(uri);
		
		// Hack to close dropdowns:
		$(".hover").removeClass("hover");		
	}
}

function mouseenter_a(e)
{
	var physicsLinks = $(e.target).data("physicsLinks") || "";
	var mathsLinks = $(e.target).data("mathsLinks") || "";
	var questionLinks = $(e.target).data("questionLinks") || "";
	
	var links = [];
	links = links.concat(physicsLinks !== "" ? physicsLinks.split(",") : []);
	links = links.concat(mathsLinks !== "" ? mathsLinks.split(",") : []);
	links = links.concat(questionLinks !== "" ? questionLinks.split(",") : []);
	
	$("a").removeClass("related-link");
		
	if (links.length > 0)
	{
		
		$(e.target).addClass("related-link");
		for(var i in links)
		{
			$('#' + links[i]).addClass("related-link");
		}
	}
}

function checkAnswer_click(e)
{
	var correct = true;
	$("input[type='checkbox']").each(function(i,e)
	{
		correct = correct && (e.value == "1" && e.checked  || e.value == "0" && !e.checked);
	});
	
	if(correct)
	{
		$(".question-wrong").hide();
		$(".question-explanation").fadeIn(200);
		$("#checkAnswer").hide();
	}
	else
	{
		$(".question-explanation").hide();
		$(".question-wrong").fadeIn(200);
	}
	console.log(correct)
}

function button_click(e)
{
	if ($(e.target).data("playVideo"))
	{
		playVideo($(e.target).data("playVideo"));
	}
}

function playVideo(video)
{
	$("#video-modal video").remove();
	$("#video-modal").append($('<video width="640" height="480" controls autoplay/>').attr("src", contextPath + "/static/video/" + video));
	$('#video-modal').foundation('reveal', 'open');
}


$(function()
{
	$("body").on("click", "a", click_a);
	$("body").on("mouseenter", "a", mouseenter_a);
	$("body").on("click", "#checkAnswer", checkAnswer_click);
	$("body").on("click", "button", button_click);
	
	$("#video-modal").on("closed", function()
	{
		$("video").remove();
	});
});