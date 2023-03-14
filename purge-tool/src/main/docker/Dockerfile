FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11
MAINTAINER os-team@dbc.dk

ENV JAVA_OPTS=-Xmx10G

COPY target/holdings-items-purge-tool-jar-with-dependencies.jar /usr/local/share/jar/
COPY src/main/docker/holdings-items-purge-tool.sh /usr/local/bin/

ENTRYPOINT [ "/usr/local/bin//holdings-items-purge-tool.sh" ]
