<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";

const navigationItems = [
  { label: "dashboard", to: "/app/dashboard" },
  { label: "projects", to: "/app/projects" },
  { label: "test cases", to: "/app/projects/demo/test-cases" },
  { label: "test runs", to: "/app/projects/demo/test-runs" },
  { label: "defects", to: "/app/projects/demo/defects" },
  { label: "reports", to: "/app/projects/demo/reports" },
  { label: "settings", to: "/app/settings" }
];

const auth = useAuthStore();
const router = useRouter();
const { activeWorkspace, role, user } = storeToRefs(auth);
const workspaceName = computed(() => activeWorkspace.value?.name ?? "QAFlow Workspace");

async function logout() {
  await auth.logout();
  await router.push("/auth/login");
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">QF</span>
        <div>
          <strong>QAFlow</strong>
          <span>QA management</span>
        </div>
      </div>

      <nav class="navigation" aria-label="Primary">
        <RouterLink v-for="item in navigationItems" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div>
          <span class="eyebrow">Workspace</span>
          <h1>{{ workspaceName }}</h1>
        </div>
        <div class="topbar-actions">
          <span class="role-pill">{{ role ?? "VIEWER" }}</span>
          <span class="user-label">{{ user?.displayName }}</span>
          <button type="button" @click="logout">Log out</button>
        </div>
      </header>

      <main class="content">
        <slot>
          <section class="hero-panel">
            <p class="eyebrow">Quality workflow</p>
            <h2>Manage test runs, defects, evidence, and reports in one place.</h2>
          </section>
        </slot>
      </main>
    </div>
  </div>
</template>