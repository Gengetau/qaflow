<script setup lang="ts">
import MetricCard from "../components/MetricCard.vue";

const metrics = [
  { label: "Ready test cases", value: "128", trend: "+12 this week" },
  { label: "Latest pass rate", value: "84%", trend: "last run" },
  { label: "Open defects", value: "17", trend: "5 critical" },
  { label: "Active test runs", value: "3", trend: "2 in progress" }
];
</script>

<template>
  <section class="page-grid">
    <div class="page-heading">
      <p class="eyebrow">Project dashboard</p>
      <h2>Release quality snapshot</h2>
      <p>Track execution progress, open risk, and report readiness for the active project.</p>
    </div>

    <div class="metric-grid">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :trend="metric.trend"
      />
    </div>

    <section class="workflow-panel">
      <h3>Execution workflow</h3>
      <ol>
        <li>Create suites and test cases</li>
        <li>Create a test run from selected cases</li>
        <li>Mark items as passed, failed, blocked, or skipped</li>
        <li>Create defects from failed items</li>
        <li>Export a quality report</li>
      </ol>
    </section>
  </section>
</template>
