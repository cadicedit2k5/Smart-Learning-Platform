import { computed, ref } from "vue";
import type { AuthUser, LoginRequest } from "./types";
import { defineStore } from "pinia";
import { getCurrentUser, login as loginApi } from "./authApi";
import { tokenStorage } from "./tokenStorage";

export const useAuthStore = defineStore('auth', () => {
    const user = ref<AuthUser | null>(null);

    const isAuthenticated = computed(() => user.value !== null);

    const login = async (request: LoginRequest) => {
        const res = await loginApi(request);

        tokenStorage.set(res.accessToken, res.expiresIn);
        
        try {
            const currentUser = await getCurrentUser()

            user.value = currentUser

            return currentUser
        } catch (error) {
            tokenStorage.clear()
            user.value = null

            throw error
        }
    };

    const logout = () => {
        tokenStorage.clear();
        user.value = null;
    }

    return {
      user,
      isAuthenticated,
      login,
      logout,
    }
});
