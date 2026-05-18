<template>
  <article class="patient-info-card surface-card">
    <header class="card-head">
      <div>
        <p class="eyebrow">PATIENT</p>
        <h2 class="title">患者信息</h2>
      </div>
      <div class="pill-row">
        <span class="status-pill" :class="revisitPillClass">{{ revisitText }}</span>
        <span class="status-pill" :class="statusPillClass">{{ statusText }}</span>
      </div>
    </header>

    <section class="kv-grid">
      <div class="kv">
        <p class="k">序号</p>
        <p class="v">{{ appointmentId }}</p>
      </div>
      <div class="kv">
        <p class="k">病历号</p>
        <p class="v">{{ scheduleId }}</p>
      </div>
      <div class="kv">
        <p class="k">病人姓名</p>
        <p class="v">{{ patientName }}</p>
      </div>
      <div class="kv">
        <p class="k">挂号时间</p>
        <p class="v">{{ appointmentDate }}</p>
      </div>
      <div class="kv">
        <p class="k">科室名</p>
        <p class="v">{{ deptName }}</p>
      </div>
      <div class="kv">
        <p class="k">诊室名</p>
        <p class="v">{{ clinicName }}</p>
      </div>
      <div class="kv">
        <p class="k">就诊医生</p>
        <p class="v">{{ doctorName }}</p>
      </div>
      <div class="kv kv--full">
        <p class="k">备注</p>
        <p class="v v--muted">{{ statusDesc }}</p>
      </div>
    </section>
  </article>
</template>
<script setup lang="ts">
import { computed } from "vue";

const props = defineProps({
  appointmentId: {
    type: String,
    required: true,
    default: "1",
  },
  scheduleId: {
    type: String,
    required: true,
    default: "001",
  },
  patientName: {
    type: String,
    required: true,
    default: "默认患者",
  },
  isRevisit: {
    type: Number,
    required: true,
    default: 0,
  },
  status: {
    type: Number,
    required: true,
    default: 18,
  },
  description: {
    type: String,
    required: false,
    default: "请阐释症状,例:患者头部发热",
  },
  appointmentDate: {
    type: String,
    required: true,
    default: "2025-05-01",
  },
  deptName: {
    type: String,
    required: true,
    default: "内科",
  },
  clinicName: {
    type: String,
    required: true,
    default: "门诊1",
  },
  doctorName: {
    type: String,
    required: true,
    default: "待执行",
  },
  statusDesc: {
    type: String,
    required: true,
    default: "无",
  },
});

const revisitText = computed(() => (props.isRevisit === 0 ? '初诊' : '复诊'))
const revisitPillClass = computed(() => (props.isRevisit === 0 ? '' : 'is-success'))

const statusText = computed(() => {
  if (props.status === 0) return '待就诊'
  if (props.status === 1) return '已就诊'
  if (props.status === 2) return '已取消'
  return '异常'
})

const statusPillClass = computed(() => {
  if (props.status === 1) return 'is-success'
  if (props.status === 2) return 'is-warning'
  return ''
})
</script>

<style lang="scss" scoped>
.patient-info-card {
  width: 400px;
  height: 350px;
  cursor: pointer;
  border: 1px solid var(--color-border);
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

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.title {
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
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3) var(--space-4);
}

.kv {
  min-width: 0;
}

.kv--full {
  grid-column: 1 / -1;
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

@media (max-width: 768px) {
  .kv-grid {
    grid-template-columns: 1fr;
  }
}
</style>
