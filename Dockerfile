FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY src/main/java ./src/main/java
RUN mkdir -p target/classes && find src/main/java -name '*.java' -print > target/sources.txt \
    && javac --release 17 -encoding UTF-8 -d target/classes @target/sources.txt

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes ./target/classes
COPY credit_simulator ./credit_simulator
COPY bin/credit_simulator ./bin/credit_simulator
COPY file_inputs.txt ./file_inputs.txt
RUN chmod +x credit_simulator bin/credit_simulator
ENTRYPOINT ["./credit_simulator"]
