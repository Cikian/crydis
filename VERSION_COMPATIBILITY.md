# Crydis 多版本兼容性使用指南

## 📋 版本支持一览表

| Java 版本 | Spring Boot 版本 | 编译命令 | 说明 |
|----------|-----------------|--------|------|
| 8-17    | 2.6.6-2.7.x    | 默认或 `-P spring-boot-2.6` | ✅ 推荐用于现有项目 |
| 8       | 2.6.6-2.6.x    | `-P spring-boot-2.6` | ✅ 最低版本支持 |
| 17+     | 3.0-3.1.x      | `-P spring-boot-3.0` | ✅ 支持虚拟线程等特性 |
| 17+     | 3.2.x+         | `-P spring-boot-3.2` | ✅ 最新 Spring Boot 3 |
| 21+     | 4.0.x          | `-P spring-boot-4.0` | ⚠️ 预览版本 |

---

## 🔧 使用方式

### 1️⃣ **Spring Boot 2.6.6 项目（Java 8+）**

```bash
# 编译时指定 Profile
mvn clean install -P spring-boot-2.6

# 或在项目 pom.xml 中依赖
<dependency>
    <groupId>cn.cikian</groupId>
    <artifactId>crydis</artifactId>
    <version>0.1.0</version>
</dependency>

# 配置 application.yml
crydis:
  enable: true
  host: localhost
  port: 6379
  database: 0
  timeout: 3000
  max-active: 50
  max-idle: 10
  min-idle: 5
  max-wait: 3000
```

### 2️⃣ **Spring Boot 2.7.x 项目（Java 8-17）**

```bash
# 使用默认配置编译（推荐）
mvn clean install

# 依赖配置（同上）
```

### 3️⃣ **Spring Boot 3.0-3.1 项目（Java 17+）**

```bash
# 编译时指定 Profile
mvn clean install -P spring-boot-3.0

# 依赖配置（同上）
# 注意：Spring Boot 3.x 会自动使用 Jedis 4.4.3
```

### 4️⃣ **Spring Boot 3.2+ 项目（Java 17+）**

```bash
# 编译时指定 Profile
mvn clean install -P spring-boot-3.2

# 依赖配置（同上）
```

### 5️⃣ **Spring Boot 4.x 项目（Java 21+）**

```bash
# 编译时指定 Profile（需要 JDK 21+）
mvn clean install -P spring-boot-4.0

# 依赖配置（同上）
# 注意：需要配置 Spring 的 snapshot 仓库
```

### 6️⃣ **非 Spring 项目（Java 8+）**

```java
import cn.cikian.crydis.CrydisManager;
import cn.cikian.crydis.service.Crydis;

public class Main {
    public static void main(String[] args) {
        // 手动初始化
        CrydisManager.builder()
                .host("localhost")
                .port(6379)
                .password(null)
                .database(0)
                .timeout(3000)
                .maxActive(50)
                .maxIdle(10)
                .minIdle(5)
                .maxWait(3000L)
                .init();

        // 使用静态方法调用
        Crydis.set("key", "value");
        String value = Crydis.get("key");
        System.out.println(value);

        // 销毁连接
        CrydisManager.destroy();
    }
}
```

---

## 🧪 Java 版本验证

如果需要针对特定 Java 版本测试代码编译：

```bash
# 测试 Java 11 编译
mvn clean test -P java-11

# 测试 Java 17 编译
mvn clean test -P java-17

# 测试 Java 21 编译
mvn clean test -P java-21
```

---

## 📦 依赖版本对应表

| 配置 Profile | Spring Boot | Java | Jackson | Logback | Jedis |
|------------|------------|------|---------|---------|-------|
| default | 2.7.18 | 8 | 2.15.4 | 1.2.13 | 3.9.0 |
| spring-boot-2.6 | 2.6.15 | 8 | 2.15.4 | 1.2.13 | 3.9.0 |
| spring-boot-3.0 | 3.1.7 | 17 | 2.15.4 | 1.2.13 | 4.4.3 |
| spring-boot-3.2 | 3.2.2 | 17 | 2.16.1 | 1.4.14 | 4.4.3 |
| spring-boot-4.0 | 4.0.0-SNAPSHOT | 21 | 2.17.0 | 1.5.0 | 5.0.0 |

---

## ⚡ 关键改进说明

### 1. **灵活的版本管理**
```xml
<!-- ❌ 旧方式：所有项目都被强制使用 Spring Boot Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<!-- ✅ 新方式：使用 BOM，可以独立管理版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. **独立 Profile 系统**
```xml
<!-- 每个 Profile 可以独立配置版本 -->
<profile>
    <id>spring-boot-2.6</id>
    <properties>
        <spring-boot.version>2.6.15</spring-boot.version>
        <java.version>1.8</java.version>
    </properties>
    <dependencyManagement>
        <!-- 独立的依赖管理 -->
    </dependencyManagement>
</profile>
```

### 3. **Java 编译器参数优化**
```xml
<maven.compiler.release>8</maven.compiler.release>
<!-- 使用 release 参数保证最好的跨版本兼容性 -->
```

### 4. **更新的插件版本**
- Maven Compiler Plugin: 3.12.1（从 3.11.0）
- Javadoc Plugin: 3.6.3（从 3.6.2）
- Surefire Plugin: 3.1.2（新增，完全支持 JUnit 5）

---

## 🚀 发布新版本步骤

```bash
# 1. 清理并编译（使用默认 Spring Boot 2.7.x）
mvn clean install

# 2. 测试 Spring Boot 2.6 兼容性
mvn clean test -P spring-boot-2.6

# 3. 测试 Spring Boot 3.x 兼容性
mvn clean test -P spring-boot-3.0
mvn clean test -P spring-boot-3.2

# 4. 测试不同 Java 版本
mvn clean test -P java-11
mvn clean test -P java-17

# 5. 打包源代码和文档
mvn clean source:jar javadoc:jar

# 6. 发布到 Maven Central（如果已配置）
mvn clean deploy -P release
```

---

## ⚠️ 注意事项

### Spring Boot 2.6 的限制
- Spring Boot 2.6.0 引入了新的配置验证规则
- 建议最低使用 **2.6.6** 版本以获得更好的稳定性
- 低于 2.6.6 的版本可能存在已知问题

### Spring Boot 3.x 的变更
- 需要 **Java 17+** 运行环境
- Jedis 版本升级到 **4.4.3**
- 部分包结构可能发生变化，但本库已处理兼容

### Spring Boot 4.x 的预览
- 仅提供 **快照版本** 配置
- 正式版本发布后需要更新版本号
- 需要 **Java 21+** 和最新的 IDE 支持
- 需要在项目中配置 Spring snapshot 仓库

### Jedis 版本选择
- **Spring Boot 2.x** 使用 Jedis 3.9.0（支持 Java 8-17）
- **Spring Boot 3.x** 使用 Jedis 4.4.3（支持 Java 17+）
- **Spring Boot 4.x** 使用 Jedis 5.0.0（支持 Java 21+）

---

## 🔍 故障排除

### 问题 1：编译错误 "Unsupported class-file format"
```
错误示例：
  [ERROR] error: class file for java.lang.Record has wrong version 61.0, should be 55.0

解决方案：检查 JDK 版本是否与 Profile 匹配
# 查看当前 JDK 版本
java -version

# 示例：
- 如果使用 JDK 11，但想编译 Java 17 代码会出错
- 应该安装对应版本的 JDK 或使用 jenv 管理多个 JDK

# 使用 jenv 切换 JDK（如果已安装）
jenv global 17.0.0
mvn clean install -P spring-boot-3.0
```

### 问题 2：Spring Boot 依赖版本冲突
```
错误示例：
  [ERROR] Failed to execute goal ... dependency conflict

解决方案：在使用项目的 pom.xml 中使用正确的 dependencyManagement
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.18</version>  <!-- 与 crydis 保持一致 -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 问题 3：Jedis 连接失败
```
错误示例：
  redis.clients.jedis.exceptions.JedisConnectionException: Could not connect

解决方案：检查 Redis 服务
# 启动 Redis（Docker 示例）
docker run -d -p 6379:6379 redis:latest

# 或本地安装（macOS）
brew install redis
redis-server
```

### 问题 4：Jackson 反序列化错误
```
错误示例：
  com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException

解决方案：确保对象有无参构造器
@Data
public class User {
    private int id;
    private String name;
    
    // ✅ 需要无参构造器（Lombok 的 @Data 会自动生成）
    public User() {}
}
```

---

## 📚 完整命令示例

```bash
# 🔹 完整的开发工作流

# 1. 克隆项目
git clone https://github.com/Cikian/crydis.git
cd crydis

# 2. 编译并运行所有测试（默认 Spring Boot 2.7.x）
mvn clean test

# 3. 验证多版本兼容性
mvn clean test -P spring-boot-2.6
mvn clean test -P spring-boot-3.0
mvn clean test -P spring-boot-3.2

# 4. 验证多个 Java 版本编译
mvn clean test -P java-11
mvn clean test -P java-17
mvn clean test -P java-21

# 5. 生成源代码和文档
mvn clean source:jar javadoc:jar

# 6. 打包为 JAR
mvn clean package -DskipTests

# 7. 本地安装到 Maven 仓库
mvn clean install -DskipTests

# 8. 发布到 Maven Central（需要配置）
mvn clean deploy -P release
```

---

## 📞 支持与反馈

如有兼容性问题，请提交 Issue 并包含以下信息：

```markdown
### 环境信息
- Java 版本：java -version 的输出
- Spring Boot 版本：pom.xml 或 build.gradle 中的版本
- Maven 版本：mvn -version 的输出
- 使用场景：Spring 项目 / 非 Spring 项目

### 问题描述
详细的错误日志（如有）

### 复现步骤
1. ...
2. ...
3. ...
```

**项目主页**：https://github.com/Cikian/crydis
**作者网站**：https://www.cikian.cn
