FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11
MAINTAINER os-team@dbc.dk

COPY target/holdings-items-purge-tool-jar-with-dependencies.jar /usr/local/share/jar/

ENTRYPOINT [ "java", "-jar", "/usr/local/share/jar/holdings-items-purge-tool-jar-with-dependencies.jar" ]
