<template>
  <div class="card-message">
    <DoctorCard v-if="cardType === 'doctor'" :data="data" />
    <ScheduleCard v-else-if="cardType === 'schedule'" :data="data" @book="handleBook" />
    <AppointmentResultCard v-else-if="cardType === 'appointment_result'" :data="data" @cancel="handleCancel" />
    <div v-else class="card-unknown">[未知卡片类型: {{ cardType }}]</div>
  </div>
</template>

<script lang="ts" setup>
import DoctorCard from './DoctorCard.vue'
import ScheduleCard from './ScheduleCard.vue'
import AppointmentResultCard from './AppointmentResultCard.vue'

defineProps<{ cardType: string; data: any }>()

const emit = defineEmits<{
  book: [scheduleId: number]
  cancel: [appointmentId: number]
}>()

const handleBook = (scheduleId: number) => emit('book', scheduleId)
const handleCancel = (appointmentId: number) => emit('cancel', appointmentId)
</script>

<style lang="scss" scoped>
.card-message {
  margin: 8px 0;
}
.card-unknown {
  padding: 8px 12px;
  color: #94a3b8;
  font-size: 0.8rem;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
}
</style>
