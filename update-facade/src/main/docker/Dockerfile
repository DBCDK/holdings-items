FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11

ENV LOG_FORMAT=json
EXPOSE 8080/tcp 8686/tcp
CMD ["/entrypoint.sh"]

LABEL ${docker.labels.base} ${docker.labels.httpClient} \
    HAZELCAST_CLUSTER="DNS name for hazelcast cluster"

ADD target/docker/entrypoint.sh /entrypoint.sh
ADD target/${project.artifactId}-jar-with-dependencies.jar /service.jar
