window.onload = function () {
    //<editor-fold desc="Changeable Configuration Block">

    // Work out where Isaac is running and where to find swagger.json:
    var baseUrl = window.location.href.replace(/api-docs.*/, "api");

    if (document.location.hostname === "localhost") {
        baseUrl ="http://" + window.location.host + "/isaac-api/api";
    } else if (document.location.hostname.endsWith(".eu.ngrok.io")) {
        // Have reserved domains on ngrok.io, hardcode them for ease of use:
        baseUrl = "https://isaacscience.eu.ngrok.io/isaac-api/api";
    }

    // the following lines will be replaced by docker/configurator, when it runs in a docker-container
    window.ui = SwaggerUIBundle({
        url: baseUrl + "/openapi.json",
        dom_id: '#swagger-ui',
        deepLinking: true,
        docExpansion: 'none',
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        plugins: [
            SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout"
    });

    //</editor-fold>
}
