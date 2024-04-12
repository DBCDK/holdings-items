FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11

ENV LOG_FORMAT=json
EXPOSE 8080/tcp 8686/tcp
CMD ["/entrypoint.sh"]

LABELS TITLE="Name of service exposed in test client (required)" \
       CONNECT_TIMEOUT="Duration for connection to content service (default: 2s)" \
       EXPOSED_URL="Url this is exposed upon, for context root and url in WSDL (required)" \
       LOG_LEVEL="Log level (default: INFO)" \
       ROOT_LOG_LEVEL="Non dbc log level (default: WARN)" \
       HTTP_PORT="Service port (default: 8080)" \
       HTTP_POOL_SIZE="Concurrent requests to content service (default: 4)" \
       READ_TIMEOUT="Duration for a response from content service (default: 2m)" \
       TARGET_URL="Content service url (bean url) (required)" \
       USER_AGENT="Request user agent (default: soap-facade-service/1.0)"

ADD target/docker/entrypoint.sh /entrypoint.sh
ADD target/${project.artifactId}-jar-with-dependencies.jar /service.jar
