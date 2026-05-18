<template>
  <CardView style="height: 100%; " v-loading="loading"
    :cardsprops="doctors.map(doctor => ({ ...doctor, cardType: 'admin' }))" :myCard="DoctorCard">
  </CardView>
</template>

<script lang="ts" setup>
import DoctorCard from '../DoctorCard.vue';
import CardView from '@/views/CardView.vue'
import { useHospitalStore } from '@/stores/hospitalData'
import { storeToRefs } from 'pinia';
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router'

const hospitalStore = useHospitalStore();
const loading = ref(true);
const route = useRoute();
const { doctors } = storeToRefs(hospitalStore);

onMounted(async () => {
  await hospitalStore.getDoctors(route.query.departmentId as string, route.query.clinicId as string);
  loading.value = false;
})
</script>

<style lang="scss" scoped>
.medical-ap {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
