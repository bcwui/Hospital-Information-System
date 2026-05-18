<script setup lang="ts">
import { onMounted, reactive, type Reactive, ref, computed } from 'vue';
import { doctorGetDetailRegistration } from '@/api/doctor/registrations';
import { useRoute, useRouter } from 'vue-router';
import { type detailRegistration } from '@/api/doctor/registrations';
import DiagnoseForm from './DiagnoseForm.vue';

const patientData: Reactive<detailRegistration> = reactive({
  deptName: "无",
  clinicName: "无",
  appointmentId: "",
  scheduleId: "无",
  doctorName: "无",
  doctorId: "",
  patientName: "无",
  patientId: "",
  isRevisit: 0,
  status: 0,
  description: "无",
  appointmentDate: "无",
  statusDesc: "无",
  aiConsultSessionId: "无",
});
const dialogTableVisible = ref(false);
const loading = ref(true);
const route = useRoute();
const router = useRouter();
const doctorId = computed(() => (typeof route.query.doctorId === 'string' ? route.query.doctorId : ''))
const appointmentId = computed(() => (typeof route.query.appointmentId === 'string' ? route.query.appointmentId : ''))

const revisitText = computed(() => (patientData.isRevisit === 0 ? '初诊' : '复诊'))
const revisitPillClass = computed(() => (patientData.isRevisit === 0 ? '' : 'is-success'))

const statusText = computed(() => {
  if (patientData.status === 0) return '待就诊'
  if (patientData.status === 1) return '已就诊'
  if (patientData.status === 2) return '已取消'
  return '异常'
})

const statusPillClass = computed(() => {
  if (patientData.status === 1) return 'is-success'
  if (patientData.status === 2) return 'is-warning'
  return ''
})

onMounted(async () => {
  if (!doctorId.value || !appointmentId.value) {
    loading.value = false;
    return;
  }
  const newPatientData = await doctorGetDetailRegistration(doctorId.value, appointmentId.value);
  if (newPatientData) {
    Object.assign(patientData, newPatientData);
  }
  loading.value = false;
})
</script>

<template>
  <el-scrollbar class="detail-registrations" v-loading="loading">
    <section class="main-shell utility-gap">
      <header class="page-head">
        <div>
          <p class="eyebrow">REGISTRATION</p>
          <h2>患者信息</h2>
          <p class="subtext">患者：{{ patientData.patientName }} · 时间：{{ patientData.appointmentDate }}</p>
        </div>
        <el-button type="primary" plain @click="router.back()">返回</el-button>
      </header>

      <div class="content-grid">
        <section class="surface-card info-card">
          <header class="card-head">
            <div>
              <p class="eyebrow">DETAIL</p>
              <h3 class="card-title">详细信息</h3>
            </div>
            <div class="pill-row">
              <span class="status-pill" :class="revisitPillClass">{{ revisitText }}</span>
              <span class="status-pill" :class="statusPillClass">{{ statusText }}</span>
            </div>
          </header>

          <section class="card-body info-body">
            <section class="kv-grid">
              <div class="kv">
                <p class="k">序号</p>
                <p class="v">{{ patientData.appointmentId }}</p>
              </div>
              <div class="kv">
                <p class="k">病历号</p>
                <p class="v">{{ patientData.scheduleId }}</p>
              </div>
              <div class="kv">
                <p class="k">科室名</p>
                <p class="v">{{ patientData.deptName }}</p>
              </div>
              <div class="kv">
                <p class="k">诊室名</p>
                <p class="v">{{ patientData.clinicName }}</p>
              </div>
              <div class="kv">
                <p class="k">就诊医生</p>
                <p class="v">{{ patientData.doctorName }}</p>
              </div>
              <div class="kv">
                <p class="k">患者编号</p>
                <p class="v">{{ patientData.patientId }}</p>
              </div>
              <div class="kv kv--full">
                <p class="k">症状</p>
                <p class="v v--muted">{{ patientData.description }}</p>
              </div>
              <div class="kv kv--full">
                <p class="k">备注</p>
                <p class="v v--muted">{{ patientData.statusDesc }}</p>
              </div>
            </section>
          </section>

          <footer class="card-footer">
            <div>
              <el-button v-if="patientData.status !== 1" type="success"
                @click="dialogTableVisible = true">完成就诊</el-button>
              <el-button v-else type="success" disabled>已完成</el-button>
            </div>
            <el-button type="danger" disabled>取消就诊</el-button>
          </footer>
        </section>
      </div>
    </section>
  </el-scrollbar>

  <el-dialog v-model="dialogTableVisible" title="请填写医生信息" width="800">
    <DiagnoseForm :appointmentId="patientData.appointmentId" :doctorId="patientData.doctorId"
      :patientId="patientData.patientId" @close="dialogTableVisible = false">
    </DiagnoseForm>
  </el-dialog>
</template>

<style scoped lang="scss">
.content-grid {
  width: 100%;
}

.info-card {
  width: 100%;
  max-width: 600px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.card-body {
  min-height: 0;
}

.info-body {
  flex: 1 1 auto;
  overflow: auto;
}

.info-card {
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-primary);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  margin-bottom: var(--space-3);
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}

.kv-grid {
  width: 100%;
}

.kv-grid::after {
  content: '';
  display: block;
  clear: both;
}

.kv {
  float: left;
  width: 50%;
  padding-right: var(--space-4);
  margin-bottom: var(--space-3);
  min-width: 0;
}

.kv:nth-of-type(2n) {
  padding-right: 0;
}

.kv--full {
  clear: both;
  width: 100%;
  padding-right: 0;
}

.k {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
}

.v {
  margin: 4px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  word-break: break-word;
}

.v--muted {
  font-weight: 500;
  color: var(--color-text-muted);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
  padding-top: var(--space-4);
}

@media (max-width: 768px) {
  .kv-grid {
    width: 100%;
  }

  .kv {
    float: none;
    width: 100%;
    padding-right: 0;
  }

  .card-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
