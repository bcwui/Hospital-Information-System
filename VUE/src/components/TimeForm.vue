<template>
  <el-form ref="formRef" :model="dateForm" :rules="rules" label-width="120px" @submit.prevent="handleSubmit">
    <!-- 开始时间 -->
    <el-form-item label="起始时间" prop="startDate">
      <el-date-picker v-model="dateForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" />
    </el-form-item>

    <!-- 结束时间 -->
    <el-form-item label="结束时间" prop="endDate">
      <el-date-picker v-model="dateForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" />
    </el-form-item>

    <el-form-item class="submit-button">
      <el-button type="primary" native-type="submit">
        提交表单
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

const props = defineProps({
  optionType: {
    type: String,
    default: 'autoUpdate'
  }
})

const emits = defineEmits(["autoUpdate"])

// 表单引用（用于手动触发验证）
const formRef = ref<FormInstance>()

// 响应式表单数据
const dateForm = reactive({
  startDate: '',
  endDate: ''
})

// 生成初始日期范围
const initDates = generateDateRange()
dateForm.startDate = initDates.startDate
dateForm.endDate = initDates.endDate

// 开始日期验证器
const validateStartDate = (_: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) return callback(new Error('开始日期不能为空'))

  if (dateForm.endDate && value > dateForm.endDate) {
    callback(new Error('开始日期不能晚于结束日期'))
  } else {
    callback()
  }
}

// 结束日期验证器
const validateEndDate = (_: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) return callback(new Error('结束日期不能为空'))

  if (dateForm.startDate && value < dateForm.startDate) {
    callback(new Error('结束日期不能早于开始日期'))
  } else {
    callback()
  }
}

// 验证规则
// 需要出现在验证器定义之后
const rules = reactive<FormRules>({
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' },
    { validator: validateStartDate, trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
    { validator: validateEndDate, trigger: 'change' }
  ]
})

// 日期生成工具函数（保持原功能）
function generateDateRange() {
  const now = new Date()
  const sevenDaysLater = new Date(now)
  sevenDaysLater.setDate(now.getDate() + 7)

  return {
    startDate: formatDate(now),
    endDate: formatDate(sevenDaysLater)
  }
}

// 日期格式化函数
function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 提交处理函数
const handleSubmit = async () => {
  try {
    // 执行表单验证
    await formRef.value?.validate()

    // 提交逻辑
    switch (props.optionType) {
      case "autoUpdate":
        emits("autoUpdate", dateForm.startDate, dateForm.endDate)
        break;

      default:
        ElMessage.error("意外操作")
        break;
    }
  } catch {
    ElMessage.error({
      message: '请正确填写表单内容',
      duration: 2000
    })
  }
}
</script>

<style scoped>
.el-form {

  .el-form-item {
    margin: 40px 0;
  }

  .submit-button {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
