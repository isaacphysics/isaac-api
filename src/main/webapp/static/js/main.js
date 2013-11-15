var continueLogging = true;

function log(event) {
	
	event.sourcePage = window.location.pathname;
	event.server = window.location.host;
	
	if (continueLogging) {
		
		$.post(ij.proxyPath + "/api/log", {sessionId: sessionStorage.sessionId, event: JSON.stringify(event)})
		 .success(function(e) {
			 if (!e.success)
				 continueLogging = false;
		 })
		 .error(function() {
			 console.error("Error logging action:", event);
			 continueLogging = false;
		 });
	}
	else {
		
		console.log("Skipping log request - a previous request failed.");
	}
}

function loadContent(uri, addToHistory) {
	
	uri = uri.replace(ij.proxyPath,"");
	
	// Catch URLS that can be rendered without a round-trip to the server
	var renderedLocally = true;
	switch(uri)
	{
	case "/home":
		soy.renderElement($("#content")[0], rutherford.pages.home, null, ij);
		break;
	case "/register":
		soy.renderElement($("#content")[0], rutherford.pages.register, null, ij);
		break;
	case "/discussion":
		soy.renderElement($("#content")[0], rutherford.pages.discussion, null, ij);
		break;
	case "/about-us":
		soy.renderElement($("#content")[0], rutherford.pages.about_us, null, ij);
		break;
	case "/real-world":
		soy.renderElement($("#content")[0], rutherford.pages.real_world, null, ij);
		break;
	case "/applying":
		soy.renderElement($("#content")[0], rutherford.pages.applying, null, ij);
		break;
	case "/challenge":
		soy.renderElement($("#content")[0], rutherford.pages.challenge, null, ij);
		break;
	case "/why-physics":
		soy.renderElement($("#content")[0], rutherford.pages.why_physics, null, ij);
		break;
	default:
		renderedLocally = false;
		break;
	}
	
	if (!renderedLocally)
	{
		// We need to request the page from the server. Do that.
		
		var template = null;
		
		if (uri.indexOf("/learn") == 0)
			template = rutherford.pages.learn;
		
		if (uri.indexOf("/topics/") == 0)
			template = rutherford.pages.topic;
		
		if (uri.indexOf("/questions/") == 0)
			template = rutherford.pages.question;
		
		if (uri.indexOf("/concepts/") == 0)
			template = rutherford.pages.concept;
		
		
		if (template)
		{
			// This is a URI we know about
			$.get(ij.proxyPath + "/api" + uri, function(json) {
				soy.renderElement($("#content")[0], template, json, ij);
				pageRendered();
				
				
			});
		}
		else
		{
			// Not sure that this URI has a matching template on the server. Die.
			console.error("Template not found for uri", uri);
		}
	}
	else
	{
		pageRendered();
	}
	
	//var oldLoc = window.location.href;
	//urlHistory.push(oldLoc);
	
	console.log("Leaving", window.location.href);
	console.log("Arriving at", uri);
	if (addToHistory)
	{
		history.pushState(ij.proxyPath + uri,null,ij.proxyPath + uri);	
	}

	// Google Analytics
	ga('send', 'pageview', ij.proxyPath + uri);
	
	// Our analytics
	log({type: "page_render"});

}

//var urlHistory = [document.location.href];

function popHistoryState(e)
{
	console.log("Popping state. Moved to", document.location.href, "State:", e.state);
	//loadContent(document.location.href);
	if (e.state !== null)
	{
		loadContent(e.state, false);
	}
}


function click_a(e)
{
	if ($(e.target).data("playVideo"))
	{
		playVideo($(e.target).data("playVideo"));
		return;
	}

	var uri = $(e.target).data("contentUri");
	
	// handle case where you may be clicking on an object inside of an anchor
	if(uri == undefined && !$(e.target).is("a") && $(e.target).parent().is("a")){
		uri = $(e.target).parent().data("contentUri");
	}
	
	if ($(e.target).hasClass("disabled")) {
        e.stopImmediatePropagation();
        return false;
	}
	
	if (uri != undefined)
	{
		log({type: "link_click",
			 target: uri});
	
		console.log("Loading URI", uri);
		
		loadContent(uri, true);
		
		// Hack to close dropdowns:
		$(".hover").removeClass("hover");		
		
		return false;
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
	$("input[type='radio']").each(function(i,e)
	{
		correct = correct && (e.value == "1" && e.checked  || e.value == "0" && !e.checked);
	});
	$("input[type='text']").each(function(i,e)
	{
		correct = correct && ($(e).data("expectedAnswer") == e.value);
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
	console.log(correct);
	log({type: "question_response",
		 answerCorrect: correct});
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
	log({type: "play_video",
		 target: video});

	$("#video-modal video").remove();
	$("#video-modal").append($('<video width="640" height="480" controls autoplay/>').attr("src", ij.proxyPath + "/static/video/" + video));
	$('#video-modal').foundation('reveal', 'open');
}


$(function()
{
	$("body").on("click", "a", click_a);
	//$("body").on("mouseenter", "a", mouseenter_a);
	$("body").on("click", "#checkAnswer", checkAnswer_click);
	$("body").on("click", "button", button_click);
	
	$("#video-modal").on("closed", function()
	{
		$("video").remove();
	});

	$("body").on("mouseenter",".plumbLink",plumb);
	
	window.addEventListener("popstate", popHistoryState);

	var uri = document.location.pathname.substring(ij.proxyPath.length);
	history.replaceState(uri, null, ij.proxyPath + uri);
	
	MathJax.Hub.Config({
		  tex2jax: {inlineMath: [['$','$'], ['\\(','\\)']]}
		});
	
	
	if (!sessionStorage.getItem("sessionId"))
		sessionStorage.setItem("sessionId", ij.newSessionId);
	
	pageRendered();
});

function quickQuestions(){
        $('.quick-question').append("<a href='#' class='qq-toggle'>Show Answer</a>");
        
        //Hack to hide any numbers that have found there way in the list
        $('.quick-question').prev('.item-number').remove();
        
        $('.quick-question a').click(function (e){
                var answer = $(this).siblings("div:last");

                if(answer.hasClass("hidden")){
                        answer.removeClass("hidden");
                        $(this).text("Hide Answer")
                    	log({type: "show_quick_question_answer"});
                }
                else{
                        answer.addClass("hidden");
                        $(this).text("Show Answer")
                }
		e.preventDefault();
		return false;
        });
}

// Set to only work in concepts currently
function buildConcertina(){
	$('#conceptContent h5').each(function(){ 
		var headerText = $(this).text();
		
		$(this).nextUntil("h5").wrapAll('<div class="content" data-section-content/>');
		
	    $(this).nextUntil("h5").andSelf().wrapAll('<section/>');
	    
	    $(this).wrap('<div class="title" data-section-title/>');
	    
	    // Added anchor so that the link is clearly visible to screenreaders.
	    $(this).replaceWith('<h5><a href="#">' +$(this).text() + '</a></h5>');
	    
	});
	
	$("#conceptContent section").wrapAll('<div class="section-container accordion" data-section="accordion" data-options="multi_expand:true;"/>');
}

function plumb(e) {
	var myid = e.target.id;
	
	var conceptLinks = $(e.target).data("conceptLinks") || "";
	var questionLinks = $(e.target).data("questionLinks") || "";
	
	var links = [];
	links = links.concat(conceptLinks !== "" ? conceptLinks.split(",") : []);
	links = links.concat(questionLinks !== "" ? questionLinks.split(",") : []);
	
	jsPlumb.detachEveryConnection();
	for(var i in links) {
		if ($("#"+links[i]).length > 0) { // if the concept/question is missing then do nothing
			jsPlumb.connect({
				source:myid,
				target:links[i],
				endpoint:"Blank",
				connector: ["Bezier", {"curviness":50} ] ,
				anchors:[ [ [0, 0.5, -1, 0], [1,0.5,1,0] ],[ [0, 0.5, -1, 0], [1,0.5,1,0] ]]});
		}
	}
}

jsPlumb.ready(function() {
	jsPlumb.Defaults.Container = $("#content");
	// your jsPlumb related init code goes here
});

// Equivalent to our page ready
function pageRendered()
{
	MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
	quickQuestions();
	buildConcertina();
}

