<template>
  <div class="chat-page">
    <section class="main-shell utility-gap">
      <header class="page-head">
        <div>
          <p class="eyebrow">AI CONSULT</p>
          <h2>AI 健康咨询</h2>
          <p class="subtext">与AI助手进行健康咨询，获取初步诊断建议</p>
        </div>
      </header>
    </section>
    <div class="chat-wrapper">
      <ChatView :could-send="true" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import ChatView from '@/views/ChatView.vue';
</script>

<style lang="scss" scoped>
.chat-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-wrapper {
  flex: 1;
  min-height: 0;
  padding: 0 2rem 2rem 2rem;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .chat-wrapper {
    padding: 0 1rem 1rem 1rem;
  }
}
</style>
