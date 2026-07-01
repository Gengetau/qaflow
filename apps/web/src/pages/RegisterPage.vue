<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const displayName = ref("QA Owner");
const email = ref("owner@example.com");
const password = ref("password123");
const workspaceName = ref("Acme Quality Lab");
const errorMessage = ref("");
const isSubmitting = ref(false);

async function submit() {
  errorMessage.value = "";
  isSubmitting.value = true;

  try {
    await auth.register({
      displayName: displayName.value,
      email: email.value,
      password: password.value,
      workspaceName: workspaceName.value
    });
    await router.push("/app/dashboard");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Registration failed";
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <section class="auth-page">
    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">Create account</p>
      <h2>Start a QAFlow workspace</h2>
      <label>
        Display name
        <input v-model="displayName" type="text" autocomplete="name" required />
      </label>
      <label>
        Email
        <input v-model="email" type="email" autocomplete="email" required />
      </label>
      <label>
        Password
        <input v-model="password" type="password" autocomplete="new-password" minlength="8" required />
      </label>
      <label>
        Workspace
        <input v-model="workspaceName" type="text" required />
      </label>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? "Creating" : "Register" }}
      </button>
      <RouterLink class="auth-link" to="/auth/login">Log in</RouterLink>
    </form>
  </section>
</template>