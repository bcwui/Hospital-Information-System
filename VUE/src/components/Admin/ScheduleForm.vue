<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" style="max-width: 800px">
    <el-row :gutter="20">
      <!-- 左列 -->
      <el-col :span="12">
        <el-form-item label="排班日期" prop="scheduleDate">
          <el-date-picker v-model="formData.scheduleDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD"
            style="width: 100%" />
        </el-form-item>

        <el-form-item label="时间段" prop="timeSlot">
          <div class="time-range-picker">
            <el-time-select v-model="startTime" placeholder="开始时间" :max-time="endTime" format="HH:mm" />
            <span class="time-separator">至</span>
            <el-time-select v-model="endTime" placeholder="结束时间" :min-time="startTime" format="HH:mm" />
          </div>
        </el-form-item>
      </el-col>

      <!-- 右列 -->
      <el-col :span="12">
        <el-form-item label="最大人数" prop="maxPatients">
          <el-input-number v-model="formData.maxPatients" :min="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="当前人数" prop="currentPatients">
          <el-input-number v-model="formData.currentPatients" :min="0" :max="formData.maxPatients"
            controls-position="right" />
        </el-form-item>

        <el-form-item label="有效状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">有效</el-radio>
            <el-radio :label="0">无效</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item>
      <el-button type="primary" @click="handleSubmit">保存设置</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref, watch, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useHospitalStore } from '@/stores/hospitalData'
import { useRoute } from 'vue-router'

interface ScheduleForm {
  scheduleDate: string
  timeSlot: string
  maxPatients: number
  currentPatients: number
  status: number
}

const route = useRoute()
const hospitalStore = useHospitalStore()


const emits = defineEmits(['submit'])
const props = defineProps({
  optionType: {
    type: String,
    required: true
  },
  doctorId: {
    type: String,
    required: true
  },
  clinicId: {
    type: String,
    required: true
  },
  scheduleId: {
    type: String,
    required: false
  }
})

const formData = reactive<ScheduleForm>({
  scheduleDate: '',
  timeSlot: '',
  maxPatients: 1,
  currentPatients: 0,
  status: 1
})

// 时间范围处理
const startTime = ref('')
const endTime = ref('')
watch([startTime, endTime], () => {
  formData.timeSlot = `${startTime.value}-${endTime.value}`
})

// 验证规则
const rules = reactive<FormRules<ScheduleForm>>({
  scheduleDate: [
    { required: true, message: '请选择排班日期', trigger: 'change' }
  ],
  timeSlot: [
    {
      validator: (_, __, callback) => {
        if (!startTime.value || !endTime.value) {
          callback(new Error('请选择完整时间段'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  maxPatients: [
    { required: true, message: '请输入最大人数', trigger: 'blur' },
    { type: 'number', min: 1, message: '至少1人', trigger: 'blur' }
  ],
  currentPatients: [
    { required: true, message: '请输入当前人数', trigger: 'blur' },
    {
      type: 'number',
      validator: (_, value, callback) => {
        if (value > formData.maxPatients) {
          callback(new Error('不能超过最大人数'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ],
  status: [
    { required: true, message: '请选择有效状态', trigger: 'change' }
  ]
})

const formRef = ref<FormInstance>()
const toQueryString = (value: unknown) => (typeof value === 'string' ? value : '')
const doctorName = computed(() => toQueryString(route.query.name))
const doctorTitle = computed(() => toQueryString(route.query.title))
const doctorIntroduction = computed(() => toQueryString(route.query.introduction))
const doctorAvatar = computed(() => toQueryString(route.query.avatar))
const getScheduleWindow = () => {
  const currentDate = new Date();
  const sevenDaysLaterDate = new Date(currentDate.getTime() + 7 * 86400000);
  const formatDate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  return {
    startDate: formatDate(currentDate),
    endDate: formatDate(sevenDaysLaterDate)
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  startTime.value = ''
  endTime.value = ''
  formData.maxPatients = 1
  formData.currentPatients = 0
  formData.status = 1
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()

    // 构造最终数据
    const submitData = {
      ...formData,
      maxPatients: Number(formData.maxPatients),
      currentPatients: Number(formData.currentPatients)
    }
    if (valid) {
      if (props.optionType === 'create') {
        const res = await hospitalStore.createSchedule(
          props.doctorId,
          props.clinicId,
          submitData.scheduleDate,
          submitData.timeSlot,
          submitData.maxPatients,
          submitData.currentPatients,
          submitData.status,
          doctorName.value,
          doctorTitle.value,
          doctorIntroduction.value,
          doctorAvatar.value,
          {
            title: doctorTitle.value,
            startDate: getScheduleWindow().startDate,
            endDate: getScheduleWindow().endDate,
          }
        )
        if (res) {
          ElMessage.success('排班设置保存成功')
          resetForm()
        }
      } else if (props.optionType === 'update' && props.scheduleId) {
        emits('submit', submitData)
      }
    }
  } catch {
    ElMessage.error('请完善必填项')
  }
}
</script>

<style scoped>
.time-range-picker {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-separator {
  color: #666;
  padding: 0 5px;
}

.el-input-number {
  width: 100%;
}

.el-radio-group {
  margin-top: 5px;
}
</style>
