<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import AppLayout from "./components/AppLayout.vue";

const route = useRoute();
const usesAppShell = computed(() => route.meta.requiresAuth === true);
</script>

<template>
  <AppLayout v-if="usesAppShell">
    <RouterView />
  </AppLayout>
  <RouterView v-else />
</template>