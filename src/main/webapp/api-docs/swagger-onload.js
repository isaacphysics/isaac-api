window.onload = function() {

    // Polyfill for String.endsWith
    if (!String.prototype.endsWith) {
        String.prototype.endsWith = function(search, this_len) {
            if (this_len === undefined || this_len > this.length) {
                this_len = this.length;
            }
            return this.substring(this_len - search.length, this_len) === search;
        };
    }

    // Work out where Isaac is running and where to find swagger.json:
    var baseUrl = window.location.href.replace(/api-docs.*/, "api");

    if (document.location.hostname === "localhost") {
        baseUrl ="http://" + window.location.host + "/isaac-api/api";
    } else if (document.location.hostname.endsWith(".eu.ngrok.io")) {
        // Have reserved domains on ngrok.io, hardcode them for ease of use:
        baseUrl = "https://isaacscience.eu.ngrok.io/isaac-api/api";
    }

    // Begin Swagger UI call region
    window.ui = SwaggerUIBundle({
        url: baseUrl + "/swagger.json",
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        plugins: [
            SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout",
        docExpansion: "none",
        requestInterceptor: function (request) {
            // The basePath and host in swagger.json are incorrect, because the API does not
            // know how it is being accessed. We can forcibly correct this here:
            request.url = request.url.replace(/.*?(\/api\/.*?)?\/api/, baseUrl);
            console.log(request.url);
            return request;
        }
    })
    // End Swagger UI call region
};