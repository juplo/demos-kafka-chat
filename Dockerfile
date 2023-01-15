FROM eclipse-temurin:17-jre
VOLUME /tmp
COPY target/*.jar /opt/app.jar
ENTRYPOINT [ "java", "-jar", "/opt/app.jar" ]
CMD []
