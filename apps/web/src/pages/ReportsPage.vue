<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  exportHtmlReport,
  getReportSummary,
  getTestRunReport,
  type ReportSummaryResponse,
  type TestRunReportResponse
} from "../app/api/reports";
import { useAuthStore } from "../app/stores/auth";

const route = useRoute();
const auth = useAuthStore();
const { token } = storeToRefs(auth);

const projectId = computed(() => String(route.params.projectId ?? "demo"));
const summary = ref<ReportSummaryResponse | null>(null);
const report = ref<TestRunReportResponse | null>(null);
const htmlPreview = ref("");
const loading = ref(false);
const exporting = ref(false);
const pageError = ref("");

const latestRun = computed(() => summary.value?.latestRun ?? null);
const passRate = computed(() => report.value?.passRate ?? latestRun.value?.passRate ?? 0);
const totalCases = computed(() => report.value?.totalCases ?? latestRun.value?.totalCases ?? 0);
const failedCases = computed(() => report.value?.failedCases ?? []);
const linkedDefects = computed(() => report.value?.linkedDefects ?? []);

onMounted(() => {
  void loadReport();
});

watch(projectId, () => {
  summary.value = null;
  report.value = null;
  htmlPreview.value = "";
  void loadReport();
});

async function loadReport() {
  if (!token.value || !projectId.value) {
    summary.value = null;
    report.value = null;
    return;
  }

  loading.value = true;
  pageError.value = "";
  try {
    summary.value = await getReportSummary(token.value, projectId.value);
    if (summary.value.latestRun) {
      report.value = await getTestRunReport(token.value, projectId.value, summary.value.latestRun.id);
    }
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Report data could not be loaded.";
  } finally {
    loading.value = false;
  }
}

async function exportReport() {
  if (!token.value || !projectId.value) {
    return;
  }

  exporting.value = true;
  pageError.value = "";
  try {
    htmlPreview.value = await exportHtmlReport(token.value, projectId.value);
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Report export could not be generated.";
  } finally {
    exporting.value = false;
  }
}
</script>

<template>
  <section class="page-grid">
    <div class="page-heading">
      <p class="eyebrow">Reports</p>
      <h2>Quality report preview</h2>
      <p>Generate HTML reports with execution totals, pass rate, failed cases, and linked defects.</p>
    </div>

    <p v-if="pageError" class="form-error">{{ pageError }}</p>

    <div class="metric-grid">
      <article class="metric-card" data-test="report-pass-rate">
        <span>Pass rate</span>
        <strong>{{ passRate }}%</strong>
        <small>{{ latestRun?.name ?? "No completed run" }}</small>
      </article>
      <article class="metric-card">
        <span>Total cases</span>
        <strong>{{ totalCases }}</strong>
        <small>{{ summary?.readyTestCases ?? 0 }} ready</small>
      </article>
      <article class="metric-card">
        <span>Failed cases</span>
        <strong>{{ report?.failed ?? latestRun?.failed ?? 0 }}</strong>
        <small>{{ report?.blocked ?? latestRun?.blocked ?? 0 }} blocked</small>
      </article>
      <article class="metric-card">
        <span>Open defects</span>
        <strong>{{ summary?.openDefects ?? 0 }}</strong>
        <small>{{ summary?.criticalDefects ?? 0 }} critical</small>
      </article>
    </div>

    <article class="report-preview">
      <div class="panel-heading flush-heading">
        <div>
          <p class="eyebrow">{{ summary?.projectName ?? "Project" }}</p>
          <h3>{{ report?.testRunName ?? latestRun?.name ?? "No report data" }}</h3>
        </div>
        <button type="button" data-test="export-html" :disabled="exporting || !summary" @click="exportReport">
          {{ exporting ? "Exporting..." : "Export HTML" }}
        </button>
      </div>

      <section class="report-section">
        <h4>Failed cases</h4>
        <div v-if="failedCases.length" class="table-surface">
          <div class="table-row header report-row">
            <span>Case</span>
            <span>Title</span>
            <span>Actual result</span>
          </div>
          <div v-for="failedCase in failedCases" :key="failedCase.testRunItemId" class="table-row report-row">
            <strong>{{ failedCase.caseKey }}</strong>
            <span>{{ failedCase.title }}</span>
            <span>{{ failedCase.actualResult ?? "No result note" }}</span>
          </div>
        </div>
        <p v-else class="compact-empty">No failed cases.</p>
      </section>

      <section class="report-section">
        <h4>Linked defects</h4>
        <div v-if="linkedDefects.length" class="table-surface">
          <div class="table-row header report-row">
            <span>Case</span>
            <span>Defect</span>
            <span>Status</span>
          </div>
          <div v-for="defect in linkedDefects" :key="defect.id" class="table-row report-row">
            <strong>{{ defect.caseKey }}</strong>
            <span>{{ defect.title }}</span>
            <span class="status" :class="defect.status.toLowerCase()">{{ defect.status }}</span>
          </div>
        </div>
        <p v-else class="compact-empty">No linked defects.</p>
      </section>
    </article>

    <section v-if="htmlPreview" class="workflow-panel">
      <div class="panel-heading flush-heading">
        <div>
          <p class="eyebrow">HTML export</p>
          <h3>Preview source</h3>
        </div>
      </div>
      <pre data-test="html-preview" class="html-preview">{{ htmlPreview }}</pre>
    </section>

    <section v-if="loading" class="workflow-panel compact-empty">Loading report...</section>
  </section>
</template>
