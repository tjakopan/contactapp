FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
ENV PROJECT_DIR=/project
WORKDIR $PROJECT_DIR
COPY src $PROJECT_DIR/src
COPY pom.xml $PROJECT_DIR
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-alpine
ENV APP_DIR=/application
ENV APP_FILE=contactapp.jar

EXPOSE 8888

WORKDIR $APP_DIR
COPY --from=builder /project/target/*-fat.jar $APP_DIR/$APP_FILE

ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $APP_FILE"]
