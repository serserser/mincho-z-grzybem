FROM openjdk:10-slim
MAINTAINER jgolda

ADD build/libs/totalPurchases.jar /app.jar
RUN set -o xtrace
CMD ["java", "-jar", "/app.jar"]