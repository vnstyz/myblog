# 多阶段构建
# 阶段一：编译打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# 先拷贝 pom 以利用依赖缓存
COPY pom.xml .
RUN mvn -B -e dependency:go-offline
# 再拷贝源码并打包（跳过测试，Docker 环境内无需跑单测）
COPY src ./src
RUN mvn -B -e clean package -DskipTests

# 阶段二：运行
FROM eclipse-temurin:21-jre
WORKDIR /app
# 使用固定的 jar 名称，便于后续升级/排查
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
