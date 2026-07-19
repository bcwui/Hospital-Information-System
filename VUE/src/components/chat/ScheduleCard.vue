<template>
  <div v-if="data && data.length" class="schedule-card-list">
    <div v-for="item in data" :key="item.scheduleId" class="schedule-card-item">
      <div class="sch-info">
        <div class="sch-doctor">{{ item.doctorName }}</div>
        <div class="sch-time">{{ item.scheduleDate }} {{ item.timeSlot }}</div>
        <div class="sch-remaining" :class="{ full: item.remaining === 0 }">
          {{ item.remaining > 0 ? `剩余 ${item.remaining} 个名额` : '已约满' }}
        </div>
      </div>
      <button v-if="item.remaining > 0" class="book-btn" @click.stop="$emit('book', item.scheduleId)">
        预约
      </button>
    </div>
  </div>
  <div v-else class="card-empty">暂无可预约排班</div>
</template>

<script lang="ts" setup>
defineProps<{ data: any }>()
defineEmits<{ book: [scheduleId: number] }>()
</script>

<style lang="scss" scoped>
.schedule-card-list { display: flex; flex-direction: column; gap: 6px; }
.schedule-card-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  background: #fefce8; border: 1px solid #fde68a; border-radius: 10px;
}
.sch-info { flex: 1; }
.sch-doctor { font-weight: 600; font-size: 0.9rem; color: #713f12; }
.sch-time { font-size: 0.82rem; color: #a16207; margin-top: 2px; }
.sch-remaining { font-size: 0.78rem; color: #16a34a; margin-top: 2px; }
.sch-remaining.full { color: #dc2626; }
.book-btn {
  flex-shrink: 0;
  padding: 6px 16px;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: white; border: none; border-radius: 8px;
  font-size: 0.82rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease; margin-left: 12px;
  &:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(14,165,233,0.35); }
}
.card-empty { color: #94a3b8; font-size: 0.85rem; padding: 8px; }
</style>
