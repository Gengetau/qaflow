<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const email = ref("owner@example.com");
const password = ref("password123");
const errorMessage = ref("");
const isSubmitting = ref(false);
const redirectTo = computed(() => (typeof route.query.redirect === "string" ? route.query.redirect : "/app/dashboard"));

async function submit() {
  errorMessage.value = "";
  isSubmitting.value = true;

  try {
    await auth.login({ email: email.value, password: password.value });
    await router.push(redirectTo.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Login failed";
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <section class="auth-page">
    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">Welcome back</p>
      <h2>Log in to QAFlow</h2>
      <label>
        Email
        <input v-model="email" type="email" autocomplete="email" required />
      </label>
      <label>
        Password
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? "Logging in" : "Log in" }}
      </button>
      <RouterLink class="auth-link" to="/auth/register">Create account</RouterLink>
    </form>
  </section>
</template>