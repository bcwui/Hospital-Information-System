<template>
  <div class="appointments-page">
    <section class="main-shell utility-gap">
      <header class="page-head">
        <div>
          <p class="eyebrow">MY APPOINTMENTS</p>
          <h2>我的预约</h2>
          <p class="subtext">查看和管理您的预约挂号记录</p>
        </div>
      </header>
    </section>

    <div class="filter-tabs">
      <el-radio-group v-model="statusFilter" @change="loadAppointments">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">待就诊</el-radio-button>
        <el-radio-button :value="1">已就诊</el-radio-button>
        <el-radio-button :value="2">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <div class="appointments-list" v-loading="loading">
      <div v-if="appointments.length === 0" class="empty-state">
        <el-empty description="暂无预约记录" />
      </div>

      <div v-else class="appointment-cards">
        <div
          v-for="item in appointments"
          :key="item.appointmentId"
          class="appointment-card"
          :class="getStatusClass(item.status)"
        >
          <div class="card-header">
            <span class="status-tag" :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
            <span class="appointment-id">#{{ item.appointmentId }}</span>
          </div>

          <div class="card-body">
            <div class="doctor-info">
              <h3>{{ item.doctorName }}</h3>
              <p>{{ item.doctorTitle }} · {{ item.clinicName }}</p>
              <p class="dept">{{ item.deptName }}</p>
            </div>

            <div class="time-info">
              <p class="date">{{ item.appointmentDate }}</p>
              <p class="slot">{{ item.timeSlot }}</p>
            </div>
          </div>

          <div class="card-footer">
            <span class="create-time">预约时间：{{ formatTime(item.createTime) }}</span>
            <el-button
              v-if="item.canCancel"
              type="danger"
              size="small"
              @click="handleCancel(item)"
            >
              取消预约
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { getPatientAppointments, cancelRegistrations, type AppointmentVO } from '@/api/patient/registrations';

const userStore = useUserStore();
const loading = ref(false);
const statusFilter = ref<number | undefined>(undefined);
const appointments = ref<AppointmentVO[]>([]);

const loadAppointments = async () => {
  const patientId = userStore.userInfo?.patientId;
  if (!patientId) {
    ElMessage.warning('请先完善患者信息');
    return;
  }

  loading.value = true;
  try {
    const res = await getPatientAppointments(String(patientId), statusFilter.value);
    appointments.value = res || [];
  } catch (error) {
    console.error('加载预约记录失败', error);
  } finally {
    loading.value = false;
  }
};

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待就诊',
    1: '已就诊',
    2: '已取消',
  };
  return map[status] || '未知';
};

const getStatusClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'pending',
    1: 'completed',
    2: 'cancelled',
  };
  return map[status] || '';
};

const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const handleCancel = async (item: AppointmentVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消 ${item.doctorName} 医生 ${item.appointmentDate} ${item.timeSlot} 的预约吗？`,
      '取消预约',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    await cancelRegistrations(String(item.appointmentId), String(item.patientId));
    ElMessage.success('取消成功');
    loadAppointments();
  } catch (error) {
    // 用户点击取消按钮时 error 为 'cancel'
    if (error !== 'cancel') {
      console.error('取消预约失败', error);
    }
  }
};

onMounted(() => {
  loadAppointments();
});
</script>

<style lang="scss" scoped>
.appointments-page {
  padding: 1rem;
}

.filter-tabs {
  margin: 1rem 0;
}

.appointments-list {
  min-height: 200px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.appointment-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
}

.appointment-card {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-left: 4px solid #409eff;

  &.pending {
    border-left-color: #409eff;
  }

  &.completed {
    border-left-color: #67c23a;
  }

  &.cancelled {
    border-left-color: #909399;
    opacity: 0.7;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
}

.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;

  &.pending {
    background-color: #409eff;
  }

  &.completed {
    background-color: #67c23a;
  }

  &.cancelled {
    background-color: #909399;
  }
}

.appointment-id {
  font-size: 12px;
  color: #909399;
}

.card-body {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.8rem;
}

.doctor-info {
  h3 {
    margin: 0 0 4px 0;
    font-size: 16px;
    color: #303133;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #606266;
  }

  .dept {
    color: #909399;
    font-size: 12px;
  }
}

.time-info {
  text-align: right;

  .date {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    margin: 0;
  }

  .slot {
    font-size: 13px;
    color: #409eff;
    margin: 4px 0 0 0;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.8rem;
  border-top: 1px solid #ebeef5;
}

.create-time {
  font-size: 12px;
  color: #909399;
}
</style>
