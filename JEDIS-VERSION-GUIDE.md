# Jedis 版本选择指南

## Jedis 版本与 Java 版本兼容性矩阵

| Jedis 版本 | 最低 Java 版本 | 推荐 Java 版本 | Spring Boot 支持 | 状态 | 推荐场景 |
|-----------|----------------|---------------|-----------------|------|---------|
| **3.0.x** | Java 7 | Java 8 | 1.5.x - 2.3.x | 🔴 维护中 | 遗留系统 |
| **3.1.x** | Java 8 | Java 8 | 2.0.x - 2.3.x | 🔴 维护中 | 遗留系统 |
| **3.2.x** | Java 8 | Java 8 | 2.4.x - 2.5.x | 🟡 安全修复 | 标准系统 |
| **3.3.x** | Java 8 | Java 8 | 2.5.x - 2.6.x | 🟡 安全修复 | 标准系统 |
| **3.4.x** | Java 8 | Java 8 | 2.6.x | 🟢 活跃 | 稳定系统 |
| **3.5.x** | Java 8 | Java 8 | 2.6.x - 2.7.x | 🟢 活跃 | 稳定系统 |
| **3.6.x** | Java 8 | Java 8-11 | 2.7.x | 🟢 活跃 | 推荐使用 |
| **3.7.x** | Java 8 | Java 8-11 | 2.7.x | 🟢 活跃 | 推荐使用 |
| **3.8.x** | Java 8 | Java 8-17 | 2.7.x | 🟢 活跃 | 推荐使用 |
| **3.9.x** | Java 8 | Java 8-17 | 2.7.x | 🟢 最新稳定 | ⭐ **强烈推荐** |
| **4.0.x** | Java 9 | Java 11-17 | - | 🟢 活跃 | Java 11+ |
| **4.1.x** | Java 9 | Java 11-17 | 3.0.x | 🟢 活跃 | Java 11+ |
| **4.2.x** | Java 9 | Java 11-17 | 3.0.x - 3.1.x | 🟢 活跃 | Java 11+ |
| **4.3.x** | Java 9 | Java 11-17 | 3.1.x - 3.2.x | 🟢 活跃 | Java 11+ |
| **4.4.x** | Java 9 | Java 11-21 | 3.2.x - 3.3.x | 🟢 最新稳定 | ⭐ **Spring Boot 3.x 推荐** |
| **5.0.x** | Java 17 | Java 17-21 | 3.4.x+ | 🟢 开发中 | Java 17+ |

---

## 推荐版本（2024年）

### 🎯 非 Spring 项目推荐

| 你的 Java 版本 | 推荐 Jedis 版本 | Maven 依赖 |
|---------------|----------------|-----------|
| **Java 8** | **3.9.0** | `<version>3.9.0</version>` |
| **Java 9-11** | **3.9.0** 或 **4.4.3** | `<version>3.9.0</version>` 或 `<version>4.4.3</version>` |
| **Java 12-16** | **3.9.0** 或 **4.4.3** | `<version>4.4.3</version>` |
| **Java 17** | **4.4.3** | `<version>4.4.3</version>` |
| **Java 18-21** | **4.4.3** | `<version>4.4.3</version>` |
| **Java 22-25** | **4.4.3** 或 **5.x** | `<version>4.4.3</version>` |

---

## 非 Spring 项目使用示例

### ✅ 方式一：使用 Jedis 3.9.0（Java 8 环境）

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>cn.cikian</groupId>
        <artifactId>crydis</artifactId>
        <version>0.0.1</version>
    </dependency>

    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>3.9.0</version>
    </dependency>
</dependencies>
```

```java
public class Main {
    public static void main(String[] args) {
        // 初始化
        CrydisManager.builder()
            .host("127.0.0.1")
            .port(6379)
            .init();

        // 使用
        Crydis.set("key", "value");
        String value = Crydis.get("key");

        // 销毁
        CrydisManager.destroy();
    }
}
```

### ✅ 方式二：使用 Jedis 4.4.3（Java 17+ 环境）

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>cn.cikian</groupId>
        <artifactId>crydis</artifactId>
        <version>0.0.1</version>
    </dependency>

    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>4.4.3</version>
    </dependency>
</dependencies>
```

```java
public class Main {
    public static void main(String[] args) {
        // 初始化
        CrydisManager.builder()
            .host("127.0.0.1")
            .port(6379)
            .init();

        // 使用
        Crydis.set("key", "value");
        String value = Crydis.get("key");

        // 销毁
        CrydisManager.destroy();
    }
}
```

---

## 版本选择决策树

```
你的 Java 版本是什么？
│
├─ Java 8
│  └─ 推荐: Jedis 3.9.0 ✅
│
├─ Java 9-11
│  ├─ 需要 Jedis 4.x 新特性? 
│  │  ├─ 是 → 推荐: Jedis 4.4.3 ✅
│  │  └─ 否 → 推荐: Jedis 3.9.0 ✅
│
├─ Java 12-16
│  └─ 推荐: Jedis 4.4.3 ✅
│
└─ Java 17+
   └─ 推荐: Jedis 4.4.3 ✅
```

---

## 版本对比：Jedis 3.x vs 4.x

### 功能差异

| 特性 | Jedis 3.x | Jedis 4.x | 说明 |
|------|----------|----------|------|
| Pipeline API | ✅ 支持 | ✅ 支持 | - |
| 事务支持 | ✅ 支持 | ✅ 支持 | - |
| Cluster 支持 | ✅ 支持 | ✅ 支持 | - |
| Sentinel 支持 | ✅ 支持 | ✅ 支持 | - |
| 响应式编程 | ❌ 不支持 | ✅ 支持 | Jedis 4.x 新增 |
| Lua 脚本优化 | 基本 | ✅ 增强 | 4.x 性能更好 |
| 连接池重构 | 基本 | ✅ 重构 | 4.x 更高效 |
| SSL/TLS 支持 | 基本 | ✅ 增强 | 4.x 更好支持 |
| IPv6 支持 | ✅ 支持 | ✅ 支持 | - |
| 命令管道优化 | 基本 | ✅ 优化 | 4.x 更快 |

### 性能对比

| 指标 | Jedis 3.x | Jedis 4.x | 提升 |
|------|----------|----------|------|
| 普通操作 | 基准 | ~10-15% | 提升 |
| Pipeline 操作 | 基准 | ~20-30% | 显著提升 |
| 连接获取 | 基准 | ~5-10% | 提升 |
| 内存使用 | 基准 | ~10-15% | 降低 |

---

## Spring Boot 项目的版本管理

Spring Boot 项目**无需手动管理 Jedis 版本**，它会自动管理！

| Spring Boot 版本 | 自动管理的 Jedis 版本 | Java 要求 |
|----------------|---------------------|---------|
| 1.5.x - 2.1.x | 2.x | Java 8 |
| 2.2.x - 2.4.x | 3.x | Java 8 |
| 2.5.x - 2.7.x | 3.x | Java 8-17 |
| 3.0.x - 3.3.x | 4.x | Java 17+ |

---

## 常见问题

### ❓ 如何检查当前使用的 Jedis 版本？

```bash
# Maven 项目
mvn dependency:tree | grep jedis
```

### ❓ Jedis 3.x 代码能在 4.x 运行吗？

**大部分可以！** Crydis 已经做好了兼容性处理，两种版本都可以直接使用 `Crydis.xxx()` 静态方法。

### ❓ 出现 `NoSuchMethodError` 怎么办？

通常是 **运行时 Jedis 版本与编译时不一致**。解决方案：

1. **统一版本**：在 pom.xml 中明确指定 Jedis 版本
2. **排除冲突**：如果其他依赖引入了不同版本的 Jedis，使用 `<exclusions>` 排除

```xml
<dependency>
    <groupId>xxx</groupId>
    <artifactId>some-library</artifactId>
    <exclusions>
        <exclusion>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## 推荐实践

### 1. 生产环境
- ✅ 使用经过充分测试的稳定版本（3.9.0 或 4.4.3）
- ✅ 通过 Maven BOM 或 parent POM 管理版本
- ✅ 在测试环境中验证后再升级

### 2. 开发环境
- ✅ 使用最新稳定版本获取最新特性
- ✅ 关注 Jedis 的 Release Notes
- ✅ 定期更新以获取安全修复

### 3. 迁移建议
- ✅ 从 Jedis 3.x 迁移到 4.x 通常是无缝的
- ✅ 先在测试环境验证
- ✅ 检查是否使用了已废弃的 API

---

## 官方资源

- **Jedis GitHub**: https://github.com/redis/jedis
- **Jedis 文档**: https://www.jedis.dev/
- **Redis 命令参考**: https://redis.io/commands/
