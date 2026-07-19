<template>
  <div :class="['tool-step', statusClass]">
    <span class="tool-icon">
      <svg v-if="step.status === 'running'" class="spin" viewBox="0 0 24 24" fill="none" width="14" height="14">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" opacity="0.25"/>
        <path d="M12 2a10 10 0 019.95 9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
      </svg>
      <svg v-else-if="step.status === 'success'" viewBox="0 0 24 24" fill="none" width="14" height="14">
        <circle cx="12" cy="12" r="10" stroke="#22c55e" stroke-width="2"/>
        <path d="M8 12l3 3 5-5" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" width="14" height="14">
        <circle cx="12" cy="12" r="10" stroke="#ef4444" stroke-width="2"/>
        <path d="M15 9l-6 6M9 9l6 6" stroke="#ef4444" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </span>
    <span class="tool-label">{{ step.label }}</span>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { MessageBlock } from '@/types/chat'

const props = defineProps<{
  step: Extract<MessageBlock, { type: 'toolStep' }>
}>()

const statusClass = computed(() => `tool-${props.step.status}`)
</script>

<style lang="scss" scoped>
.tool-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin: 4px 0;
  border-radius: 8px;
  font-size: 0.82rem;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.tool-success { color: #16a34a; background: #f0fdf4; border-color: #bbf7d0; }
.tool-error { color: #dc2626; background: #fef2f2; border-color: #fecaca; }
.tool-icon { display: flex; align-items: center; flex-shrink: 0; }
.tool-label { flex: 1; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
