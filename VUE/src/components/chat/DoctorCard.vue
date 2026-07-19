<template>
  <div v-if="data && data.length" class="doctor-card-list">
    <div v-for="doctor in data" :key="doctor.doctorId" class="doctor-card-item">
      <div class="doc-header">
        <span class="doc-name">{{ doctor.name }}</span>
        <span class="doc-title">{{ doctor.title }}</span>
      </div>
      <div v-if="doctor.introduction" class="doc-intro">{{ doctor.introduction }}</div>
      <div class="doc-dept">{{ doctor.departmentName }} · {{ doctor.clinicName }}</div>
    </div>
  </div>
  <div v-else class="card-empty">暂无医生信息</div>
</template>

<script lang="ts" setup>
defineProps<{ data: any }>()
</script>

<style lang="scss" scoped>
.doctor-card-list { display: flex; flex-direction: column; gap: 8px; }
.doctor-card-item {
  padding: 12px 14px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
}
.doc-header { display: flex; align-items: baseline; gap: 8px; margin-bottom: 4px; }
.doc-name { font-weight: 600; font-size: 0.95rem; color: #0c4a6e; }
.doc-title { font-size: 0.78rem; color: #0ea5e9; }
.doc-intro { font-size: 0.82rem; color: #64748b; margin-bottom: 4px; }
.doc-dept { font-size: 0.75rem; color: #94a3b8; }
.card-empty { color: #94a3b8; font-size: 0.85rem; padding: 8px; }
</style>
