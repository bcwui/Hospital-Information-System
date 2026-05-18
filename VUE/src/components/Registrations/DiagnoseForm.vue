<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { creatRegistrationDiagnoses } from '@/api/doctor/registrations'

interface MedicalForm {
  diagnosisResult: string
  examination: string
  prescription: string
  advice: string
}

const props = defineProps({
  appointmentId: {
    type: String,
    required: true
  },
  doctorId: {
    type: String,
    required: true
  },
  patientId: {
    type: String,
    required: true
  }
})

const formData = reactive<MedicalForm>({
  diagnosisResult: '',
  examination: '',
  prescription: '',
  advice: ''
})

const rules = reactive<FormRules<MedicalForm>>({
  diagnosisResult: [
    { required: true, message: '诊断结果不能为空', trigger: 'blur' },
    { min: 10, message: '至少需要10个字符的详细描述', trigger: 'blur' }
  ],
  prescription: [
    { required: true, message: '处方内容不能为空', trigger: 'blur' },
    { min: 20, message: '请完整填写药品信息和使用说明', trigger: 'blur' }
  ],
  examination: [
    { max: 200, message: '检查项目不宜超过200字', trigger: 'blur' }
  ],
  advice: [
    { max: 150, message: '建议内容不宜超过150字', trigger: 'blur' }
  ]
})

const formRef = ref<FormInstance>()

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    // 此处添加提交逻辑
    creatRegistrationDiagnoses(
      props.appointmentId,
      props.doctorId,
      props.patientId,
      formData.diagnosisResult,
      formData.examination,
      formData.prescription,
      formData.advice,
    ).then(() => {
      ElMessage.success('病历提交成功')
    }).catch(() => {
      ElMessage.error('病历提交失败，请稍后重试')
    })
    resetForm()
  } catch {
    ElMessage.error('请正确填写所有必填项')
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
}
</script>


<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" style="max-width: 800px">
    <el-form-item label="诊断结果" prop="diagnosisResult">
      <el-input v-model="formData.diagnosisResult" type="textarea" :autosize="{ minRows: 3 }" placeholder="请输入详细诊断结论" />
    </el-form-item>

    <el-form-item label="检查项目" prop="examination">
      <el-input v-model="formData.examination" type="textarea" :autosize="{ minRows: 2 }" placeholder="列出需要进行的检查项目" />
    </el-form-item>

    <el-form-item label="处方" prop="prescription">
      <el-input v-model="formData.prescription" type="textarea" :autosize="{ minRows: 4 }"
        placeholder="详细填写药品名称、剂量和使用方法" />
    </el-form-item>

    <el-form-item label="医嘱建议" prop="advice">
      <el-input v-model="formData.advice" type="textarea" :autosize="{ minRows: 2 }" placeholder="填写康复建议和注意事项" />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="handleSubmit">提交病历</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.el-textarea {
  width: 100%;
}

.el-form-item:last-child {
  margin-top: 24px;
}
</style>
