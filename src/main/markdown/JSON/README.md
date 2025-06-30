# JSON

The provided [engine-partner-api.swagger.json](./engine-partner-api.swagger.json) files provide documentation for the HTTP/JSON implementation of the Engine Partner API.

The provided [docker-compose definition](compose.yaml) allows the swagger documentation to be browsed using [swagger-ui](https://swagger.io/tools/swagger-ui/).

You may use docker to launch the browser:

```bash
docker compose up
```

<!-- markdownlint-disable-next-line MD034 -->
Then navigate to swagger-ui in your browser at http://localhost:9753

Note: Due to limitations in swagger-ui and the pre-3.1 swagger spec, swagger-ui cannot be directly used to execute the UI due to lack of mTLS support.
