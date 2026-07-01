import { useAuthStore } from "../stores/auth";
import { createRouter, createWebHistory, type RouterHistory } from "vue-router";
import DashboardPage from "../../pages/DashboardPage.vue";
import DefectsPage from "../../pages/DefectsPage.vue";
import LoginPage from "../../pages/LoginPage.vue";
import ProjectsPage from "../../pages/ProjectsPage.vue";
import RegisterPage from "../../pages/RegisterPage.vue";
import ReportsPage from "../../pages/ReportsPage.vue";
import SettingsPage from "../../pages/SettingsPage.vue";
import TestCasesPage from "../../pages/TestCasesPage.vue";
import TestRunsPage from "../../pages/TestRunsPage.vue";

export function createQaflowRouter(history: RouterHistory = createWebHistory()) {
  const router = createRouter({
    history,
    routes: [
      { path: "/", redirect: "/app/dashboard" },
      { path: "/auth/login", component: LoginPage, meta: { guestOnly: true } },
      { path: "/auth/register", component: RegisterPage, meta: { guestOnly: true } },
      { path: "/app/dashboard", component: DashboardPage, meta: { requiresAuth: true } },
      { path: "/app/projects/:projectId/dashboard", component: DashboardPage, meta: { requiresAuth: true } },
      { path: "/app/projects", component: ProjectsPage, meta: { requiresAuth: true } },
      { path: "/app/projects/:projectId/test-cases", component: TestCasesPage, meta: { requiresAuth: true } },
      { path: "/app/projects/:projectId/test-runs", component: TestRunsPage, meta: { requiresAuth: true } },
      { path: "/app/projects/:projectId/defects", component: DefectsPage, meta: { requiresAuth: true } },
      { path: "/app/defects/:defectId", component: DefectsPage, meta: { requiresAuth: true } },
      { path: "/app/projects/:projectId/reports", component: ReportsPage, meta: { requiresAuth: true } },
      { path: "/app/settings", component: SettingsPage, meta: { requiresAuth: true } }
    ]
  });

  router.beforeEach((to) => {
    const auth = useAuthStore();
    auth.restoreSession();

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { path: "/auth/login", query: { redirect: to.fullPath } };
    }

    if (to.meta.guestOnly && auth.isAuthenticated) {
      return "/app/dashboard";
    }

    return true;
  });

  return router;
}

export const router = createQaflowRouter();
