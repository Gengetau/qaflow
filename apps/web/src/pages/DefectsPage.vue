<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  addDefectComment,
  createProjectDefect,
  getDefect,
  listDefects,
  transitionDefect,
  updateDefect,
  type Defect,
  type DefectPriority,
  type DefectSeverity,
  type DefectStatus
} from "../app/api/defects";
import { useAuthStore } from "../app/stores/auth";

const defectStatuses: DefectStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "REOPENED"];
const severityOptions: DefectSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const priorityOptions: DefectPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const transitionTargets: Record<DefectStatus, DefectStatus[]> = {
  OPEN: ["IN_PROGRESS"],
  IN_PROGRESS: ["RESOLVED"],
  RESOLVED: ["CLOSED", "REOPENED"],
  CLOSED: ["REOPENED"],
  REOPENED: ["IN_PROGRESS"]
};

const route = useRoute();
const auth = useAuthStore();
const { role, token } = storeToRefs(auth);

const projectId = computed(() => String(route.params.projectId ?? ""));
const routeDefectId = computed(() => String(route.params.defectId ?? ""));
const defects = ref<Defect[]>([]);
const selectedDefectId = ref("");
const loading = ref(false);
const submitting = ref(false);
const pageError = ref("");
const editorOpen = ref(false);
const editingDefectId = ref("");
const transitionTarget = ref("" as "" | DefectStatus);
const commentBody = ref("");

const defectForm = reactive({
  title: "",
  description: "",
  severity: "MEDIUM" as DefectSeverity,
  priority: "MEDIUM" as DefectPriority,
  assigneeId: ""
});

const canWrite = computed(() => role.value === "OWNER" || role.value === "TESTER");
const selectedDefect = computed(() => defects.value.find((defect) => defect.id === selectedDefectId.value) ?? null);
const editorTitle = computed(() => (editingDefectId.value ? "Edit defect" : "New defect"));
const totalOpenDefects = computed(
  () => defects.value.filter((defect) => defect.status === "OPEN" || defect.status === "REOPENED").length
);
const groupedDefects = computed(() => {
  const groups: Record<DefectStatus, Defect[]> = {
    OPEN: [],
    IN_PROGRESS: [],
    RESOLVED: [],
    CLOSED: [],
    REOPENED: []
  };
  defects.value.forEach((defect) => {
    groups[defect.status].push(defect);
  });
  return groups;
});
const availableTransitions = computed(() =>
  selectedDefect.value ? transitionTargets[selectedDefect.value.status] : []
);

onMounted(() => {
  void loadDefects();
});

watch(
  () => [projectId.value, routeDefectId.value],
  () => {
    selectedDefectId.value = "";
    defects.value = [];
    void loadDefects();
  }
);

watch(selectedDefect, (defect) => {
  transitionTarget.value = defect?.status ?? "";
});

async function loadDefects() {
  if (!token.value) {
    defects.value = [];
    return;
  }

  loading.value = true;
  pageError.value = "";
  try {
    if (projectId.value) {
      defects.value = await listDefects(token.value, projectId.value);
    } else if (routeDefectId.value) {
      defects.value = [await getDefect(token.value, routeDefectId.value)];
    } else {
      defects.value = [];
    }

    if (!defects.value.some((defect) => defect.id === selectedDefectId.value)) {
      selectedDefectId.value = routeDefectId.value || defects.value[0]?.id || "";
    }
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Defects could not be loaded.";
  } finally {
    loading.value = false;
  }
}

function openNewDefect() {
  editingDefectId.value = "";
  defectForm.title = "";
  defectForm.description = "";
  defectForm.severity = "MEDIUM";
  defectForm.priority = "MEDIUM";
  defectForm.assigneeId = "";
  editorOpen.value = true;
}

function openEditDefect() {
  if (!selectedDefect.value) {
    return;
  }

  editingDefectId.value = selectedDefect.value.id;
  defectForm.title = selectedDefect.value.title;
  defectForm.description = selectedDefect.value.description ?? "";
  defectForm.severity = selectedDefect.value.severity;
  defectForm.priority = selectedDefect.value.priority;
  defectForm.assigneeId = selectedDefect.value.assigneeId ?? "";
  editorOpen.value = true;
}

async function saveDefect() {
  if (!token.value || (!projectId.value && !editingDefectId.value)) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  const payload = {
    title: defectForm.title.trim(),
    description: defectForm.description.trim(),
    severity: defectForm.severity,
    priority: defectForm.priority,
    assigneeId: defectForm.assigneeId.trim() || null
  };

  try {
    const saved = editingDefectId.value
      ? await updateDefect(token.value, editingDefectId.value, payload)
      : await createProjectDefect(token.value, projectId.value, payload);
    replaceDefect(saved, !editingDefectId.value);
    selectedDefectId.value = saved.id;
    editorOpen.value = false;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Defect could not be saved.";
  } finally {
    submitting.value = false;
  }
}

async function transitionSelectedDefect(event: Event) {
  const nextStatus = ((event.target as unknown) as { value: DefectStatus }).value;
  if (!token.value || !selectedDefect.value || nextStatus === selectedDefect.value.status) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    replaceDefect(await transitionDefect(token.value, selectedDefect.value.id, nextStatus));
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Defect status could not be changed.";
    transitionTarget.value = selectedDefect.value.status;
  } finally {
    submitting.value = false;
  }
}

async function saveComment() {
  if (!token.value || !selectedDefect.value || !commentBody.value.trim()) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    const comment = await addDefectComment(token.value, selectedDefect.value.id, commentBody.value.trim());
    replaceDefect({
      ...selectedDefect.value,
      comments: [...selectedDefect.value.comments, comment],
      updatedAt: comment.updatedAt
    });
    commentBody.value = "";
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Comment could not be saved.";
  } finally {
    submitting.value = false;
  }
}

function replaceDefect(updatedDefect: Defect, prepend = false) {
  if (defects.value.some((defect) => defect.id === updatedDefect.id)) {
    defects.value = defects.value.map((defect) => (defect.id === updatedDefect.id ? updatedDefect : defect));
    return;
  }
  defects.value = prepend ? [updatedDefect, ...defects.value] : [...defects.value, updatedDefect];
}
</script>

<template>
  <section class="page-grid defects-page">
    <div class="page-heading action-heading">
      <div>
        <p class="eyebrow">Defects</p>
        <h2>Risk and fix tracking</h2>
        <p>Track defect lifecycle from open investigation through resolution and closure.</p>
      </div>
      <button v-if="canWrite && projectId" data-test="new-defect" type="button" @click="openNewDefect">
        New defect
      </button>
    </div>

    <p v-if="pageError" class="form-error">{{ pageError }}</p>

    <section class="execution-panel defect-metrics">
      <div>
        <span>All defects</span>
        <strong>{{ defects.length }}</strong>
      </div>
      <div>
        <span>Open risk</span>
        <strong>{{ totalOpenDefects }}</strong>
      </div>
      <div>
        <span>Critical</span>
        <strong>{{ defects.filter((defect) => defect.severity === "CRITICAL").length }}</strong>
      </div>
      <div>
        <span>Comments</span>
        <strong>{{ defects.reduce((total, defect) => total + defect.comments.length, 0) }}</strong>
      </div>
    </section>

    <div class="defect-workspace">
      <section class="kanban defect-board">
        <section v-for="status in defectStatuses" :key="status" :data-test="`defect-board-${status}`">
          <div class="board-heading">
            <h3>{{ status.replace("_", " ") }}</h3>
            <span>{{ groupedDefects[status].length }}</span>
          </div>
          <div v-if="loading" class="empty-state">Loading defects...</div>
          <div v-else-if="groupedDefects[status].length === 0" class="empty-state compact-empty">No defects</div>
          <button
            v-for="defect in groupedDefects[status]"
            :key="defect.id"
            class="defect-card"
            :class="{ selected: defect.id === selectedDefectId }"
            type="button"
            @click="selectedDefectId = defect.id"
          >
            <span class="status" :class="defect.status.toLowerCase()">{{ defect.status }}</span>
            <strong>{{ defect.title }}</strong>
            <small>{{ defect.caseKey ?? "Project defect" }}</small>
            <span class="card-badges">
              <span class="priority" :class="defect.severity.toLowerCase()">{{ defect.severity }}</span>
              <span class="priority" :class="defect.priority.toLowerCase()">{{ defect.priority }}</span>
            </span>
          </button>
        </section>
      </section>

      <section class="table-surface defect-detail-panel" data-test="defect-detail">
        <div v-if="!selectedDefect" class="empty-state detail-empty">
          <h3>Select a defect</h3>
          <p>Status, comments, and lifecycle actions appear here.</p>
        </div>
        <template v-else>
          <div class="panel-heading defect-detail-heading">
            <div>
              <p class="eyebrow">{{ selectedDefect.caseKey ?? "Project defect" }}</p>
              <h3>{{ selectedDefect.title }}</h3>
              <p>{{ selectedDefect.description || "No description set." }}</p>
            </div>
            <button
              v-if="canWrite"
              class="secondary-button"
              data-test="edit-defect"
              type="button"
              @click="openEditDefect"
            >
              Edit
            </button>
          </div>

          <div class="case-meta-grid defect-meta-grid">
            <span class="status" :class="selectedDefect.status.toLowerCase()">{{ selectedDefect.status }}</span>
            <span class="priority" :class="selectedDefect.severity.toLowerCase()">
              {{ selectedDefect.severity }}
            </span>
            <span class="priority" :class="selectedDefect.priority.toLowerCase()">
              {{ selectedDefect.priority }}
            </span>
            <span>{{ selectedDefect.testRunItemId ? "Linked run item" : "Project level" }}</span>
          </div>

          <div v-if="canWrite" class="defect-transition-row">
            <label>
              Transition
              <select
                data-test="defect-transition-input"
                v-model="transitionTarget"
                :disabled="submitting || availableTransitions.length === 0"
                @change="transitionSelectedDefect"
              >
                <option :value="selectedDefect.status">{{ selectedDefect.status }}</option>
                <option v-for="status in availableTransitions" :key="status" :value="status">
                  {{ status }}
                </option>
              </select>
            </label>
          </div>

          <div class="comment-section">
            <div class="panel-heading flush-heading">
              <div>
                <p class="eyebrow">Comments</p>
                <h3>{{ selectedDefect.comments.length }} notes</h3>
              </div>
            </div>
            <div v-if="selectedDefect.comments.length === 0" class="empty-state compact-empty">No comments yet</div>
            <article v-for="comment in selectedDefect.comments" :key="comment.id" class="comment-row">
              {{ comment.body }}
            </article>

            <form v-if="canWrite" class="comment-form" data-test="defect-comment-form" @submit.prevent="saveComment">
              <label>
                Add comment
                <textarea
                  data-test="defect-comment-input"
                  v-model="commentBody"
                  rows="3"
                  required
                  maxlength="4000"
                />
              </label>
              <div class="button-row end">
                <button type="submit" :disabled="submitting || !commentBody.trim()">Save comment</button>
              </div>
            </form>
          </div>
        </template>
      </section>
    </div>

    <div v-if="editorOpen" class="modal-backdrop">
      <form class="modal defect-editor" data-test="defect-editor-form" @submit.prevent="saveDefect">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Defect</p>
            <h3>{{ editorTitle }}</h3>
          </div>
          <button class="ghost-button" type="button" @click="editorOpen = false">Close</button>
        </div>

        <label>
          Title
          <input data-test="defect-title-input" v-model="defectForm.title" required maxlength="240" />
        </label>
        <label>
          Description
          <textarea data-test="defect-description-input" v-model="defectForm.description" rows="4" />
        </label>
        <div class="case-editor-grid">
          <label>
            Severity
            <select data-test="defect-severity-input" v-model="defectForm.severity">
              <option v-for="severity in severityOptions" :key="severity" :value="severity">{{ severity }}</option>
            </select>
          </label>
          <label>
            Priority
            <select data-test="defect-priority-input" v-model="defectForm.priority">
              <option v-for="priority in priorityOptions" :key="priority" :value="priority">{{ priority }}</option>
            </select>
          </label>
          <label>
            Assignee ID
            <input v-model="defectForm.assigneeId" placeholder="Optional UUID" />
          </label>
        </div>

        <div class="button-row end">
          <button class="secondary-button" type="button" @click="editorOpen = false">Cancel</button>
          <button type="submit" :disabled="submitting">Save defect</button>
        </div>
      </form>
    </div>
  </section>
</template>
