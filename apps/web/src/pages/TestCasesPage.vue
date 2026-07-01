<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  createTestCase,
  getTestCase,
  listTestCases,
  updateTestCase,
  type TestCase,
  type TestCasePriority,
  type TestCaseStatus,
  type TestCaseType
} from "../app/api/testCases";
import { useAuthStore } from "../app/stores/auth";

interface StepForm {
  stepOrder: number;
  action: string;
  expectedResult: string;
}

const route = useRoute();
const auth = useAuthStore();
const { role, token } = storeToRefs(auth);

const projectId = computed(() => String(route.params.projectId ?? ""));
const cases = ref<TestCase[]>([]);
const selectedCaseId = ref("");
const selectedCaseDetail = ref<TestCase | null>(null);
const totalCases = ref(0);
const loadingCases = ref(false);
const loadingDetail = ref(false);
const submitting = ref(false);
const pageError = ref("");
const editorOpen = ref(false);
const editingCaseId = ref("");

const filters = reactive({
  query: "",
  status: "" as "" | TestCaseStatus,
  priority: "" as "" | TestCasePriority
});

const caseForm = reactive({
  suiteId: "",
  caseKey: "",
  title: "",
  description: "",
  preconditions: "",
  priority: "MEDIUM" as TestCasePriority,
  type: "FUNCTIONAL" as TestCaseType,
  status: "DRAFT" as TestCaseStatus,
  steps: [{ stepOrder: 1, action: "", expectedResult: "" }] as StepForm[]
});

const canWrite = computed(() => role.value === "OWNER" || role.value === "TESTER");
const editorTitle = computed(() => (editingCaseId.value ? "Edit test case" : "New test case"));

onMounted(() => {
  void loadCases();
});

watch(projectId, () => {
  selectedCaseId.value = "";
  selectedCaseDetail.value = null;
  void loadCases();
});

watch(selectedCaseId, (caseId) => {
  if (!caseId) {
    selectedCaseDetail.value = null;
    return;
  }
  if (selectedCaseDetail.value?.id === caseId) {
    return;
  }
  void loadCaseDetail(caseId);
});

async function loadCases() {
  if (!token.value || !projectId.value) {
    cases.value = [];
    return;
  }

  loadingCases.value = true;
  pageError.value = "";
  try {
    const page = await listTestCases(token.value, projectId.value, filters);
    cases.value = page.items;
    totalCases.value = page.totalItems;

    if (!cases.value.some((testCase) => testCase.id === selectedCaseId.value)) {
      selectedCaseDetail.value = null;
      selectedCaseId.value = cases.value[0]?.id ?? "";
    }
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Test cases could not be loaded.";
  } finally {
    loadingCases.value = false;
  }
}

async function loadCaseDetail(caseId: string) {
  if (!token.value) {
    return;
  }

  loadingDetail.value = true;
  pageError.value = "";
  try {
    selectedCaseDetail.value = await getTestCase(token.value, caseId);
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Test case detail could not be loaded.";
  } finally {
    loadingDetail.value = false;
  }
}

function openNewCase() {
  editingCaseId.value = "";
  caseForm.suiteId = "";
  caseForm.caseKey = "";
  caseForm.title = "";
  caseForm.description = "";
  caseForm.preconditions = "";
  caseForm.priority = "MEDIUM";
  caseForm.type = "FUNCTIONAL";
  caseForm.status = "DRAFT";
  caseForm.steps = [{ stepOrder: 1, action: "", expectedResult: "" }];
  editorOpen.value = true;
}

function openEditCase() {
  if (!selectedCaseDetail.value) {
    return;
  }

  const testCase = selectedCaseDetail.value;
  editingCaseId.value = testCase.id;
  caseForm.suiteId = testCase.suiteId ?? "";
  caseForm.caseKey = testCase.caseKey;
  caseForm.title = testCase.title;
  caseForm.description = testCase.description ?? "";
  caseForm.preconditions = testCase.preconditions ?? "";
  caseForm.priority = testCase.priority;
  caseForm.type = testCase.type;
  caseForm.status = testCase.status;
  caseForm.steps = testCase.steps.map((step) => ({
    stepOrder: step.stepOrder,
    action: step.action,
    expectedResult: step.expectedResult
  }));
  editorOpen.value = true;
}

function addStep() {
  caseForm.steps.push({
    stepOrder: caseForm.steps.length + 1,
    action: "",
    expectedResult: ""
  });
}

function removeStep(index: number) {
  if (caseForm.steps.length === 1) {
    return;
  }
  caseForm.steps.splice(index, 1);
  caseForm.steps.forEach((step, stepIndex) => {
    step.stepOrder = stepIndex + 1;
  });
}

async function saveCase() {
  if (!token.value || !projectId.value) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  const payload = {
    suiteId: caseForm.suiteId.trim() || null,
    caseKey: caseForm.caseKey.trim(),
    title: caseForm.title.trim(),
    description: caseForm.description.trim(),
    preconditions: caseForm.preconditions.trim(),
    priority: caseForm.priority,
    type: caseForm.type,
    status: caseForm.status,
    steps: caseForm.steps.map((step, index) => ({
      stepOrder: index + 1,
      action: step.action.trim(),
      expectedResult: step.expectedResult.trim()
    }))
  };

  try {
    if (editingCaseId.value) {
      const updated = await updateTestCase(token.value, editingCaseId.value, payload);
      cases.value = cases.value.map((testCase) => (testCase.id === updated.id ? updated : testCase));
      selectedCaseDetail.value = updated;
      selectedCaseId.value = updated.id;
    } else {
      const created = await createTestCase(token.value, projectId.value, payload);
      cases.value = [created, ...cases.value];
      totalCases.value += 1;
      selectedCaseDetail.value = created;
      selectedCaseId.value = created.id;
    }
    editorOpen.value = false;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Test case could not be saved.";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page-grid test-cases-page">
    <div class="page-heading action-heading">
      <div>
        <p class="eyebrow">Test cases</p>
        <h2>Reusable verification assets</h2>
        <p>Author test cases with priority, type, status, preconditions, and ordered steps.</p>
      </div>
      <button v-if="canWrite" data-test="new-case" type="button" @click="openNewCase">New case</button>
    </div>

    <p v-if="pageError" class="form-error">{{ pageError }}</p>

    <form class="table-surface case-filter-bar" data-test="case-filter-form" @submit.prevent="loadCases">
      <label>
        Search
        <input data-test="case-query" v-model="filters.query" placeholder="Case key, title, or description" />
      </label>
      <label>
        Status
        <select data-test="case-status" v-model="filters.status">
          <option value="">Any status</option>
          <option value="DRAFT">DRAFT</option>
          <option value="READY">READY</option>
          <option value="DEPRECATED">DEPRECATED</option>
        </select>
      </label>
      <label>
        Priority
        <select data-test="case-priority" v-model="filters.priority">
          <option value="">Any priority</option>
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
      </label>
      <button type="submit">Apply</button>
    </form>

    <div class="project-layout">
      <section class="table-surface">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Project cases</p>
            <h3>{{ totalCases }} total</h3>
          </div>
        </div>
        <div v-if="loadingCases" class="empty-state">Loading test cases...</div>
        <div v-else-if="cases.length === 0" class="empty-state">
          <h3>No test cases yet</h3>
          <p>Create the first reusable verification asset for this project.</p>
        </div>
        <div v-else>
          <div class="table-row header case-row">
            <span>Case</span>
            <span>Title</span>
            <span>Priority</span>
          </div>
          <button
            v-for="testCase in cases"
            :key="testCase.id"
            class="table-row case-row selectable-row"
            :class="{ selected: testCase.id === selectedCaseId }"
            type="button"
            @click="selectedCaseId = testCase.id"
          >
            <span>{{ testCase.caseKey }}</span>
            <span>{{ testCase.title }}</span>
            <span class="priority" :class="testCase.priority.toLowerCase()">{{ testCase.priority }}</span>
          </button>
        </div>
      </section>

      <section class="table-surface case-detail-panel">
        <div v-if="loadingDetail" class="empty-state">Loading detail...</div>
        <div v-else-if="!selectedCaseDetail" class="empty-state detail-empty">
          <h3>Select a test case</h3>
          <p>Steps, preconditions, and editor actions appear here.</p>
        </div>
        <template v-else>
          <div class="panel-heading">
            <div>
              <p class="eyebrow">{{ selectedCaseDetail.caseKey }}</p>
              <h3>{{ selectedCaseDetail.title }}</h3>
              <p>{{ selectedCaseDetail.description || "No description set." }}</p>
            </div>
            <button v-if="canWrite" class="secondary-button" data-test="edit-case" type="button" @click="openEditCase">
              Edit
            </button>
          </div>
          <div class="case-meta-grid">
            <span class="status" :class="selectedCaseDetail.status.toLowerCase()">{{ selectedCaseDetail.status }}</span>
            <span class="priority" :class="selectedCaseDetail.priority.toLowerCase()">
              {{ selectedCaseDetail.priority }}
            </span>
            <span>{{ selectedCaseDetail.type }}</span>
          </div>
          <div class="case-detail-copy">
            <p class="eyebrow">Preconditions</p>
            <p>{{ selectedCaseDetail.preconditions || "No preconditions set." }}</p>
          </div>
          <div>
            <div class="table-row header step-row">
              <span>#</span>
              <span>Action</span>
              <span>Expected</span>
            </div>
            <div v-for="step in selectedCaseDetail.steps" :key="step.id ?? step.stepOrder" class="table-row step-row">
              <span>{{ step.stepOrder }}</span>
              <span>{{ step.action }}</span>
              <span>{{ step.expectedResult }}</span>
            </div>
          </div>
        </template>
      </section>
    </div>

    <div v-if="editorOpen" class="modal-backdrop">
      <form class="modal case-editor" data-test="case-editor-form" @submit.prevent="saveCase">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Test case</p>
            <h3>{{ editorTitle }}</h3>
          </div>
          <button class="ghost-button" type="button" @click="editorOpen = false">Close</button>
        </div>

        <label>
          Case key
          <input data-test="case-key-input" v-model="caseForm.caseKey" required maxlength="80" />
        </label>
        <label>
          Title
          <input data-test="case-title-input" v-model="caseForm.title" required maxlength="240" />
        </label>
        <label>
          Description
          <textarea v-model="caseForm.description" rows="3" />
        </label>
        <label>
          Preconditions
          <textarea v-model="caseForm.preconditions" rows="3" />
        </label>
        <div class="case-editor-grid">
          <label>
            Priority
            <select data-test="case-priority-input" v-model="caseForm.priority">
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </label>
          <label>
            Type
            <select data-test="case-type-input" v-model="caseForm.type">
              <option value="FUNCTIONAL">FUNCTIONAL</option>
              <option value="REGRESSION">REGRESSION</option>
              <option value="SMOKE">SMOKE</option>
              <option value="EXPLORATORY">EXPLORATORY</option>
            </select>
          </label>
          <label>
            Status
            <select data-test="case-status-input" v-model="caseForm.status">
              <option value="DRAFT">DRAFT</option>
              <option value="READY">READY</option>
              <option value="DEPRECATED">DEPRECATED</option>
            </select>
          </label>
        </div>

        <div class="suite-toolbar">
          <div>
            <p class="eyebrow">Steps</p>
            <h3>{{ caseForm.steps.length }} steps</h3>
          </div>
          <button class="secondary-button" type="button" @click="addStep">Add step</button>
        </div>

        <div class="step-editor-list">
          <div v-for="(step, index) in caseForm.steps" :key="index" class="step-editor">
            <span>{{ index + 1 }}</span>
            <label>
              Action
              <input :data-test="`step-action-${index}`" v-model="step.action" required />
            </label>
            <label>
              Expected result
              <input :data-test="`step-expected-${index}`" v-model="step.expectedResult" required />
            </label>
            <button class="ghost-danger-button" type="button" @click="removeStep(index)">Remove</button>
          </div>
        </div>

        <div class="button-row end">
          <button class="secondary-button" type="button" @click="editorOpen = false">Cancel</button>
          <button type="submit" :disabled="submitting">Save case</button>
        </div>
      </form>
    </div>
  </section>
</template>
