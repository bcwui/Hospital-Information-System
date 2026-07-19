<template>
  <div v-if="sources && sources.length" class="ref-block">
    <div class="ref-toggle" @click="expanded = !expanded">
      <span>参考来源 ({{ sources.length }})</span>
      <svg :class="{ rotated: expanded }" viewBox="0 0 24 24" fill="none" width="14" height="14">
        <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <div v-show="expanded" class="ref-list">
      <div v-for="(source, i) in sources" :key="i" class="ref-item">
        <span class="ref-idx">[{{ i + 1 }}]</span>
        <div>
          <div class="ref-title">{{ source.title }}</div>
          <div class="ref-snippet">{{ source.snippet }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import type { RefSource } from '@/types/chat'

defineProps<{ sources: RefSource[] }>()
const expanded = ref(false)
</script>

<style lang="scss" scoped>
.ref-block { margin: 6px 0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
.ref-toggle {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 12px; cursor: pointer; font-size: 0.8rem; color: #64748b;
  background: #f8fafc;
  &:hover { background: #f1f5f9; }
  svg { transition: transform 0.2s; }
  svg.rotated { transform: rotate(180deg); }
}
.ref-list { padding: 8px 12px; display: flex; flex-direction: column; gap: 6px; }
.ref-item { display: flex; gap: 6px; font-size: 0.8rem; line-height: 1.5; }
.ref-idx { color: #0ea5e9; font-weight: 600; flex-shrink: 0; }
.ref-title { font-weight: 500; color: #334155; }
.ref-snippet { color: #94a3b8; font-size: 0.75rem; }
</style>
