<template>
  <div class="patient-container">
    <el-container style="height: 100%;">
      <el-header style="display: flex; justify-content: space-between; align-items: center;">
        <div class="tip">医疗门诊</div>
        <el-button type="primary" @click="logout">退出登录</el-button>
      </el-header>
      <el-main style="height: 100%;">
        <el-container style="height: 100%;">
          <el-aside>
            <me-nu :routers="routers"></me-nu>
          </el-aside>
          <el-main class="main-content">
            <router-view></router-view>
          </el-main>
        </el-container>
      </el-main>
    </el-container>
  </div>
</template>

<script lang="ts" setup>
import MeNu from '@/components/MeNu.vue';
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';

const userStore = useUserStore()

const route = useRouter()

const logout = () => {
  userStore.logout()
  route.push('/')
}

const routers = [
  {
    router: '/doctor',
    name: '挂号预约'
  },
  {
    router: '/doctor/myinfo',
    name: '我的信息'
  },
]


</script>

<style lang="scss" scoped>
.patient-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 95%;
  height: 95%;
  background: vars.$card-bg;
  border-radius: 1rem;
  margin: 0 auto;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
}

.tip {
  font-size: 1.5rem;
  font-weight: bold;
  margin: 1rem 0;
}

.main-content {
  border-radius: 1rem;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
  width: 100%;
  height: 100%;
  background: vars.$card-bg;
}
</style>
