<script setup lang="ts">
import LogIn from '@/components/LogIn.vue';
import { reactive } from 'vue';
import RegiSter from '@/components/RegiSter.vue';

const data = reactive({
  isShow: 'login'
})
const handleT = (mess: 'login' | 'register') => {
  data.isShow = mess
}
</script>

<template>
  <div class="home">
    <div class="home-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>

    <div class="home-container">
      <div class="brand-section">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="14" fill="url(#icon-grad)"/>
            <path d="M14 24h20M24 14v20M18 20l6-6 6 6M18 28l6 6 6-6" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="icon-grad" x1="0" y1="0" x2="48" y2="48">
                <stop stop-color="#0ea5e9"/>
                <stop offset="1" stop-color="#6366f1"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1 class="brand-name">智慧医疗信息管理系统</h1>
        <p class="brand-desc">
          集预约挂号、AI健康咨询、智能排班于一体的<br/>新一代医疗服务平台
        </p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-dot"></span>
            在线预约
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            AI健康助手
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            智能排班
          </div>
        </div>
      </div>

      <div class="auth-section">
        <div class="auth-card">
          <log-in @turnLoR="handleT" v-show="data.isShow === 'login'"></log-in>
          <regi-ster @turnLoR="handleT" v-show="data.isShow === 'register'"></regi-ster>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.home {
  width: 100vw;
  height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #f0f9ff 0%, #ecfeff 30%, #f8fafc 60%, #faf5ff 100%);
}

.home-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
  filter: blur(60px);
}

.shape-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.25), transparent 70%);
  top: -150px;
  right: -150px;
}

.shape-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18), transparent 70%);
  bottom: -100px;
  left: -100px;
}

.shape-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.15), transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.home-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 4rem;
  max-width: 1100px;
  width: 100%;
  padding: 2rem;
}

.brand-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.75rem;
  padding-right: 2rem;
}

.brand-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 0.25rem;
  animation: float 3s ease-in-out infinite;

  svg {
    width: 100%;
    height: 100%;
    filter: drop-shadow(0 8px 16px rgba(14, 165, 233, 0.3));
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.brand-name {
  font-size: 2.25rem;
  font-weight: 800;
  background: linear-gradient(135deg, #0ea5e9, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.3;
  margin: 0;
}

.brand-desc {
  font-size: 1.05rem;
  color: #64748b;
  line-height: 1.7;
  margin: 0;
}

.brand-features {
  display: flex;
  gap: 1.5rem;
  margin-top: 0.5rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9rem;
  color: #475569;
  font-weight: 500;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  box-shadow: 0 0 8px rgba(14, 165, 233, 0.4);
}

.auth-section {
  width: 460px;
  flex-shrink: 0;
}

.auth-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 50px rgba(14, 165, 233, 0.1), 0 8px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.6);
  overflow: hidden;
}

@media (max-width: 900px) {
  .home-container {
    flex-direction: column;
    gap: 2rem;
    padding: 1.5rem;
  }

  .brand-section {
    align-items: center;
    text-align: center;
    padding-right: 0;
  }

  .brand-name {
    font-size: 1.75rem;
  }

  .auth-section {
    width: 100%;
    max-width: 460px;
  }
}
</style>
