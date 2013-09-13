function loadContent(url) {
	$.get(url, function(json) {
		templateResult = example.list(json);
		$("#content").html(templateResult);
	});
}