<script setup lang="ts">
import { ElScrollbar, ElEmpty } from 'element-plus';
import RegistrationCard from './RegistrationCard.vue';
import { useRouter } from 'vue-router';
import { onMounted, ref, reactive, type Reactive } from 'vue';
import { doctorGetRegistrations } from '@/api/doctor/registrations';
import { getUesrInfo } from '@/api/patient/myInfo';
import type { Registration } from '@/api/doctor/registrations';

const loading = ref(true);
const router = useRouter();
const registrations: Reactive<Registration[]> = reactive([])
const goToDetail = (name: string, doctorId: string, appointmentId: string) => {
  router.push({
    name: "detailRegistrations",
    params: {
      name: name,
    },
    query: {
      doctorId: doctorId,
      appointmentId: appointmentId,
    },
  })
}
onMounted(async () => {
  const userInfo = await getUesrInfo();
  const doctorId = userInfo?.doctorId;
  if (!doctorId) {
    loading.value = false;
    return;
  }
  const getRegistrations = await doctorGetRegistrations(doctorId);
  if (getRegistrations) {
    registrations.splice(0, registrations.length, ...getRegistrations);
  }
  loading.value = false;
})
</script>

<template>
  <section class="main-shell utility-gap">
    <header class="page-head">
      <div>
        <p class="eyebrow">REGISTRATIONS</p>
        <h2>挂号记录</h2>
      </div>
    </header>
    <el-scrollbar v-loading="loading" class="registrations-scroll">
      <div v-if="registrations.length > 0" class="registrations-grid">
        <RegistrationCard v-for="item in registrations" :key="item.appointmentId" class="registration-card"
          @click="goToDetail(item.patientName, item.doctorId, item.appointmentId)" v-bind="item">
        </RegistrationCard>
      </div>
      <el-empty v-else description="还没有挂号记录" class="empty-state surface-card" />
    </el-scrollbar>
  </section>
</template>

<style lang="scss" scoped>
.registrations-scroll {
  height: calc(100vh - 350px);
  width: 100%;
}

.registrations-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: stretch;
}

.registration-card {
  flex: 1 1 340px;
  min-width: 280px;
  height: 450px;
}

.empty-state {
  width: 100%;
  padding: var(--space-8) 0;
}

@media (max-width: 768px) {
  .registration-card {
    flex-basis: 100%;
    min-width: 0;
  }
}
</style>
