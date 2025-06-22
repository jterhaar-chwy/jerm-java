FROM 278833423079.dkr.ecr.us-east-1.amazonaws.com/plat/java-baseimg:21-latest

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
