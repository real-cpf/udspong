FROM amazoncorretto:18-al2-jdk
COPY ./main /app
# ENTRYPOINT exec java -jar /app/udsPong-0.1.jar node2
# For Spring-Boot project, use the entrypoint below to reduce Tomcat startup time.
#ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar podman.jar
