<template>
  <el-scrollbar>
    <div class="medical-ap">
      <ApTop @changeLoading="changeLoading" card-type="admin"></ApTop>
      <div style="flex: 1;">
        <RouterView v-loading="loading"></RouterView>
      </div>
    </div>
  </el-scrollbar>
</template>

<script lang="ts" setup>
import ApTop from '@/components/ApTop.vue'
import { RouterView } from 'vue-router'
import { ElScrollbar } from 'element-plus';
import { ref } from 'vue';

const loading = ref(false)
const changeLoading = () => {
  loading.value = !loading.value
}
</script>

<style lang="scss" scoped>
.medical-ap {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
