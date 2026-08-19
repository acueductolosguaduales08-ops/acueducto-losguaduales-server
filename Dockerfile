FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
# Paso separado para que Docker cachee las dependencias entre builds: mientras el pom.xml no
# cambie, este paso (el mas pesado de descargar) se reusa aunque cambie el codigo fuente. Esto
# acelera el BUILD en Render (el tiempo de "desplegando"), no el arranque del contenedor ya
# construido (ver las banderas JVM mas abajo para eso).
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/acueducto-backend.jar /app/app.jar
EXPOSE 8080
ENV PORT=8080
# Banderas pensadas para el plan gratuito de Render (contenedor chico: 512MB RAM / 0.1 CPU) y
# para minimizar el tiempo de arranque en frio (spin-up tras dormir por inactividad), sin tocar
# el comportamiento de la aplicacion:
# - TieredStopAtLevel=1: usa solo el compilador JIT rapido (C1), sin el compilador de mayor
#   rendimiento (C2). Arranca mas rapido; el costo es algo menos de rendimiento en picos de
#   trafico sostenido, que no es el perfil de uso de este servicio.
# - UseSerialGC: recolector de basura de un solo hilo, con menos overhead de arranque y memoria
#   que el recolector por defecto (G1), apropiado para una CPU tan limitada (0.1 core).
# NO se fijo un tamano de heap (-Xmx) a mano para no arriesgar quedarse corto en otro plan de
# Render con mas o menos memoria: el JVM ya detecta el limite del contenedor automaticamente.
ENTRYPOINT ["java","-XX:TieredStopAtLevel=1","-XX:+UseSerialGC","-jar","/app/app.jar"]
