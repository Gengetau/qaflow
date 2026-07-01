import { createRouter, createWebHistory } from "vue-router";
import DashboardPage from "../../pages/DashboardPage.vue";
import DefectsPage from "../../pages/DefectsPage.vue";
import LoginPage from "../../pages/LoginPage.vue";
import ProjectsPage from "../../pages/ProjectsPage.vue";
import RegisterPage from "../../pages/RegisterPage.vue";
import ReportsPage from "../../pages/ReportsPage.vue";
import SettingsPage from "../../pages/SettingsPage.vue";
import TestCasesPage from "../../pages/TestCasesPage.vue";
import TestRunsPage from "../../pages/TestRunsPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/app/dashboard" },
    { path: "/auth/login", component: LoginPage },
    { path: "/auth/register", component: RegisterPage },
    { path: "/app/dashboard", component: DashboardPage },
    { path: "/app/projects", component: ProjectsPage },
    { path: "/app/projects/:projectId/test-cases", component: TestCasesPage },
    { path: "/app/projects/:projectId/test-runs", component: TestRunsPage },
    { path: "/app/projects/:projectId/defects", component: DefectsPage },
    { path: "/app/projects/:projectId/reports", component: ReportsPage },
    { path: "/app/settings", component: SettingsPage }
  ]
});
