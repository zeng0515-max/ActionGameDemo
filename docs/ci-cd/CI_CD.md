# ActionGameDemo CI/CD

## CI

GitHub Actions 工作流：`.github/workflows/ci.yml`

触发时机：

- push 到 `master`、`main` 或 `codex/**`
- 创建 Pull Request

流程：

1. 检出代码
2. 安装 JDK 17
3. 缓存 Maven 依赖
4. 在 `server/` 目录执行 `mvn -B test`

## 后续 CD

CD 阶段可基于 Docker 镜像构建与推送，建议后续接入：

- `docker build` 多阶段构建
- 镜像版本打标签并推送到镜像仓库
- 部署到测试环境并执行健康检查
