<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { getDashboard, type DashboardResponse, type DefectStatus, type TestRunItemResult } from "../app/api/reports";
import { useAuthStore } from "../app/stores/auth";
import MetricCard from "../components/MetricCard.vue";

const defectStatuses: DefectStatus[] = ["OPEN", "IN_PROGRESS", "REOPENED", "RESOLVED", "CLOSED"];
const testResults: TestRunItemResult[] = ["PASSED", "FAILED", "BLOCKED", "SKIPPED", "UNTESTED"];

const route = useRoute();
const auth = useAuthStore();
const { token } = storeToRefs(auth);

const projectId = computed(() => String(route.params.projectId ?? "demo"));
const dashboard = ref<DashboardResponse | null>(null);
const loading = ref(false);
const pageError = ref("");

const resultTotal = computed(() => testResults.reduce((total, result) => total + countResult(result), 0));
const executedTotal = computed(() => resultTotal.value - countResult("UNTESTED"));
const runProgress = computed(() =>
  resultTotal.value === 0 ? 0 : Math.round((executedTotal.value / resultTotal.value) * 100)
);
const defectTotal = computed(() => defectStatuses.reduce((total, status) => total + countDefects(status), 0));

const metrics = computed(() => [
  {
    label: "Ready test cases",
    value: String(dashboard.value?.readyTestCases ?? 0),
    trend: `${dashboard.value?.totalTestCases ?? 0} total`,
    testId: "ready-test-cases"
  },
  {
    label: "Latest pass rate",
    value: `${dashboard.value?.latestPassRate ?? 0}%`,
    trend: "last completed run",
    testId: "latest-pass-rate"
  },
  {
    label: "Open defects",
    value: String(dashboard.value?.openDefects ?? 0),
    trend: `${dashboard.value?.criticalDefects ?? 0} critical`,
    testId: "open-defects"
  },
  {
    label: "Active test runs",
    value: String(dashboard.value?.activeTestRuns ?? 0),
    trend: `${runProgress.value}% executed`,
    testId: "active-test-runs"
  }
]);

onMounted(() => {
  void loadDashboard();
});

watch(projectId, () => {
  dashboard.value = null;
  void loadDashboard();
});

async function loadDashboard() {
  if (!token.value || !projectId.value) {
    dashboard.value = null;
    return;
  }

  loading.value = true;
  pageError.value = "";
  try {
    dashboard.value = await getDashboard(token.value, projectId.value);
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Dashboard could not be loaded.";
  } finally {
    loading.value = false;
  }
}

function countDefects(status: DefectStatus) {
  return dashboard.value?.defectsByStatus?.[status] ?? 0;
}

function countResult(result: TestRunItemResult) {
  return dashboard.value?.testResults?.[result] ?? 0;
}

function width(value: number, total: number) {
  return `${total === 0 ? 0 : Math.round((value / total) * 100)}%`;
}

function label(value: string) {
  return value.replaceAll("_", " ").toLowerCase();
}
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
        :data-test="metric.testId"
        :label="metric.label"
        :value="metric.value"
        :trend="metric.trend"
      />
    </div>

    <p v-if="pageError" class="form-error">{{ pageError }}</p>

    <section class="dashboard-panels">
      <article class="workflow-panel">
        <div class="panel-heading flush-heading">
          <div>
            <p class="eyebrow">Execution</p>
            <h3>Run progress</h3>
          </div>
          <strong data-test="run-progress">{{ runProgress }}%</strong>
        </div>
        <div class="progress-track dashboard-progress" aria-label="Run progress">
          <span :style="{ width: `${runProgress}%` }"></span>
        </div>
        <div class="chart-list">
          <div v-for="result in testResults" :key="result" class="chart-row" :data-test="`test-result-${result}`">
            <span>{{ label(result) }}</span>
            <div class="mini-track"><span :style="{ width: width(countResult(result), resultTotal) }"></span></div>
            <strong>{{ countResult(result) }}</strong>
          </div>
        </div>
      </article>

      <article class="workflow-panel">
        <div class="panel-heading flush-heading">
          <div>
            <p class="eyebrow">Defects</p>
            <h3>Status distribution</h3>
          </div>
          <strong>{{ dashboard?.openDefects ?? 0 }} open</strong>
        </div>
        <div class="chart-list">
          <div v-for="status in defectStatuses" :key="status" class="chart-row" :data-test="`defect-status-${status}`">
            <span>{{ label(status) }}</span>
            <div class="mini-track defect-track">
              <span :style="{ width: width(countDefects(status), defectTotal) }"></span>
            </div>
            <strong>{{ countDefects(status) }}</strong>
          </div>
        </div>
      </article>
    </section>

    <section v-if="loading" class="workflow-panel compact-empty">Loading dashboard...</section>
  </section>
</template>
