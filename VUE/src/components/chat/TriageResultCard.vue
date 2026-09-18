<template>
  <div class="triage-card" :class="levelClass">
    <div class="triage-header">
      <span class="triage-badge" :class="levelClass">{{ levelEmoji }} {{ levelText }}</span>
      <span class="triage-dept" v-if="dept">推荐科室：{{ dept }}</span>
    </div>
    <p class="triage-advice">{{ advice }}</p>
    <p class="triage-disclaimer">* 分诊结果仅供参考，最终以医生判断为准</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  level: string; // "red" | "yellow" | "green"
  advice: string;
  dept?: string;
}>();

const levelClass = computed(() => `level-${props.level}`);
const levelText = computed(() => {
  switch (props.level) {
    case "red": return "紧急";
    case "yellow": return "需关注";
    case "green": return "常规";
    default: return "";
  }
});
const levelEmoji = computed(() => {
  switch (props.level) {
    case "red": return "🔴";
    case "yellow": return "🟡";
    case "green": return "🟢";
    default: return "";
  }
});
</script>

<style lang="scss" scoped>
.triage-card {
  padding: 14px 16px;
  border-radius: 12px;
  margin: 8px 0;
  border: 1.5px solid;
  &.level-red { background: #fef2f2; border-color: #fecaca; }
  &.level-yellow { background: #fffbeb; border-color: #fde68a; }
  &.level-green { background: #f0fdf4; border-color: #bbf7d0; }
}
.triage-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.triage-badge {
  font-size: 13px; font-weight: 700; padding: 2px 10px; border-radius: 20px;
  &.level-red { background: #fee2e2; color: #dc2626; }
  &.level-yellow { background: #fef3c7; color: #d97706; }
  &.level-green { background: #dcfce7; color: #16a34a; }
}
.triage-dept { font-size: 13px; color: #6366f1; font-weight: 600; }
.triage-advice { font-size: 14px; color: #334155; margin: 0; line-height: 1.6; }
.triage-disclaimer { font-size: 11px; color: #94a3b8; margin: 8px 0 0 0; }
</style>
