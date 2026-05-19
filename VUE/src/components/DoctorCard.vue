<template>
  <article class="doctor-card surface-card">
    <header class="doctor-card__header">
      <div class="doctor-card__title">
        <p class="eyebrow">医生信息</p>
        <h3 class="doctor-name">{{ name }}</h3>
        <p class="subtitle">{{ title }} · {{ deptName }}</p>
      </div>
    </header>

    <div class="doctor-card__body">
      <div class="doctor-avatar">
        <img :src="avatarSrc" alt="医生头像" />
      </div>
      <div class="doctor-card__desc">
        <p class="desc-label">简介</p>
        <el-scrollbar height="120px">
          <p class="doctor-description">{{ introduction }}</p>
        </el-scrollbar>
      </div>
    </div>

    <footer class="doctor-card__actions" v-if="props.cardType === 'doctor'">
      <el-button type="primary" @click.stop="getSchedule()">获取排班</el-button>
    </footer>
    <footer class="doctor-card__actions" v-else-if="props.cardType === 'admin'">
      <el-button type="info" @click.stop="getCrudSchedule()">获取排班</el-button>
      <el-button type="primary" @click.stop="dialogTableVisible = true">修改</el-button>
      <el-button type="danger" @click.stop="deleteDoctor(doctorId)">删除</el-button>
    </footer>
  </article>
  <el-dialog v-model="dialogTableVisible" title="请填写医生信息" width="800">
    <DoctorForm :optionType="optionType" :doctor-id="doctorId" :user-id="userId" @success="dialogTableVisible = false"
      :initial="{
        name: props.name,
        title: props.title,
        introduction: props.introduction,
        avatarUrl: avatarSrc,
      }"></DoctorForm>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { ElButton, ElScrollbar, ElMessage, ElMessageBox } from 'element-plus';
import DoctorForm from '@/components/Admin/DoctorForm.vue';
import { useHospitalStore } from '@/stores/hospitalData';
import { useRoute } from 'vue-router';
import router from '@/router';
import defaultAvatar from '@/assets/doctor.png';
const dialogTableVisible = ref(false);
const optionType = ref("update")
const hospitalStore = useHospitalStore();
const route = useRoute();
const getQueryString = (value: unknown): string => {
  if (Array.isArray(value)) {
    const [first] = value;
    return typeof first === 'string' ? first : '';
  }
  return typeof value === 'string' ? value : '';
};

const getRouteString = (value: unknown): string => getQueryString(value);
const props = defineProps({
  cardType: {
    type: String,
    required: true,
  },
  name: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  deptName: {
    type: String,
    required: true,
  },
  introduction: {
    type: String,
    required: true,
  },
  avatar: {
    type: String,
    default: '',
  },
  doctorId: {
    type: String,
    required: true,
  },
  userId: {
    type: String,
    required: true,
  },
})

const departmentId = computed(() => getQueryString(route.query.departmentId));
const clinicId = computed(() => getQueryString(route.query.clinicId));
const avatarSrc = computed(() => props.avatar || defaultAvatar);

const getSchedule = () => {
  if (!props.doctorId) {
    ElMessage.error('缺少 doctorId，无法跳转到排班页');
    return;
  }

  const department =
    getRouteString(route.params.department) ||
    getRouteString(route.query.departmentName) ||
    '未知科室';
  const clinic =
    getRouteString(route.params.clinic) ||
    getRouteString(route.query.clinicName) ||
    '未知门诊';

  router.push({
    name: "clinicDoctorSchedule",
    params: {
      department,
      clinic,
      doctor: props.name
    },
    query: {
      doctorId: props.doctorId,
      title: props.title,
      name: props.name,
      introduction: props.introduction,
      avatar: props.avatar,
    }
  }).catch((err) => {
    const msg = err instanceof Error ? err.message : String(err);
    ElMessage.error(`跳转失败：${msg}`);
  })
}

const getCrudSchedule = () => {
  if (!props.doctorId) {
    ElMessage.error('缺少 doctorId，无法跳转到排班页');
    return;
  }

  const department =
    getRouteString(route.params.department) ||
    getRouteString(route.query.departmentName) ||
    '未知科室';
  const clinic =
    getRouteString(route.params.clinic) ||
    getRouteString(route.query.clinicName) ||
    '未知门诊';

  router.push({
    name: "crudClinicDoctorSchedule",
    params: {
      department,
      clinic,
      doctor: props.name
    },
    query: {
      doctorId: props.doctorId,
      clinicId: route.query.clinicId as string,
      title: props.title,
      name: props.name,
      introduction: props.introduction,
      avatar: props.avatar,
    }
  }).catch((err) => {
    const msg = err instanceof Error ? err.message : String(err);
    ElMessage.error(`跳转失败：${msg}`);
  })
}

const deleteDoctor = async (doctorId: string) => {
  if (!departmentId.value || !clinicId.value) {
    ElMessage({
      message: '缺少科室或门诊信息，无法删除医生',
      type: 'error',
    })
    return;
  }

  try {
    await ElMessageBox.confirm(`确定要删除医生“${props.name}”吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return;
  }

  const res = await hospitalStore.deleteDoctor(doctorId, departmentId.value, clinicId.value)
  if (res) {
    ElMessage({
      message: '删除医生成功',
      type: 'success',
    })
  } else {
    ElMessage({
      message: '删除医生失败',
      type: 'error',
    })
  }
}
</script>

<style lang="scss" scoped>
.doctor-card {
  position: relative;
  height: 320px;
  width: 300px;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  border: 1px solid transparent;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #10b981, #0ea5e9, #6366f1);
    opacity: 0;
    transition: opacity 0.35s ease;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 32px rgba(14, 165, 233, 0.12);
    border-color: rgba(14, 165, 233, 0.15);

    &::before {
      opacity: 1;
    }

    .doctor-name {
      background: linear-gradient(135deg, #0ea5e9, #6366f1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

.doctor-card__header {
  display: flex;
  justify-content: space-between;
}

.doctor-card__title {
  min-width: 0;
}

.eyebrow {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: 600;
}

.doctor-avatar {
  width: 100px;
  height: 100px;
  margin: 0;
  border-radius: 50%;
  overflow: hidden;
  flex: 0 0 auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 3px solid #fff;
  transition: all 0.3s ease;

  .doctor-card:hover & {
    border-color: rgba(14, 165, 233, 0.2);
    box-shadow: 0 6px 16px rgba(14, 165, 233, 0.15);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.doctor-name {
  font-size: 18px;
  font-weight: 700;
  margin: 4px 0;
  color: var(--color-text);
  transition: all 0.3s ease;
}

.subtitle {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.doctor-card__body {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-3);
  align-items: start;
  flex: 1;
  margin: 20px 0;
  min-height: 0;
}

.doctor-card__desc {
  min-width: 0;
}

.desc-label {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  margin-bottom: var(--space-2);
  font-weight: 600;
}

.doctor-description {
  font-size: 12px;
  color: var(--color-text);
  line-height: 1.6;
}

.doctor-card__actions {
  display: flex;
  justify-content: center;
  gap: var(--space-2);
  flex-wrap: wrap;

  :deep(.el-button) {
    border-radius: 8px;
    font-size: 0.82rem;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-1px);
    }
  }
}
</style>
