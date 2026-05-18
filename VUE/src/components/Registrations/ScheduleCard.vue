<template>
  <article class="schedule-card surface-card">
    <header class="schedule-card__header">
      <div class="schedule-card__title">
        <p class="eyebrow">号源信息 · 编号 {{ props.scheduleId }}</p>
        <h3>{{ props.doctorName }}</h3>
        <p class="subtitle">{{ props.doctorTitle }}</p>
      </div>
      <div class="schedule-card__quota">
        <span class="status-pill" :class="props.canBook ? 'is-success' : 'is-warning'">
          {{ props.canBook ? '可预约' : '不可预约' }}
        </span>
        <strong>{{ props.remainingQuota }}</strong>
        <small>剩余号源</small>
      </div>
    </header>

    <div class="schedule-card__body">
      <el-avatar class="schedule-card__avatar" :size="96"
        :src="props.doctorAvatar ? props.doctorAvatar : '/src/assets/doctor.png'" />
      <div class="schedule-card__info-grid">
        <div class="info-row">
          <span class="label">医生编号</span>
          <span class="value">{{ props.doctorId }}</span>
        </div>
        <div class="info-row">
          <span class="label">挂号日期</span>
          <span class="value">{{ props.scheduleDate }}</span>
        </div>
        <div class="info-row">
          <span class="label">时间段</span>
          <span class="value">{{ props.timeSlot }}</span>
        </div>
        <div class="info-row">
          <span class="label">职称</span>
          <span class="value">{{ props.doctorTitle }}</span>
        </div>
        <div class="info-row" v-if="props.doctorIntroduction">
          <span class="label">简介</span>
          <span class="value muted">{{ props.doctorIntroduction }}</span>
        </div>
      </div>
    </div>

    <footer class="schedule-card__actions" v-if="props.cardType === 'doctor'">
      <el-button type="primary" size="large" @click="createSchedule" :disabled="!props.canBook">
        立即预约
      </el-button>
    </footer>
    <footer class="schedule-card__actions" v-else-if="props.cardType === 'admin'">
      <el-button type="danger" size="large" @click="deleteSchedule" :disabled="!props.canBook">
        取消排班
      </el-button>
    </footer>
  </article>
</template>

<script lang="ts" setup>
import { ref, type Ref, onMounted, computed } from 'vue';
import { ElAvatar, ElMessage, ElMessageBox } from 'element-plus';
import { createRegistrations } from '@/api/patient/registrations';
import type { UserInfo } from '@/stores/user';
import { useHospitalStore } from '@/stores/hospitalData';
import { useRoute } from 'vue-router';

const user: Ref<UserInfo> = ref({
  userId: '',
  patientId: '',
  username: '',
  email: '',
  phone: '',
  avatar: '',
  name: '',
  gender: 0,
  age: 0,
  idCard: '',
  region: '',
  address: '',
  role: 0,
  createTime: '',
  updateTime: '',
  token: '',
});

const props = defineProps({
  cardType: {
    type: String,
    required: true,
  },
  scheduleId: {
    type: String,
    required: true,
  },
  doctorId: {
    type: String,
    required: true,
  },
  doctorName: {
    type: String,
    required: true,
  },
  doctorTitle: {
    type: String,
    required: true,
  },
  doctorIntroduction: {
    type: String,
    required: true,
  },
  doctorAvatar: {
    type: String,
    required: true,
  },
  scheduleDate: {
    type: String,
    required: true,
  },
  timeSlot: {
    type: String,
    required: true,
  },
  remainingQuota: {
    type: Number,
    required: true,
  },
  canBook: {
    type: Boolean,
    required: true,
  }
});

const route = useRoute();
const hospitalStore = useHospitalStore();
const routeDoctorTitle = computed(() => (typeof route.query.title === 'string' ? route.query.title : ''))
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
    endDate: formatDate(sevenDaysLaterDate),
  };
};

const createSchedule = async () => {
  try {
    if (!user.value.patientId) {
      ElMessage.error('请先登录后再预约');
      return;
    }
    const res = await createRegistrations(user.value.patientId, props.scheduleId);
    if (res) {
      ElMessage.success('预约成功');
    }
  } catch (error) {
    ElMessage.error('预约失败，请稍后重试');
    console.error(error);
  }
}

const deleteSchedule = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要取消该排班吗？\n医生：${props.doctorName}\n日期：${props.scheduleDate}（${props.timeSlot}）`,
      '取消排班确认',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '返回',
        type: 'warning',
      }
    )
  } catch {
    return
  }
  const window = getScheduleWindow();
  const res = await hospitalStore.deleteSchedule(props.scheduleId, props.doctorId, {
    title: routeDoctorTitle.value,
    startDate: window.startDate,
    endDate: window.endDate,
  })
  if (res) {
    ElMessage.success('删除排班成功')
  } else {
    ElMessage.error('删除排班失败')
  }
}


onMounted(() => {
  const userInfoString = localStorage.getItem('userInfo');
  if (userInfoString) {
    user.value = JSON.parse(userInfoString) as UserInfo;
  }
})
</script>

<style lang="scss" scoped>
.schedule-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  height: auto;
  min-height: 420px;
}

.schedule-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.schedule-card__title h3 {
  margin: 4px 0;
  font-size: 24px;
  color: var(--color-text);
}

.schedule-card__title .subtitle {
  color: var(--color-text-muted);
  margin-top: 2px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.schedule-card__quota {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: flex-end;
}

.schedule-card__quota strong {
  font-size: 28px;
  color: var(--color-text);
  line-height: 1;
}

.schedule-card__quota small {
  color: var(--color-text-muted);
}

.schedule-card__body {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-4);
  align-items: center;
  flex: 1;
}

.schedule-card__avatar {
  border: 4px solid var(--color-primary-soft);
}

.schedule-card__info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-3);
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
}

.value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.value.muted {
  font-weight: 400;
  color: var(--color-text-muted);

  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  overflow: hidden;
}

.schedule-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: auto;
}

@media (max-width: 640px) {
  .schedule-card__body {
    grid-template-columns: 1fr;
  }

  .schedule-card__quota {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
