<template>
  <div class="side-nav">
    <el-menu
      style="background-color: transparent; border-right: none;"
      :router="true"
      default-active="0"
    >
      <el-menu-item
        class="menu-item"
        v-for="(item, index) of props.routers"
        :key="item.router"
        :index="`${index}`"
        :route="item.router"
      >
        <span class="menu-icon">
          <svg v-if="item.name.includes('预约') || item.name.includes('挂号')" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.8"/>
            <path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <svg v-else-if="item.name.includes('科室') || item.name.includes('管理')" viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12h6v10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else-if="item.name.includes('AI') || item.name.includes('ai') || item.name.includes('助手')" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.8"/>
            <path d="M8 12h3l1.5-4 1.5 8 1.5-4H16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else-if="item.name.includes('信息') || item.name.includes('资料')" viewBox="0 0 24 24" fill="none">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="1.8"/>
          </svg>
          <svg v-else-if="item.name.includes('RAG') || item.name.includes('知识')" viewBox="0 0 24 24" fill="none">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.8"/>
            <path d="M12 6v6l4 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="menu-label">{{ item.name }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script lang="ts" setup>
import type { PropType } from 'vue';
const props = defineProps({
  routers: {
    type: Array as PropType<{ router: string; name: string }[]>,
    required: true,
  },
});
</script>

<style lang="scss" scoped>
.side-nav {
  padding: 0.75rem 0.5rem;
}

.menu-item {
  border-radius: 12px;
  margin: 0.35rem 0;
  padding: 0 1rem;
  height: 48px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: transparent;
  color: #475569;
  font-weight: 500;
  font-size: 0.93rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    background: linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(99, 102, 241, 0.06));
    color: #0ea5e9;
    transform: translateX(2px);
  }
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;

  svg {
    width: 22px;
    height: 22px;
  }
}

.menu-label {
  white-space: nowrap;
}

.el-menu-item.is-active {
  background: linear-gradient(135deg, #0ea5e9, #6366f1) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.35);
  font-weight: 600;

  .menu-icon svg {
    filter: drop-shadow(0 1px 2px rgba(255, 255, 255, 0.3));
  }

  &:hover {
    transform: none;
    background: linear-gradient(135deg, #0284c7, #4f46e5) !important;
  }
}
</style>
