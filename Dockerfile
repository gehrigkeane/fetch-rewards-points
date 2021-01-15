FROM adoptopenjdk:15-jre-hotspot

WORKDIR /app
COPY target/fetch-rewards-points.jar /app/fetch-rewards-points.jar

EXPOSE 8080
ARG JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseStringDeduplication"
ARG SPRING_PROFILES_ACTIVE=default
CMD ["/bin/bash", "-c", "java $JAVA_OPTS -jar /app/fetch-rewards-points.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
