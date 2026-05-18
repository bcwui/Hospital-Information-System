<template>
  <CardView v-loading="loading" :cardsprops="schedules.map((item) => { return { ...item, cardType: 'admin' } })"
    :myCard="ScheduleCard" style="text-align: center; height: 100%;">
  </CardView>
</template>

<script lang="ts" setup>
import ScheduleCard from '@/components/Registrations/ScheduleCard.vue'
import CardView from '@/views/CardView.vue'
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHospitalStore } from '@/stores/hospitalData';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';

const loading = ref(true);
const hospitalStore = useHospitalStore();
const route = useRoute();
const { schedules } = storeToRefs(hospitalStore);
const doctorId = computed(() => typeof route.query.doctorId === 'string' ? route.query.doctorId : '');
const doctorTitle = computed(() => typeof route.query.title === 'string' ? route.query.title : '');

function getCurrentAndSevenDaysLaterDate(): {
  currentDate: string;
  sevenDaysLater: string;
} {
  const currentDate = new Date();
  const sevenDaysLaterDate = new Date(currentDate.getTime() + 7 * 86400000);

  function formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  return {
    currentDate: formatDate(currentDate),
    sevenDaysLater: formatDate(sevenDaysLaterDate)
  };
}

const loadSchedules = async () => {
  if (!doctorId.value) {
    loading.value = false;
    ElMessage.error('缺少 doctorId，无法获取排班');
    return;
  }

  loading.value = true;
  const { currentDate, sevenDaysLater } = getCurrentAndSevenDaysLaterDate();
  const ok = await hospitalStore.getSchedules(doctorId.value, doctorTitle.value, currentDate, sevenDaysLater);
  loading.value = false;
  if (ok) {
    ElMessage.success('获取排班成功');
  } else {
    ElMessage.error('获取排班失败');
  }
};

watch([doctorId, doctorTitle], () => {
  void loadSchedules();
}, { immediate: true });

</script>

<style lang="scss" scoped>
.medical-ap {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
