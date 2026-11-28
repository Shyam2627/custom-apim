FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY order-service/target/order-service-*.jar /app/app.jar

EXPOSE 8082

RUN useradd --system --create-home --uid 1001 appuser && chown -R appuser:appuser /app
RUN mkdir -p /app/logs && chown -R appuser:appuser /app/logs
USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
