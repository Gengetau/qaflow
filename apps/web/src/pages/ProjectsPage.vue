<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  createProject,
  createSuite,
  deleteProject,
  deleteSuite,
  listProjects,
  listSuites,
  updateProject,
  updateSuite,
  type Project,
  type ProjectStatus,
  type TestSuite
} from "../app/api/projects";
import { useAuthStore } from "../app/stores/auth";

const auth = useAuthStore();
const { activeWorkspace, role, token } = storeToRefs(auth);

const projects = ref<Project[]>([]);
const suites = ref<TestSuite[]>([]);
const selectedProjectId = ref("");
const loadingProjects = ref(false);
const loadingSuites = ref(false);
const submitting = ref(false);
const pageError = ref("");
const projectDialogOpen = ref(false);
const suiteDialogOpen = ref(false);
const editingProjectId = ref("");
const editingSuiteId = ref("");
const totalProjects = ref(0);

const projectForm = reactive({
  name: "",
  key: "",
  description: "",
  status: "ACTIVE" as ProjectStatus
});

const suiteForm = reactive({
  name: "",
  description: "",
  sortOrder: 0
});

const selectedProject = computed(
  () => projects.value.find((project) => project.id === selectedProjectId.value) ?? null
);
const canManageProjects = computed(() => role.value === "OWNER");
const canManageSuites = computed(() => role.value === "OWNER" || role.value === "TESTER");
const projectDialogTitle = computed(() => (editingProjectId.value ? "Edit project" : "New project"));
const suiteDialogTitle = computed(() => (editingSuiteId.value ? "Edit suite" : "New suite"));

onMounted(() => {
  void loadProjects();
});

watch(
  () => activeWorkspace.value?.id,
  () => {
    selectedProjectId.value = "";
    suites.value = [];
    void loadProjects();
  }
);

watch(selectedProjectId, (projectId) => {
  if (projectId) {
    void loadSuitesForProject(projectId);
  } else {
    suites.value = [];
  }
});

async function loadProjects() {
  if (!activeWorkspace.value || !token.value) {
    projects.value = [];
    return;
  }

  loadingProjects.value = true;
  pageError.value = "";
  try {
    const page = await listProjects(token.value, activeWorkspace.value.id);
    projects.value = page.items;
    totalProjects.value = page.totalItems;

    if (!projects.value.some((project) => project.id === selectedProjectId.value)) {
      selectedProjectId.value = projects.value[0]?.id ?? "";
    } else if (selectedProjectId.value) {
      await loadSuitesForProject(selectedProjectId.value);
    }
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Projects could not be loaded.";
  } finally {
    loadingProjects.value = false;
  }
}

async function loadSuitesForProject(projectId: string) {
  if (!token.value) {
    suites.value = [];
    return;
  }

  loadingSuites.value = true;
  pageError.value = "";
  try {
    suites.value = await listSuites(token.value, projectId);
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Suites could not be loaded.";
  } finally {
    loadingSuites.value = false;
  }
}

function openNewProject() {
  editingProjectId.value = "";
  projectForm.name = "";
  projectForm.key = "";
  projectForm.description = "";
  projectForm.status = "ACTIVE";
  projectDialogOpen.value = true;
}

function openEditProject() {
  if (!selectedProject.value) {
    return;
  }

  editingProjectId.value = selectedProject.value.id;
  projectForm.name = selectedProject.value.name;
  projectForm.key = selectedProject.value.key;
  projectForm.description = selectedProject.value.description ?? "";
  projectForm.status = selectedProject.value.status;
  projectDialogOpen.value = true;
}

async function saveProject() {
  if (!activeWorkspace.value || !token.value) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    if (editingProjectId.value) {
      const updated = await updateProject(token.value, editingProjectId.value, {
        name: projectForm.name.trim(),
        description: projectForm.description.trim(),
        status: projectForm.status
      });
      projects.value = projects.value.map((project) => (project.id === updated.id ? updated : project));
      selectedProjectId.value = updated.id;
    } else {
      const created = await createProject(token.value, {
        workspaceId: activeWorkspace.value.id,
        name: projectForm.name.trim(),
        key: projectForm.key.trim(),
        description: projectForm.description.trim()
      });
      projects.value = [created, ...projects.value];
      totalProjects.value += 1;
      selectedProjectId.value = created.id;
    }
    projectDialogOpen.value = false;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Project could not be saved.";
  } finally {
    submitting.value = false;
  }
}

async function removeSelectedProject() {
  if (!selectedProject.value || !token.value) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    const removedId = selectedProject.value.id;
    await deleteProject(token.value, removedId);
    projects.value = projects.value.filter((project) => project.id !== removedId);
    totalProjects.value = Math.max(0, totalProjects.value - 1);
    selectedProjectId.value = projects.value[0]?.id ?? "";
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Project could not be deleted.";
  } finally {
    submitting.value = false;
  }
}

function openNewSuite() {
  editingSuiteId.value = "";
  suiteForm.name = "";
  suiteForm.description = "";
  suiteForm.sortOrder = suites.value.length * 10;
  suiteDialogOpen.value = true;
}

function openEditSuite(suite: TestSuite) {
  editingSuiteId.value = suite.id;
  suiteForm.name = suite.name;
  suiteForm.description = suite.description ?? "";
  suiteForm.sortOrder = suite.sortOrder;
  suiteDialogOpen.value = true;
}

async function saveSuite() {
  if (!selectedProject.value || !token.value) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    const payload = {
      name: suiteForm.name.trim(),
      description: suiteForm.description.trim(),
      sortOrder: Number(suiteForm.sortOrder)
    };

    if (editingSuiteId.value) {
      const updated = await updateSuite(token.value, editingSuiteId.value, payload);
      suites.value = suites.value
        .map((suite) => (suite.id === updated.id ? updated : suite))
        .sort(compareSuites);
    } else {
      const created = await createSuite(token.value, selectedProject.value.id, payload);
      suites.value = [...suites.value, created].sort(compareSuites);
    }
    suiteDialogOpen.value = false;
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Suite could not be saved.";
  } finally {
    submitting.value = false;
  }
}

async function removeSuite(suite: TestSuite) {
  if (!token.value) {
    return;
  }

  submitting.value = true;
  pageError.value = "";
  try {
    await deleteSuite(token.value, suite.id);
    suites.value = suites.value.filter((item) => item.id !== suite.id);
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : "Suite could not be deleted.";
  } finally {
    submitting.value = false;
  }
}

function compareSuites(a: TestSuite, b: TestSuite) {
  return a.sortOrder - b.sortOrder || a.name.localeCompare(b.name);
}
</script>

<template>
  <section class="page-grid projects-page">
    <div class="page-heading action-heading">
      <div>
        <p class="eyebrow">Projects</p>
        <h2>QA initiatives</h2>
        <p>Create and review projects scoped to the current workspace.</p>
      </div>
      <button v-if="canManageProjects" data-test="new-project" type="button" @click="openNewProject">
        New project
      </button>
    </div>

    <p v-if="pageError" class="form-error">{{ pageError }}</p>

    <div class="project-layout">
      <section class="table-surface project-list-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Workspace projects</p>
            <h3>{{ totalProjects }} total</h3>
          </div>
        </div>

        <div v-if="loadingProjects" class="empty-state">Loading projects...</div>
        <div v-else-if="projects.length === 0" class="empty-state">
          <h3>No projects yet</h3>
          <p>Start with a focused QA scope, then add suites for coverage areas.</p>
        </div>
        <div v-else>
          <div class="table-row header project-row">
            <span>Key</span>
            <span>Name</span>
            <span>Status</span>
          </div>
          <button
            v-for="project in projects"
            :key="project.id"
            class="table-row project-row selectable-row"
            :class="{ selected: project.id === selectedProjectId }"
            type="button"
            @click="selectedProjectId = project.id"
          >
            <span>{{ project.key }}</span>
            <span>{{ project.name }}</span>
            <span class="status" :class="project.status.toLowerCase()">{{ project.status }}</span>
          </button>
        </div>
      </section>

      <section class="table-surface project-detail-panel">
        <div v-if="!selectedProject" class="empty-state detail-empty">
          <h3>Select a project</h3>
          <p>Project suites and management actions appear here.</p>
        </div>
        <template v-else>
          <div class="panel-heading">
            <div>
              <p class="eyebrow">{{ selectedProject.key }}</p>
              <h3>{{ selectedProject.name }}</h3>
              <p>{{ selectedProject.description || "No description set." }}</p>
            </div>
            <div v-if="canManageProjects" class="button-row">
              <button class="secondary-button" type="button" @click="openEditProject">Edit</button>
              <button class="danger-button" type="button" :disabled="submitting" @click="removeSelectedProject">
                Delete
              </button>
            </div>
          </div>

          <div class="suite-toolbar">
            <div>
              <p class="eyebrow">Suites</p>
              <h3>{{ suites.length }} suites</h3>
            </div>
            <button v-if="canManageSuites" class="secondary-button" type="button" @click="openNewSuite">
              Add suite
            </button>
          </div>

          <div v-if="loadingSuites" class="empty-state">Loading suites...</div>
          <div v-else-if="suites.length === 0" class="empty-state">
            <h3>No suites yet</h3>
            <p>Group cases by release area, platform, or regression lane.</p>
          </div>
          <div v-else>
            <div class="table-row header suite-row">
              <span>Order</span>
              <span>Suite</span>
              <span>Actions</span>
            </div>
            <div v-for="suite in suites" :key="suite.id" class="table-row suite-row">
              <span>{{ suite.sortOrder }}</span>
              <span>
                <strong>{{ suite.name }}</strong>
                <small>{{ suite.description || "No description" }}</small>
              </span>
              <span class="button-row">
                <button
                  v-if="canManageSuites"
                  class="ghost-button"
                  type="button"
                  @click="openEditSuite(suite)"
                >
                  Edit
                </button>
                <button
                  v-if="canManageSuites"
                  class="ghost-danger-button"
                  type="button"
                  :disabled="submitting"
                  @click="removeSuite(suite)"
                >
                  Delete
                </button>
              </span>
            </div>
          </div>
        </template>
      </section>
    </div>

    <div v-if="projectDialogOpen" class="modal-backdrop">
      <form class="modal" data-test="project-form" @submit.prevent="saveProject">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Project</p>
            <h3>{{ projectDialogTitle }}</h3>
          </div>
          <button class="ghost-button" type="button" @click="projectDialogOpen = false">Close</button>
        </div>
        <label>
          Name
          <input data-test="project-name" v-model="projectForm.name" required maxlength="180" />
        </label>
        <label>
          Key
          <input
            data-test="project-key"
            v-model="projectForm.key"
            :disabled="Boolean(editingProjectId)"
            required
            maxlength="24"
          />
        </label>
        <label>
          Description
          <textarea data-test="project-description" v-model="projectForm.description" rows="4" />
        </label>
        <label v-if="editingProjectId">
          Status
          <select v-model="projectForm.status">
            <option value="ACTIVE">ACTIVE</option>
            <option value="ARCHIVED">ARCHIVED</option>
          </select>
        </label>
        <div class="button-row end">
          <button class="secondary-button" type="button" @click="projectDialogOpen = false">Cancel</button>
          <button type="submit" :disabled="submitting">Save project</button>
        </div>
      </form>
    </div>

    <div v-if="suiteDialogOpen" class="modal-backdrop">
      <form class="modal" @submit.prevent="saveSuite">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Suite</p>
            <h3>{{ suiteDialogTitle }}</h3>
          </div>
          <button class="ghost-button" type="button" @click="suiteDialogOpen = false">Close</button>
        </div>
        <label>
          Name
          <input v-model="suiteForm.name" required maxlength="180" />
        </label>
        <label>
          Description
          <textarea v-model="suiteForm.description" rows="4" />
        </label>
        <label>
          Sort order
          <input v-model.number="suiteForm.sortOrder" min="0" type="number" />
        </label>
        <div class="button-row end">
          <button class="secondary-button" type="button" @click="suiteDialogOpen = false">Cancel</button>
          <button type="submit" :disabled="submitting">Save suite</button>
        </div>
      </form>
    </div>
  </section>
</template>
