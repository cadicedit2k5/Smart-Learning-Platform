const ACCESS_TOKEN_KEY = 'accessToken'
const TOKEN_EXPIRES_AT_KEY = 'accessTokenExpiresAt'

export const tokenStorage = {
    set(accessToken: string, expiresIn: number) {
        const expiresAt = Date.now() + expiresIn * 1000;

        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
        localStorage.setItem(
        TOKEN_EXPIRES_AT_KEY,
        String(expiresAt),
        );
    },

    get(): string | null {
        const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
        const expiresAt = localStorage.getItem(TOKEN_EXPIRES_AT_KEY);

        if (!accessToken || !expiresAt) {
            return null;
        }

        if (Date.now() > Number(expiresAt)) {
            this.clear();
            return null;
        }

        return accessToken;
    },

    clear() {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(TOKEN_EXPIRES_AT_KEY);
    }
};