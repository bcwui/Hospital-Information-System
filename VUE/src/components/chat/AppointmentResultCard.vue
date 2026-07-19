<template>
  <div class="result-card" :class="data?.success ? 'success' : 'fail'">
    <div class="result-icon">
      <svg v-if="data?.success" viewBox="0 0 24 24" fill="none" width="20" height="20">
        <circle cx="12" cy="12" r="10" stroke="#22c55e" stroke-width="2"/>
        <path d="M8 12l3 3 5-5" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" width="20" height="20">
        <circle cx="12" cy="12" r="10" stroke="#ef4444" stroke-width="2"/>
        <path d="M15 9l-6 6M9 9l6 6" stroke="#ef4444" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="result-text">{{ data?.message || '操作完成' }}</div>
    <button v-if="data?.appointmentId && data?.success" class="cancel-btn" @click="$emit('cancel', data.appointmentId)">
      取消预约
    </button>
  </div>
</template>

<script lang="ts" setup>
defineProps<{ data: any }>()
defineEmits<{ cancel: [appointmentId: number] }>()
</script>

<style lang="scss" scoped>
.result-card {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border-radius: 10px;
}
.result-card.success { background: #f0fdf4; border: 1px solid #bbf7d0; }
.result-card.fail { background: #fef2f2; border: 1px solid #fecaca; }
.result-icon { flex-shrink: 0; }
.result-text { flex: 1; font-size: 0.9rem; font-weight: 500; }
.result-card.success .result-text { color: #166534; }
.result-card.fail .result-text { color: #991b1b; }
.cancel-btn {
  padding: 5px 12px; background: #fef2f2; color: #dc2626;
  border: 1px solid #fecaca; border-radius: 6px;
  font-size: 0.78rem; cursor: pointer;
  &:hover { background: #fee2e2; }
}
</style>
