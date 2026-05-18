<template>
  <el-input v-model="doctorName" style="width: 240px;height: 40px;" placeholder="搜索医生" :prefix-icon="Search"
    @keyup.enter="searchDoctor" />
  <el-button type="primary" style="height: 40px;" @click="searchDoctor">
    <el-icon size="large">
      <Search />
    </el-icon>
  </el-button>
  <CardView style="height: 100%; " v-loading="loading"
    :cardsprops="doctors.map((item) => { return { ...item, cardType: 'admin' } })" :myCard="DoctorCard">
  </CardView>
</template>

<script lang="ts" setup>
import { Search } from '@element-plus/icons-vue'
import CardView from '@/views/CardView.vue'
import DoctorCard from '@/components/DoctorCard.vue'
import { useHospitalStore } from '@/stores/hospitalData'
import { storeToRefs } from 'pinia'
import { ElInput, ElIcon } from 'element-plus'
import { onMounted, ref } from 'vue';
const doctorName = ref('');
const hospitalStore = useHospitalStore();
const loading = ref(true);
const { doctors } = storeToRefs(hospitalStore);

const searchDoctor = async () => {
  const keyword = doctorName.value.trim();
  loading.value = true;
  try {
    // 当前 store 仅提供 searchForDoctor；若输入为空则保持现有列表。
    if (keyword) {
      await hospitalStore.searchForDoctor(keyword);
    }
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  // 重要：如果 loading 一直为 true，v-loading 的遮罩会挡住所有点击（包括“修改/删除”按钮）。
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
