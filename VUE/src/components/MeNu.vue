<template>
    <div>
      <el-menu
       style="background-color: rgba(255, 255, 255, 0);"
       :router="true"
       default-active="0"
      >
      <el-menu-item class="menu-item" v-for="(item,index ) of props.routers" :key="item.router" :index="`${index}`" :route="item.router">{{item.name}}</el-menu-item>
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
.menu-item{
    border-radius: 1rem;
    margin: 1rem;
    background: vars.$card-bg;
    &:hover{
        background: vars.$card-bg-hover;
    }
}

.el-menu-item.is-active {
  background-color: #41a3fb !important; /* 激活状态的背景颜色 */
  color: #ffffff !important; /* 激活状态的字体颜色 */
}
</style>