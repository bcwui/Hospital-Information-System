# HIS 智慧医疗信息管理系统 - 后端 API 接口文档

## 一、接口响应格式

所有接口统一使用 `Result<T>` 封装返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 500 | 失败 |

---

## 二、认证模块 `/auth`

| 方法 | 路径 | 描述 | 角色 | 请求参数 |
|------|------|------|------|----------|
| GET | `/auth/email` | 发送邮箱验证码 | 公开 | `email` |
| GET | `/auth/IsExists` | 检查用户名/邮箱是否存在 | 公开 | `username`, `email` |
| POST | `/auth/register` | 用户注册 | 公开 | `UserRegisterDTO` JSON |
| POST | `/auth/login` | 用户登录 | 公开 | `UserLoginDTO` JSON |
| POST | `/auth/logout` | 退出登录 | 登录用户 | - |
| GET | `/auth/currentUser` | 获取当前登录用户信息 | 登录用户 | - |
| POST | `/auth/updatePassword` | 修改密码 | 登录用户 | `oldPassword`, `newPassword` |
| POST | `/auth/updateAvatar` | 上传/更新用户头像 | 登录用户 | `file` (multipart) |
| POST | `/auth/updateInfo` | 更新用户个人信息 | 登录用户 | `UserUpdateDTO` JSON |

---

## 三、预约挂号模块 `/appointment`

### 基础资源接口

| 方法 | 路径 | 描述 | 角色 | 请求参数 |
|------|------|------|------|----------|
| GET | `/appointment/departments` | 获取科室列表 | admin/patient | `onlyActive` (可选，默认true) |
| GET | `/appointment/clinics` | 获取门诊列表 | admin/patient | `deptId`, `onlyActive` |
| GET | `/appointment/clinics/search` | 通过名称搜索门诊 | admin/patient | `name`, `onlyActive` |
| GET | `/appointment/doctors` | 获取医生列表 | admin/patient | `deptId`, `name`, `clinicId` |
| GET | `/appointment/doctors/{doctorId}` | 获取医生详情 | admin/patient | - |

### 排班与预约接口

| 方法 | 路径 | 描述 | 角色 | 请求参数 |
|------|------|------|------|----------|
| POST | `/appointment/schedules` | 获取可用排班列表 | admin/patient | `ScheduleQuery` JSON |
| GET | `/appointment/schedules/{scheduleId}` | 获取排班详情 | admin/patient | - |
| POST | `/appointment/doctor-schedules` | 获取医生排班 | admin/patient | `ScheduleQuery` JSON |
| POST | `/appointment/create` | 创建预约挂号 | patient | `patientId`, `scheduleId`, `isRevisit` |
| POST | `/appointment/cancel` | 取消预约挂号 | patient | `appointmentId`, `patientId` |
| GET | `/appointment/patient/{patientId}` | 获取患者预约记录 | patient | `status` (可选) |
| GET | `/appointment/doctor/{doctorId}` | 获取医生预约记录 | doctor | `date`, `status` |
| GET | `/appointment/doctor/{doctorId}/appointment/{appointmentId}` | 医生查看挂号详情 | doctor | - |

### AI 问诊接口

| 方法 | 路径 | 描述 | 角色 | 请求参数 |
|------|------|------|------|----------|
| POST | `/appointment/ai-consult/stream` | 发起AI问诊（流式SSE） | patient | `AiConsultRequest` JSON |
| GET | `/appointment/ai-consult/latest` | 获取患者最近AI问诊会话 | patient | `patientId` |
| GET | `/appointment/ai-consult/sessions` | 获取患者所有AI问诊会话列表 | patient | `patientId` |
| GET | `/appointment/ai-consult/history` | 获取AI问诊历史会话 | patient | `sessionId` |

---

## 四、医疗服务模块 `/medical`

| 方法 | 路径 | 描述 | 角色 | 请求参数 |
|------|------|------|------|----------|
| GET | `/medical/doctor/profile` | 获取当前登录医生信息 | doctor | - |
| GET | `/medical/patient/{patientId}/diagnoses` | 获取患者诊断记录列表 | patient | - |
| GET | `/medical/doctor/{doctorId}/diagnoses` | 获取医生诊断记录列表 | doctor | - |
| GET | `/medical/diagnoses/{diagId}` | 获取诊断详情 | doctor/patient | - |
| POST | `/medical/diagnoses` | 创建诊断记录 | doctor | `DiagnosisDTO` JSON |
| GET | `/medical/appointment/{appointmentId}/diagnosis` | 根据预约ID获取诊断记录 | doctor/patient | - |

---

## 五、管理员模块 `/admin`

### 医生管理

| 方法 | 路径 | 描述 | 请求参数 |
|------|------|------|----------|
| POST | `/admin/doctor` | 创建医生 | `DoctorDTO` (multipart) + `avatarFile` (可选) |
| PUT | `/admin/doctor` | 更新医生 | `DoctorDTO` (multipart) + `avatarFile` (可选) |
| DELETE | `/admin/doctor/{doctorId}` | 删除医生 | - |
| GET | `/admin/doctor/{doctorId}` | 获取医生详情 | - |

### 排班管理

| 方法 | 路径 | 描述 | 请求参数 |
|------|------|------|----------|
| POST | `/admin/schedule` | 创建排班 | `Schedule` JSON |
| DELETE | `/admin/schedule/{scheduleId}` | 逻辑删除排班 | - |
| GET | `/admin/schedule/{scheduleId}` | 获取排班详情 | - |
| POST | `/admin/schedule/auto` | 执行自动排班 | `AutoScheduleRequest` JSON |
| GET | `/admin/schedule/status` | 获取排班状态 | `startDate`, `endDate` |

### 科室管理

| 方法 | 路径 | 描述 | 请求参数 |
|------|------|------|----------|
| POST | `/admin/department` | 创建科室 | `Department` JSON |
| PUT | `/admin/department` | 更新科室 | `Department` JSON |
| DELETE | `/admin/department/{deptId}` | 逻辑删除科室 | - |
| PUT | `/admin/department/restore/{deptId}` | 恢复科室 | - |
| DELETE | `/admin/department/physical/{deptId}` | 物理删除科室 | - |
| GET | `/admin/department/{deptId}` | 获取科室详情 | - |

### 门诊管理

| 方法 | 路径 | 描述 | 请求参数 |
|------|------|------|----------|
| POST | `/admin/clinic` | 创建门诊 | `Clinic` JSON |
| PUT | `/admin/clinic` | 更新门诊 | `Clinic` JSON |
| DELETE | `/admin/clinic/{clinicId}` | 逻辑删除门诊 | - |
| PUT | `/admin/clinic/restore/{clinicId}` | 恢复门诊 | - |
| DELETE | `/admin/clinic/physical/{clinicId}` | 物理删除门诊 | - |
| GET | `/admin/clinic/{clinicId}` | 获取门诊详情 | - |

---

## 六、AI RAG 管理模块 `/ai/rag`

| 方法 | 路径 | 描述 | 角色 | 请求参数 |
|------|------|------|----------|
| POST | `/ai/rag/documents` | 文档入库 | admin | `AiRagAddRequest` JSON |
| POST | `/ai/rag/upload-pdf` | PDF文档上传并入库 | admin | `file` (multipart) |
| POST | `/ai/rag/search` | 相似检索 | admin | `AiRagSearchRequest` JSON |

---

## 七、主要数据模型

### UserVO（用户信息）

```json
{
  "userId": 1,
  "patientId": 1,
  "doctorId": null,
  "title": "主任医师",
  "introduction": "...",
  "clinicId": 1,
  "clinicName": "内科门诊",
  "deptId": 1,
  "deptName": "内科",
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "avatar": "http://..."
}
```

### DoctorVO（医生信息）

```json
{
  "doctorId": 1,
  "userId": 1,
  "name": "张医生",
  "clinicId": 1,
  "deptName": "内科",
  "title": "主任医师",
  "introduction": "...",
  "avatar": "http://...",
  "createTime": "2025-01-01T10:00:00"
}
```

### AppointmentVO（预约信息）

```json
{
  "appointmentId": 1,
  "patientId": 1,
  "patientName": "李四",
  "doctorId": 1,
  "doctorName": "张医生",
  "deptId": 1,
  "deptName": "内科",
  "clinicId": 1,
  "clinicName": "内科门诊",
  "scheduleId": 1,
  "appointmentDate": "2025-05-15",
  "timeSlot": "09:00-09:30",
  "status": 0,
  "isRevisit": 0
}
```

### ScheduleQuery（排班查询条件）

```json
{
  "deptId": 1,
  "clinicId": 1,
  "doctorId": 1,
  "title": "主任医师",
  "startDate": "2025-05-13",
  "endDate": "2025-05-20"
}
```

### DiagnosisVO（诊断记录）

```json
{
  "diagId": 1,
  "patientId": 1,
  "patientName": "李四",
  "doctorId": 1,
  "doctorName": "张医生",
  "appointmentId": 1,
  "diagnosis": "诊断描述",
  "prescription": "处方信息",
  "advice": "医嘱建议",
  "createTime": "2025-05-13T10:00:00"
}
```

---

## 八、角色说明

| 角色 | 值 | 说明 |
|------|-----|------|
| admin | `"admin"` | 管理员 |
| doctor | `"doctor"` | 医生 |
| patient | `"patient"` | 患者 |

---

## 九、预约状态说明

| 状态值 | 说明 |
|--------|------|
| 0 | 待就诊 |
| 1 | 已完成 |
| 2 | 已取消 |
| 3 | 已过期 |

---

## 十、接口模块汇总

| 模块 | 路径前缀 | 主要功能 |
|------|----------|----------|
| 认证模块 | `/auth` | 登录注册、用户信息、头像 |
| 预约挂号模块 | `/appointment` | 科室、门诊、医生、排班、预约、AI问诊 |
| 医疗服务模块 | `/medical` | 诊断记录管理 |
| 管理员模块 | `/admin` | 医生、科室、门诊、排班管理 |
| AI RAG 模块 | `/ai/rag` | 文档检索管理 |
