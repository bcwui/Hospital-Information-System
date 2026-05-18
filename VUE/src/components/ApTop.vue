<template>
  <div class="ap-top">
    <div id="patient">
      <span style="font-size: 22px; font-weight: bold;">{{ pageTitle }}部分</span>
      <el-button type="primary" @click="backpage" v-show="route.params.department">上一级</el-button>
      <div v-if="route.query.departmentId && !route.query.doctorId" id="searchInput"
        style="position: relative; bottom: 2.5px;">
        <el-input v-model="searchContent" style="width: 240px;height: 40px;"
          :placeholder="route.query.clinicId ? '在全部医生中搜索' : '在全部门诊中搜索'" :prefix-icon="Search"
          @keyup.enter="handleSearch" />
        <el-button type="primary" style="height: 40px;" @click="handleSearch">
          <el-icon size="large">
            <Search />
          </el-icon>
        </el-button>
      </div>
    </div>
    <div id="admin" v-if="effectiveCardType === 'admin'">
      <el-button type="success" @click="showAutoScheduleDialog" v-if="!departmentId">全部一键排班</el-button>
      <el-button type="primary" @click="createNewItem">添加</el-button>
    </div>
  </div>
  <el-dialog v-model="doctorDialogTableVisible" title="请填写医生信息" width="800">
    <DoctorForm :optionType="optionType" @success="doctorDialogTableVisible = false"></DoctorForm>
  </el-dialog>
  <el-dialog v-model="scheduleDialogTableVisible" title="请填写排班信息" width="800">
    <ScheduleForm :optionType="optionType" :clinicId="clinicId" :doctorId="doctorId">
    </ScheduleForm>
  </el-dialog>
  <el-dialog v-model="autoScheduleDialogVisible" title="全部一键排班" width="800">
    <TimeForm optionType="autoUpdate" @autoUpdate="handleAutoScheduleAll"></TimeForm>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import DoctorForm from '@/components/Admin/DoctorForm.vue';
import ScheduleForm from './Admin/ScheduleForm.vue';
import TimeForm from './TimeForm.vue';
import { ElButton, ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue';
import { useRouter, useRoute } from 'vue-router'
import { useHospitalStore } from '@/stores/hospitalData'
import { useUserStore } from '@/stores/user'
import { autoUpdateSchedules } from '@/api/admin/registrations'

const props = defineProps({
  cardType: {
    type: String,
    required: false,
    default: undefined
  }
})
const emit = defineEmits(['changeLoading'])
const hospitalStore = useHospitalStore();
const userStore = useUserStore();
const router = useRouter()
const route = useRoute()
const doctorDialogTableVisible = ref(false);
const scheduleDialogTableVisible = ref(false);
const autoScheduleDialogVisible = ref(false);
const optionType = ref("create")
const searchContent = ref('')
const departmentId = computed(() => (typeof route.query.departmentId === 'string' ? route.query.departmentId : ''))
const clinicId = computed(() => (typeof route.query.clinicId === 'string' ? route.query.clinicId : ''))
const doctorId = computed(() => (typeof route.query.doctorId === 'string' ? route.query.doctorId : ''))
const effectiveCardType = computed(() => {
  if (props.cardType) {
    return props.cardType
  }
  const role = userStore.userInfo?.role
  if (role === 2) {
    return 'admin'
  }
  if (role === 1) {
    return 'doctor'
  }
  return 'patient'
})

const pageTitle = computed(() => {
  const routeName = String(route.name || '')

  // 患者端挂号流程
  if (routeName === 'department') return '科室'
  if (routeName === 'clinic') return '门诊'
  if (routeName === 'clinicDoctor') return '医生'
  if (routeName === 'clinicDoctorSchedule') return '排班'

  // 管理端 CRUD 流程
  if (routeName === 'crudDepartment') return '科室'
  if (routeName === 'crudClinic') return '门诊'
  if (routeName === 'crudClinicDoctor') return '医生'
  if (routeName === 'crudClinicDoctorSchedule') return '排班'

  // 兜底（根据 query 推断）
  if (doctorId.value) return '排班'
  if (clinicId.value) return '医生'
  if (departmentId.value) return '门诊'
  return '科室'
})
const backpage = () => {
  if (route.params.department) {
    router.back()
  } else {
    return
  }
}

// 搜索处理函数
const handleSearch = async () => {
  emit('changeLoading')
  try {
    if (clinicId.value) {
      await hospitalStore.searchForDoctor(searchContent.value)
    }
    else if (departmentId.value) {
      await hospitalStore.searchForClinic(searchContent.value)
    }
  } catch (error) {
    console.log(error)
  } finally {
    emit('changeLoading')
  }
}

// 添加处理函数
const createNewItem = async () => {
  if (!departmentId.value && !clinicId.value && !doctorId.value) {
    try {
      const { value } = await ElMessageBox.prompt('请输入新科室的名称', '添加科室', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: '',
        inputPlaceholder: '例如：内科',
      })
      const newName = value.trim()
      if (!newName) return
      await hospitalStore.createDepart(newName)
    } catch {
      return
    }
    ElMessage({
      message: '添加科室成功',
      type: 'success',
    })
  } else if (!clinicId.value && departmentId.value) {
    try {
      const { value } = await ElMessageBox.prompt('请输入新门诊的名称', '添加门诊', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: '',
        inputPlaceholder: '例如：心内科门诊',
      })
      const newName = value.trim()
      if (!newName) return
      await hospitalStore.createClinic(departmentId.value, newName)
    } catch {
      return
    }
    ElMessage({
      message: '添加门诊成功',
      type: 'success',
    })
  }
  // 新增医生
  else if (clinicId.value && departmentId.value) {
    optionType.value = "create"
    doctorDialogTableVisible.value = true
  }
  // 新增排班
  else if (doctorId.value) {
    optionType.value = "create"
    scheduleDialogTableVisible.value = true
  }
  else {
    console.log('失败')
  }
}

// 显示全部一键排班对话框
const showAutoScheduleDialog = () => {
  autoScheduleDialogVisible.value = true;
}

// 处理全部一键排班
const handleAutoScheduleAll = async (startDate: string, endDate: string) => {
  try {
    await autoUpdateSchedules(startDate, endDate);
    ElMessage({
      message: `${startDate}至${endDate}期间的全部排班已自动创建`,
      type: 'success',
    });
    autoScheduleDialogVisible.value = false;
  } catch (error) {
    console.error('全部一键排班失败:', error);
  }
}
</script>

<style lang="scss" scoped>
.ap-top {
  height: 2rem;
  width: 85%;
  margin: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;

  #patient {
    display: flex;
    justify-content: space-between;
    gap: 5vw;
  }
}
</style>
