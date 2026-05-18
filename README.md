# 智慧医疗信息管理系统（HIS）

基于 **Spring Boot 3 + Vue 3** 的全栈医院信息系统，支持患者、医生、管理员三种角色。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4、JDK 17 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.x、PostgreSQL + pgvector |
| 缓存 | Redis + Redisson |
| 鉴权 | Sa-Token + Redis |
| AI | 智谱 AI GLM-4.5（Spring AI）、SSE 流式响应 |
| 对象存储 | MinIO |
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |

## 功能模块

- **患者端**：在线预约挂号、AI 智能问诊、诊断记录查询
- **医生端**：排班管理、门诊出诊、患者诊断
- **管理端**：科室/门诊/医生 CRUD、排班调度、RAG 知识库管理

## 项目结构

```
├── HIS/                  # Spring Boot 后端
│   └── src/main/java/com/project/his/
│       ├── config/       # 配置（AI、Sa-Token、MinIO 等）
│       ├── controller/   # 接口层
│       ├── service/      # 业务逻辑层
│       ├── mapper/       # 数据访问层
│       └── domain/       # 实体、DTO、VO
├── VUE/                  # Vue 3 前端
│   └── src/
│       ├── views/        # 页面（admin/doctor/patient）
│       ├── components/   # 组件
│       ├── stores/       # Pinia 状态管理
│       ├── api/          # 接口请求
│       └── router/       # 路由
├── sql/                  # 数据库初始化脚本
└── api/                  # API 接口文档
```

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x、PostgreSQL（pgvector 扩展）、Redis
- MinIO（头像存储，可选）

### 后端

```bash
cd HIS
# 修改 src/main/resources/application.yml 中的数据库、Redis、AI 密钥等配置
mvn spring-boot:run
```

### 前端

```bash
cd VUE
npm install
npm run dev
```

## 配置说明

配置文件 `application.yml` 已加入 `.gitignore`，请复制模板并填入真实配置：

```bash
cp HIS/src/main/resources/application.example.yml HIS/src/main/resources/application.yml
```
