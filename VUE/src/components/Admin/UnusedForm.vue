<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="140px" style="max-width: 1200px">
    <el-row :gutter="30">
      <!-- 左列：医生信息 -->
      <el-col :span="12">
        <el-form-item label="医生姓名" prop="doctorName">
          <el-input v-model="formData.doctorName" />
        </el-form-item>

        <el-form-item label="医生职称" prop="doctorTitle">
          <el-input v-model="formData.doctorTitle" />
        </el-form-item>

        <el-form-item label="医生头像" prop="doctorAvatar">
          <el-upload v-model:file-list="fileList" action="#" :auto-upload="false" :on-change="handleAvatarChange"
            accept="image/*" list-type="picture-card">
            <el-icon>
              <Plus />
            </el-icon>
          </el-upload>
        </el-form-item>
      </el-col>

      <!-- 右列：排班信息 -->
      <el-col :span="12">
        <el-form-item label="排班日期" prop="scheduleDate">
          <el-date-picker v-model="formData.scheduleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>

        <el-form-item label="时间段" prop="timeSlot">
          <el-time-select v-model="startTime" placeholder="开始时间" :max-time="endTime" format="HH:mm" />
          <el-time-select v-model="endTime" placeholder="结束时间" :min-time="startTime" format="HH:mm"
            style="margin-left: 10px" />
        </el-form-item>

        <el-form-item label="剩余名额" prop="remainingQuota">
          <el-input-number v-model="formData.remainingQuota" :min="0" :max="100" />
        </el-form-item>

        <el-form-item label="可预约状态" prop="canBook">
          <el-switch v-model="formData.canBook" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 跨列字段 -->
    <el-form-item label="医生简介" prop="doctorIntroduction">
      <el-input v-model="formData.doctorIntroduction" type="textarea" :autosize="{ minRows: 3 }" />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="handleSubmit">保存排班</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

interface ScheduleForm {
  scheduleId: string | null
  doctorId: string | null
  doctorName: string
  doctorTitle: string
  doctorIntroduction: string
  doctorAvatar: File | string
  scheduleDate: string
  timeSlot: string
  remainingQuota: number
  canBook: boolean
}



const formData = reactive<ScheduleForm>({
  scheduleId: null,
  doctorId: null,
  doctorName: '',
  doctorTitle: '',
  doctorIntroduction: '',
  doctorAvatar: '',
  scheduleDate: '',
  timeSlot: '',
  remainingQuota: 0,
  canBook: true
})

const startTime = ref('')
const endTime = ref('')
const fileList = ref<UploadFile[]>([])

watch([startTime, endTime], () => {
  if (startTime.value && endTime.value) {
    formData.timeSlot = `${startTime.value}-${endTime.value}`
  }
})

const handleAvatarChange = (file: UploadFile) => {
  if (file.raw) {
    formData.doctorAvatar = file.raw
  }
}

const rules = reactive<FormRules<ScheduleForm>>({
  doctorName: [
    { required: true, message: '请输入医生姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度2-10个字符', trigger: 'blur' }
  ],
  doctorTitle: [
    { required: true, message: '请输入医生职称', trigger: 'blur' }
  ],
  scheduleDate: [
    { required: true, message: '请选择排班日期', trigger: 'change' }
  ],
  timeSlot: [
    {
      validator: (_, value, callback) => {
        if (!startTime.value || !endTime.value) {
          callback(new Error('请选择完整时间段'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  remainingQuota: [
    { required: true, message: '请输入剩余名额', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '范围0-100', trigger: 'blur' }
  ],
  doctorAvatar: [
    { required: true, message: '请上传医生头像', trigger: 'change' }
  ]
})

const formRef = ref<FormInstance>()

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const form = new FormData()
    for (const [key, value] of Object.entries(formData)) {
      if (key === 'doctorAvatar' && value instanceof File) {
        form.append(key, value)
      } else {
        form.append(key, String(value))
      }
    }

    console.log('排班数据:', Object.fromEntries(form))
    ElMessage.success('排班信息保存成功')
    resetForm()
  } catch {
    ElMessage.error('请完善表单信息')
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  fileList.value = []
  startTime.value = ''
  endTime.value = ''
}
</script>

<style scoped>
.el-date-editor,
.el-time-select {
  width: 100%;
}

.el-textarea {
  width: 100%;
}
</style>
