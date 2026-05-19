<template>
  <div class="patient-container">
    <el-container style="height: 100%;">
      <el-header class="app-header">
        <div class="header-left">
          <div class="logo-area">
            <svg class="logo-icon" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="10" fill="url(#logoGrad)"/>
              <path d="M10 16h12M16 10v12M12 13l4-3 4 3M12 19l4 3 4-3" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="logoGrad" x1="0" y1="0" x2="32" y2="32">
                  <stop stop-color="#0ea5e9"/>
                  <stop offset="1" stop-color="#6366f1"/>
                </linearGradient>
              </defs>
            </svg>
            <span class="app-title">智慧医疗门诊</span>
            <span class="role-badge patient">患者端</span>
          </div>
        </div>
        <div class="header-right">
          <span class="user-greeting">你好，{{ userStore.userName }}</span>
          <el-button class="logout-btn" @click="logout">
            <svg viewBox="0 0 24 24" fill="none" width="16" height="16">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            退出登录
          </el-button>
        </div>
      </el-header>
      <el-main style="height: 100%;">
        <el-container style="height: 100%;">
          <el-aside class="app-aside">
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
    router: '/patient',
    name: '挂号预约'
  },
  {
    router: '/patient/appointments',
    name: '我的预约',
  },
  {
    router: '/patient/chat',
    name: 'AI小助手',
  },
  {
    router: '/patient/myinfo',
    name: '我的信息',
  }
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
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  margin: 0 auto;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1.5rem;
  height: 64px;
  background: linear-gradient(135deg, #f8faff, #f0f9ff);
  border-bottom: 1px solid rgba(14, 165, 233, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.logo-icon {
  width: 32px;
  height: 32px;
  filter: drop-shadow(0 2px 6px rgba(14, 165, 233, 0.3));
}

.app-title {
  font-size: 1.15rem;
  font-weight: 700;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.role-badge {
  font-size: 0.72rem;
  padding: 2px 10px;
  border-radius: 20px;
  font-weight: 600;
  letter-spacing: 0.02em;

  &.patient {
    background: linear-gradient(135deg, #dbeafe, #ede9fe);
    color: #6366f1;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-greeting {
  font-size: 0.9rem;
  color: #64748b;
  font-weight: 500;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border-radius: 10px;
  padding: 8px 16px;
  font-size: 0.88rem;
  font-weight: 500;
  color: #64748b;
  border: 1px solid #e2e8f0;
  transition: all 0.25s ease;

  &:hover {
    color: #ef4444;
    border-color: #fecaca;
    background: #fef2f2;
  }
}

.app-aside {
  width: 220px;
  padding: 0.75rem 0;
  border-right: 1px solid #f1f5f9;
}

.main-content {
  border-radius: 16px;
  overflow: auto;
  width: 100%;
  height: 100%;
  scrollbar-width: none;
  background: #fafbfc;
  margin: 0.5rem;
}

@media (max-width: 768px) {
  .app-aside {
    width: 160px;
  }

  .app-title {
    font-size: 1rem;
  }

  .user-greeting {
    display: none;
  }
}
</style>
