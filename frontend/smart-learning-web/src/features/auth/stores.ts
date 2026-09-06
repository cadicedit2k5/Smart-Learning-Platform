import { computed, ref } from "vue";
import type { AuthUser, LoginRequest, RegisterRequest, UpdateCurrentUserRequest } from "./types";
import { defineStore } from "pinia";
import { getCurrentUser, login as loginApi, updateCurrentUser } from "./authApi";
import { tokenStorage } from "./tokenStorage";

export const useAuthStore = defineStore('auth', () => {
    const user = ref<AuthUser | null>(null);
    const initialized = ref(false);

    const isAuthenticated = computed(() => user.value !== null);

    const login = async (request: LoginRequest) => {
        const res = await loginApi(request);

        tokenStorage.set(res.accessToken, res.expiresIn);
        
        try {
            user.value = await getCurrentUser()

            return user.value;
        } catch (error) {
            tokenStorage.clear()
            user.value = null

            throw error
        }
    };

    const initialize = async () => {
        if (initialized.value) return;

        try {
            if (tokenStorage.get()) {
                user.value = await getCurrentUser();
            }
        } catch (error) {
            tokenStorage.clear();
            user.value = null;
        }
        finally {
            initialized.value = true;
        }
    }

    const updateProfile = async (
        request: UpdateCurrentUserRequest,
        ) => {
        const updatedUser = await updateCurrentUser(request);

        user.value = updatedUser;

        return updatedUser;
    }

    const logout = () => {
        tokenStorage.clear();
        user.value = null;
    }

    return {
      user,
      initialized,
      isAuthenticated,
      login,
      initialize,
      logout,
      updateProfile
    }
});
