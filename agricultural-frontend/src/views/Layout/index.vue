<template>
  <LayoutNav />
  <div class="main-content">
      <router-view />
  </div>
  <LayoutFooter />
</template>

<script setup lang="ts">
import LayoutNav from './components/LayoutNav.vue';
import LayoutFooter from './components/LayoutFooter.vue';
</script>

<style scoped>
.main-content {
    min-height: calc(100vh - 200px); /* Adjust based on header/footer height */
    padding-top: 64px; /* Header height */
}
</style>
