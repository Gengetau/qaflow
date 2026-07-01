<script setup lang="ts">
const navigationItems = [
  "dashboard",
  "projects",
  "test cases",
  "test runs",
  "defects",
  "reports",
  "settings"
];
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
        <a v-for="item in navigationItems" :key="item" href="#">
          {{ item }}
        </a>
      </nav>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div>
          <span class="eyebrow">Workspace</span>
          <h1>Acme Quality Lab</h1>
        </div>
        <div class="topbar-actions">
          <span class="role-pill">OWNER</span>
          <button type="button">New test run</button>
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
