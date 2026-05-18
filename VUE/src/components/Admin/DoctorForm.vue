<script setup lang="ts">
import { useHospitalStore } from '@/stores/hospitalData'
import { useRoute } from 'vue-router'
import { reactive, ref, computed, type Ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import defaultAvatarUrl from "@/assets/doctor.png"

const defaultAvatar: Ref<File | null> = ref(null)
const hospitalStore = useHospitalStore()
const route = useRoute()
const emit = defineEmits<{
  (e: 'success'): void;
}>()
const props = defineProps({
  optionType: {
    type: String,
    required: true
  },
  doctorId: {
    type: String,
    required: false,
  },
  userId: {
    type: String,
    required: false,
  },
  initial: {
    type: Object as () =>
      | {
        name?: string
        title?: string
        introduction?: string
        avatarUrl?: string
      }
      | undefined,
    required: false,
  }
})

// 表单数据类型
interface FormData {
  username: string
  password: string
  email: string
  phone: string
  name: string
  title: string
  introduction: string
  avatar: File | null
}

// 表单数据
const formData = reactive<FormData>({
  username: '',
  password: '',
  email: '',
  phone: '',
  name: '',
  title: '',
  introduction: '',
  avatar: defaultAvatar.value,
})

// 预览用户头像
const newAvatar = ref<string>(defaultAvatarUrl)

// 表单验证规则
const isUpdate = computed(() => props.optionType === 'update')
const rules = computed<FormRules<FormData>>(() => {
  const base: FormRules<FormData> = {
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    title: [{ required: true, message: '请输入职称', trigger: 'blur' }],
    introduction: [
      { required: true, message: '请输入个人简介', trigger: 'blur' },
      { max: 500, message: '最多输入500个字符', trigger: 'blur' },
    ],
  }

  if (!isUpdate.value) {
    // create：账号字段强校验
    return {
      ...base,
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, max: 16, message: '长度在2到16个字符', trigger: 'blur' },
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在6到20个字符', trigger: 'blur' },
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change'] },
      ],
      phone: [
        { required: true, message: '请输入联系电话', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' },
      ],
      avatar: [{ required: true, message: '请上传头像', trigger: 'change' }],
    }
  }

  // update：账号字段不强制（后端若支持“空值不更新/不传不更新”，可只改医生档案字段）
  return {
    ...base,
    email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change'] }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }],
    password: [{ min: 0, max: 20, message: '密码长度最多20个字符', trigger: 'blur' }],
  }
})

const formRef = ref<FormInstance>()
const getQueryString = (value: unknown): string => {
  if (Array.isArray(value)) {
    const [first] = value;
    return typeof first === 'string' ? first : '';
  }
  return typeof value === 'string' ? value : '';
}

const clinicId = computed(() => getQueryString(route.query.clinicId))
const departmentId = computed(() => getQueryString(route.query.departmentId))

const applyInitial = () => {
  if (!isUpdate.value) return
  if (props.initial?.name != null) formData.name = props.initial.name
  if (props.initial?.title != null) formData.title = props.initial.title
  if (props.initial?.introduction != null) formData.introduction = props.initial.introduction
  if (props.initial?.avatarUrl) newAvatar.value = props.initial.avatarUrl
  // update 模式下不强制要求重新选择文件
  formData.avatar = null
  formRef.value?.clearValidate()
}

watch(
  () => [props.optionType, props.initial, props.doctorId, props.userId] as const,
  () => applyInitial(),
  { immediate: true }
)

// 提交处理
const handleSubmit = async () => {
  try {
    if (!formRef.value) return
    const valid = await formRef.value.validate()
    if (!valid) return

    // 这里处理提交逻辑
    if (!clinicId.value) {
      ElMessage.error('缺少门诊信息，无法创建医生');
      return;
    }
    if (!departmentId.value) {
      ElMessage.error('缺少科室信息，无法创建医生');
      return;
    }
    if (props.optionType === "create") {
      if (!formData.avatar) {
        ElMessage.error('请先上传头像');
        return;
      }
      const res = await hospitalStore.createDoctor(
        formData.username,
        formData.password,
        formData.email,
        formData.phone,
        departmentId.value,
        clinicId.value,
        formData.name,
        formData.title,
        formData.introduction,
        formData.avatar,
        URL.createObjectURL(formData.avatar)
      )
      if (res) {
        ElMessage({
          message: '添加医生成功',
          type: 'success',
        })
        emit('success')
      }
      return
    }
    else if (props.optionType === "update") {
      if (!props.doctorId || !props.userId) {
        ElMessage({
          message: '医生ID与用户ID不能为空',
          type: 'error',
        })
        return
      }
      if (!departmentId.value) {
        ElMessage({
          message: '缺少科室信息，无法更新医生',
          type: 'error',
        })
        return
      }
      if (!clinicId.value) {
        ElMessage({
          message: '缺少门诊信息，无法更新医生',
          type: 'error',
        })
        return
      }

      // update：如果未选择新头像，则传 null（后端应保留旧头像）
      const res = await hospitalStore.updateDoctor(
        props.doctorId,
        props.userId,
        formData.username,
        formData.password,
        formData.email,
        formData.phone,
        departmentId.value,
        clinicId.value,
        formData.name,
        formData.title,
        formData.introduction,
        formData.avatar,
        formData.avatar ? URL.createObjectURL(formData.avatar) : undefined
      )
      if (res) {
        ElMessage({
          message: '更新医生信息成功',
          type: 'success',
        })
        emit('success')
      }
      return
    }
  } catch (e) {
    // DoAxiosWithErro 已经会弹错误提示，这里主要是避免 Vue 报 Unhandled error
    console.error('DoctorForm handleSubmit error', e)
  }
}

const handleUP = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
    ElMessage.error('不支持该文件类型，请选择 JPEG、PNG 格式的图片');
    return;
  }

  // 文件大小检查（这里假设最大文件大小为 2MB）
  const maxSize = 2 * 1024 * 1024; // 2MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小超过限制，请选择小于 2MB 的图片');
    return;
  }
  newAvatar.value = URL.createObjectURL(file);
  formData.avatar = file;
}
</script>


<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" style="max-width: 800px">
    <el-row :gutter="20">
      <!-- 左列 -->
      <el-col :span="12">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :placeholder="isUpdate ? '留空表示不修改' : ''" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password :placeholder="isUpdate ? '不修改请留空' : ''" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" type="email" :placeholder="isUpdate ? '留空表示不修改' : ''" />
        </el-form-item>

      </el-col>

      <!-- 右列 -->
      <el-col :span="12">
        <el-form-item label="头像" prop="avatar">
          <div id="avatar">
            <input type="file" id="file" @change="handleUP" style="display: none;" />
            <el-avatar :src="newAvatar" :size="160" style="background-color: #fff;"></el-avatar>
            <label for="file" id="avatar-btn">修改头像</label>
            <!-- <el-button type="success" size="small" v-if="newAvatar" @click="uploadAvatar"
              style="border: 1px #fff solid; width: 68px; border-radius: 5px; margin: auto; margin-top: 15px;">
              保存修改
            </el-button> -->
          </div>
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>

        <el-form-item label="职称" prop="title">
          <el-input v-model="formData.title" />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" :placeholder="isUpdate ? '留空表示不修改' : ''" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 单独占满一行 -->
    <el-form-item label="个人简介" prop="introduction">
      <el-input v-model="formData.introduction" type="textarea" :autosize="{ minRows: 3 }" />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="handleSubmit">提交</el-button>
    </el-form-item>
  </el-form>
</template>


<style scoped lang="scss">
#avatar-btn {
  cursor: pointer;
  margin: auto;
  margin-top: 15px;
  padding: 5px 10px;
  background-color: #409eff;
  color: #fff;
  line-height: 30px;
  border-radius: 8px;
  border: #fff 1px solid;
  text-align: center;
  font-weight: bolder;
  font-size: 14px;

  &:hover {
    background-color: #66b1ff;
  }
}
</style>
