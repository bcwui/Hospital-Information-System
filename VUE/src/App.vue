<script setup lang="ts">


</script>

<template>
  <div class="container">
    <router-view></router-view>
  </div>
</template>

<style scoped lang="scss">
.container {
  width: 100%;
  min-height: 100vh;
  position: relative;
  display: flex;
  flex-direction: column;
  background-image: url(./assets/bg.png);
  background-size: cover;
}
</style>
