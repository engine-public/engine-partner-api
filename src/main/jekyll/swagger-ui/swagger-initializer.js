---
nav_exclude: true
---
window.onload = function() {
  window.ui = SwaggerUIBundle({
    // relative link to swagger file
    // {% capture version %}{% include version.txt %}{% endcapture %}
    url: '{{ "/downloads/engine-partner-api-" | append: version | append: ".swagger.json" | relative_url  }}',
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
