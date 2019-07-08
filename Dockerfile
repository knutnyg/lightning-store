FROM adoptopenjdk/openjdk11
COPY build/libs/lightning-store-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"
