<script setup lang="ts">
defineProps<{
  label: string;
  value: string;
  trend: string;
}>();
</script>

<template>
  <article class="metric-card">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small>{{ trend }}</small>
  </article>
</template>
