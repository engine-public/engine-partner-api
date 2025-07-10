---
---
window.onload = function() {
  window.ui = SwaggerUIBundle({
    // relative link to swagger file
    url: "/downloads/engine-partner-api-{% include version.txt %}.swagger.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
        // the slice disables the top bar
      SwaggerUIStandalonePreset.slice(1)
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });
};
