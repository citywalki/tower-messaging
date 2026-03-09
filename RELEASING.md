# 发布指南

本项目使用 [JReleaser](https://jreleaser.org/) 发布到 Maven Central。

## 环境变量配置

发布前需要配置以下环境变量（或 `~/.gradle/gradle.properties`）：

| 变量名 | 说明 | 获取方式 |
|--------|------|----------|
| `JRELEASER_MAVENCENTRAL_USERNAME` | Maven Central 用户名 | https://central.sonatype.com/ |
| `JRELEASER_MAVENCENTRAL_PASSWORD` | Maven Central 密码/Token | https://central.sonatype.com/ |
| `JRELEASER_GPG_PUBLIC_KEY` | GPG 公钥 (armored) | `gpg --export --armor <key-id>` |
| `JRELEASER_GPG_SECRET_KEY` | GPG 私钥 (armored) | `gpg --export-secret-keys --armor <key-id>` |
| `JRELEASER_GPG_PASSPHRASE` | GPG 密钥密码 | 创建密钥时设置的密码 |

## 发布步骤

### 1. 准备发布

确保所有测试通过：

```bash
./gradlew clean build
```

### 2. 配置版本

更新 `gradle.properties` 中的版本号（去掉 `-SNAPSHOT`）：

```properties
version=1.4.0
```

### 3. 发布到 Maven Central

```bash
./gradlew jreleaserFullRelease
```

## 常用命令

```bash
# 检查 JReleaser 配置
./gradlew jreleaserConfig

# 仅签名和部署（不创建 release）
./gradlew jreleaserDeploy

# 完整发布流程（签名、部署、发布）
./gradlew jreleaserFullRelease

# 发布到本地 Maven 仓库（测试用）
./gradlew publishToMavenLocal
```

## 故障排查

### GPG 签名问题

检查 GPG 密钥：

```bash
gpg --list-keys
gpg --list-secret-keys
```

导出密钥：

```bash
# 公钥
gpg --export --armor <key-id>

# 私钥
gpg --export-secret-keys --armor <key-id>
```

### Maven Central 认证

确保在 https://central.sonatype.com/ 注册并拥有 Namespace 权限。

### 调试模式

```bash
./gradlew jreleaserConfig --stacktrace --info
```

### SNAPSHOT 版本

JReleaser 不会将 SNAPSHOT 版本发布到 Maven Central。如需测试发布流程，请使用 release 版本。
