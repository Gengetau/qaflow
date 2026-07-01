<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  createDefectFromRunItem,
  type DefectPriority,
  type DefectSeverity
} from "../app/api/defects";
import { listTestCases, type TestCase } from "../app/api/testCases";
import {
  completeTestRun,
  createTestRun,
  listTestRuns,
  startTestRun,
  updateTestRunItemResult,
  type TestRun,
  type TestRunItem,
  type TestRunItemResult
} from "../app/api/testRuns";
import { useAuthStore } from "../app/stores/auth";

const resultOptions: Exclude<TestRunItemResult, "UNTESTED">[] = ["PASSED", "FAILED", "BLOCKED", "SKIPPED"];

const route = useRoute();
const auth = useAuthStore();
const { role, token } = storeToRefs(auth);

const projectId = computed(() => String(route.params.projectId ?? ""));
const runs = ref<TestRun[]>([]);
const selectedRunId = ref("");
const readyCases = ref<TestCase[]>([]);
const selectedCaseIds = ref<string[]>([]);
const loadingRuns = ref(false);
const loadingCases = ref(false);
const submitting = ref(false);
const pageError = ref("");
const createOpen = ref(false);
const defectEditorOpen = ref(false);
const defectSourceItem = ref<TestRunItem | null>(null);
const createdDefectNotice = ref("");
const resultNotes = reactive<Record<string, string>>({});

const runForm = reactive({
  name: "",
  description: ""
});

const defectForm = reactive({
  title: "",
  description: "",
  severity: "HIGH" as DefectSeverity,
  priority: "URGENT" as DefectPriority
});

const canWrite = computed(() => role.value === "OWNER" || role.value === "TESTER");
const selectedRun = computed(() => runs.value.find((run) => run.id === selectedRunId.value) ?? null);
const totalItems = computed(() => selectedRun.value?.items.length ?? 0);
const completedItems = computed(
  () => selectedRun.value?.items.filter((item) => item.result !== "UNTESTED").length ?? 0
);
const progressPercent = computed(() =>
  totalItems.value === 0 ? 0 : Math.round((completedItems.value / totalItems.value) * 100)
);
const resultCounts = computed(() => {
  const counts: Record<TestRunItemResult, number> = {
    UNTESTED: 0,
    PASSED: 0,
    FAILED: 0,
    BLOCKED: 0,
    SKIPPED: 0
  };
  selectedRun.value?.items.forEach((item) => {
    counts[item.result] += 1;
  });
  return counts;
});

onMounted(() => {
  void loadRuns();
});

watch(projectId, () => {
  selectedRunId.value = "";
  runs.value = [];
  void loadRuns();
});

watch(selectedRun, (run) => {
  if (!run) {
    return;
  }
  run.items.forEach((item) => {
    resultNotes[item.id] = item.actualResult ?? resultNotes[item.id] ?? "";
  });
});

async function loadRuns() {
  if (!token.value || !projectId.value) {
    runs.value = [];
    return;
  }

  loadingRuns.value = true;
  pageError.value = "";
  try {
    runs.value = await listTestRuns(token.value, projectId.value);
    if (!runs.value.some((run) => run.id === selectedRunId.value)) {
      selectedRunId.value = runs.value[0]?.id ?? "";
    }
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Test runs could not be loaded.";
  } finally {
    loadingRuns.value = false;
  }
}

async function openCreateRun() {
  runForm.name = "";
  runForm.description = "";
  selectedCaseIds.value = [];
  createOpen.value = true;
  await loadReadyCases();
}

async function loadReadyCases() {
  if (!token.value || !projectId.value) {
    readyCases.value = [];
    return;
  }

  loadingCases.value = true;
  pageError.value = "";
  try {
    const page = await listTestCases(token.value, projectId.value, { query: "", status: "READY", priority: "" }, 0, 100);
    readyCases.value = page.items;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Ready test cases could not be loaded.";
  } finally {
    loadingCases.value = false;
  }
}

async function saveRun() {
  if (!token.value || !projectId.value || selectedCaseIds.value.length === 0) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    const created = await createTestRun(token.value, projectId.value, {
      name: runForm.name.trim(),
      description: runForm.description.trim(),
      testCaseIds: selectedCaseIds.value
    });
    runs.value = [created, ...runs.value];
    selectedRunId.value = created.id;
    createOpen.value = false;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Test run could not be created.";
  } finally {
    submitting.value = false;
  }
}

async function startSelectedRun() {
  if (!token.value || !selectedRun.value) {
    return;
  }

  await mutateRun(() => startTestRun(token.value, selectedRun.value!.id), "Test run could not be started.");
}

async function completeSelectedRun() {
  if (!token.value || !selectedRun.value) {
    return;
  }

  await mutateRun(() => completeTestRun(token.value, selectedRun.value!.id), "Test run could not be completed.");
}

async function markResult(item: TestRunItem, result: Exclude<TestRunItemResult, "UNTESTED">) {
  if (!token.value || !selectedRun.value || selectedRun.value.status !== "IN_PROGRESS") {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    const updatedItem = await updateTestRunItemResult(token.value, item.id, {
      result,
      actualResult: (resultNotes[item.id] ?? "").trim()
    });
    replaceRun({
      ...selectedRun.value,
      items: selectedRun.value.items.map((runItem) => (runItem.id === updatedItem.id ? updatedItem : runItem)),
      updatedAt: updatedItem.updatedAt
    });
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Result could not be saved.";
  } finally {
    submitting.value = false;
  }
}

function openDefectFromItem(item: TestRunItem) {
  defectSourceItem.value = item;
  defectForm.title = `${item.caseKey}: ${item.title}`;
  defectForm.description = resultNotes[item.id] ?? item.actualResult ?? "";
  defectForm.severity = "HIGH";
  defectForm.priority = "URGENT";
  defectEditorOpen.value = true;
}

async function saveDefectFromItem() {
  if (!token.value || !defectSourceItem.value) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  createdDefectNotice.value = "";
  try {
    const created = await createDefectFromRunItem(token.value, defectSourceItem.value.id, {
      title: defectForm.title.trim(),
      description: defectForm.description.trim(),
      severity: defectForm.severity,
      priority: defectForm.priority,
      assigneeId: null
    });
    createdDefectNotice.value = `Defect created: ${created.title}`;
    defectEditorOpen.value = false;
    defectSourceItem.value = null;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Defect could not be created.";
  } finally {
    submitting.value = false;
  }
}

async function mutateRun(action: () => Promise<TestRun>, fallbackMessage: string) {
  submitting.value = true;
  pageError.value = "";
  try {
    replaceRun(await action());
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : fallbackMessage;
  } finally {
    submitting.value = false;
  }
}

function replaceRun(updatedRun: TestRun) {
  runs.value = runs.value.map((run) => (run.id === updatedRun.id ? updatedRun : run));
  selectedRunId.value = updatedRun.id;
}
</script>

<template>
  <section class="page-grid test-runs-page">
    <div class="page-heading action-heading">
      <div>
        <p class="eyebrow">Test runs</p>
        <h2>Execution board</h2>
        <p>Plan runs from ready cases, execute each item, and keep progress visible while testing.</p>
      </div>
      <button v-if="canWrite" data-test="new-run" type="button" @click="openCreateRun">New run</button>
    </div>

    <p v-if="pageError" class="form-error">{{ pageError }}</p>
    <p v-if="createdDefectNotice" class="form-success" data-test="run-defect-notice">{{ createdDefectNotice }}</p>

    <div class="project-layout test-run-layout">
      <section class="table-surface">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Project runs</p>
            <h3>{{ runs.length }} total</h3>
          </div>
        </div>
        <div v-if="loadingRuns" class="empty-state">Loading test runs...</div>
        <div v-else-if="runs.length === 0" class="empty-state">
          <h3>No test runs yet</h3>
          <p>Create a run from ready cases when a release candidate needs verification.</p>
        </div>
        <div v-else>
          <div class="table-row header run-row">
            <span>Run</span>
            <span>Status</span>
            <span>Progress</span>
          </div>
          <button
            v-for="run in runs"
            :key="run.id"
            class="table-row run-row selectable-row"
            :class="{ selected: run.id === selectedRunId }"
            type="button"
            @click="selectedRunId = run.id"
          >
            <span>
              <strong>{{ run.name }}</strong>
              <small>{{ run.description || "No description" }}</small>
            </span>
            <span class="status" :class="run.status.toLowerCase()">{{ run.status }}</span>
            <span>{{ run.items.filter((item) => item.result !== "UNTESTED").length }} / {{ run.items.length }}</span>
          </button>
        </div>
      </section>

      <section class="table-surface run-detail-panel">
        <div v-if="!selectedRun" class="empty-state detail-empty">
          <h3>Select a test run</h3>
          <p>Execution controls and result history appear here.</p>
        </div>
        <template v-else>
          <div class="panel-heading run-detail-heading">
            <div>
              <p class="eyebrow">Execution</p>
              <h3>{{ selectedRun.name }}</h3>
              <p>{{ selectedRun.description || "No description set." }}</p>
            </div>
            <div v-if="canWrite" class="button-row end">
              <button
                v-if="selectedRun.status === 'PLANNED'"
                data-test="start-run"
                type="button"
                :disabled="submitting"
                @click="startSelectedRun"
              >
                Start
              </button>
              <button
                v-if="selectedRun.status === 'IN_PROGRESS'"
                class="secondary-button"
                data-test="complete-run"
                type="button"
                :disabled="submitting"
                @click="completeSelectedRun"
              >
                Complete
              </button>
            </div>
          </div>

          <div class="run-progress" data-test="run-progress">
            <div>
              <strong>{{ progressPercent }}%</strong>
              <span>{{ completedItems }} of {{ totalItems }} items executed</span>
            </div>
            <div class="progress-track" aria-hidden="true">
              <span :style="{ width: `${progressPercent}%` }"></span>
            </div>
          </div>

          <div class="execution-panel run-metrics">
            <div>
              <span class="status passed">PASSED</span>
              <strong>{{ resultCounts.PASSED }}</strong>
            </div>
            <div>
              <span class="status failed">FAILED</span>
              <strong>{{ resultCounts.FAILED }}</strong>
            </div>
            <div>
              <span class="status blocked">BLOCKED</span>
              <strong>{{ resultCounts.BLOCKED }}</strong>
            </div>
            <div>
              <span class="status skipped">SKIPPED</span>
              <strong>{{ resultCounts.SKIPPED }}</strong>
            </div>
          </div>

          <div>
            <div class="table-row header run-item-row">
              <span>Case</span>
              <span>Result</span>
              <span>Actual result</span>
              <span>Actions</span>
            </div>
            <div v-for="item in selectedRun.items" :key="item.id" class="table-row run-item-row">
              <span>
                <strong>{{ item.caseKey }}</strong>
                <small>{{ item.title }}</small>
              </span>
              <span class="status" :class="item.result.toLowerCase()">{{ item.result }}</span>
              <span>
                <textarea
                  :data-test="`item-result-note-${item.id}`"
                  v-model="resultNotes[item.id]"
                  rows="2"
                  :disabled="selectedRun.status !== 'IN_PROGRESS'"
                  placeholder="Actual result"
                />
              </span>
              <span class="result-button-row">
                <button
                  v-for="result in resultOptions"
                  :key="result"
                  class="secondary-button"
                  :data-test="`mark-${item.id}-${result}`"
                  type="button"
                  :disabled="submitting || selectedRun.status !== 'IN_PROGRESS'"
                  @click="markResult(item, result)"
                >
                  {{ result }}
                </button>
                <button
                  v-if="canWrite && item.result === 'FAILED'"
                  class="secondary-button"
                  :data-test="`create-defect-${item.id}`"
                  type="button"
                  :disabled="submitting"
                  @click="openDefectFromItem(item)"
                >
                  Create defect
                </button>
              </span>
            </div>
          </div>
        </template>
      </section>
    </div>

    <div v-if="createOpen" class="modal-backdrop">
      <form class="modal run-create-modal" data-test="create-run-form" @submit.prevent="saveRun">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Test run</p>
            <h3>New execution run</h3>
          </div>
          <button class="ghost-button" type="button" @click="createOpen = false">Close</button>
        </div>
        <label>
          Name
          <input data-test="run-name-input" v-model="runForm.name" required maxlength="180" />
        </label>
        <label>
          Description
          <textarea v-model="runForm.description" rows="3" />
        </label>

        <div class="suite-toolbar">
          <div>
            <p class="eyebrow">Ready cases</p>
            <h3>{{ selectedCaseIds.length }} selected</h3>
          </div>
        </div>
        <div v-if="loadingCases" class="empty-state">Loading ready cases...</div>
        <div v-else-if="readyCases.length === 0" class="empty-state">
          <h3>No ready cases</h3>
          <p>Mark test cases as ready before creating a run.</p>
        </div>
        <div v-else class="case-picker-list">
          <label v-for="testCase in readyCases" :key="testCase.id" class="case-picker-row">
            <input
              :data-test="`case-checkbox-${testCase.id}`"
              v-model="selectedCaseIds"
              type="checkbox"
              :value="testCase.id"
            />
            <span>
              <strong>{{ testCase.caseKey }}</strong>
              <small>{{ testCase.title }}</small>
            </span>
            <span class="priority" :class="testCase.priority.toLowerCase()">{{ testCase.priority }}</span>
          </label>
        </div>

        <div class="button-row end">
          <button class="secondary-button" type="button" @click="createOpen = false">Cancel</button>
          <button type="submit" :disabled="submitting || selectedCaseIds.length === 0">Create run</button>
        </div>
      </form>
    </div>

    <div v-if="defectEditorOpen" class="modal-backdrop">
      <form class="modal defect-editor" data-test="run-item-defect-form" @submit.prevent="saveDefectFromItem">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Defect</p>
            <h3>Create defect from failed item</h3>
          </div>
          <button class="ghost-button" type="button" @click="defectEditorOpen = false">Close</button>
        </div>
        <label>
          Title
          <input data-test="run-item-defect-title-input" v-model="defectForm.title" required maxlength="240" />
        </label>
        <label>
          Description
          <textarea data-test="run-item-defect-description-input" v-model="defectForm.description" rows="4" />
        </label>
        <div class="case-editor-grid">
          <label>
            Severity
            <select data-test="run-item-defect-severity-input" v-model="defectForm.severity">
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </label>
          <label>
            Priority
            <select data-test="run-item-defect-priority-input" v-model="defectForm.priority">
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="URGENT">URGENT</option>
            </select>
          </label>
        </div>

        <div class="button-row end">
          <button class="secondary-button" type="button" @click="defectEditorOpen = false">Cancel</button>
          <button type="submit" :disabled="submitting">Save defect</button>
        </div>
      </form>
    </div>
  </section>
</template>
