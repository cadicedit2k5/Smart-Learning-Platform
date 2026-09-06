import { tokenStorage } from '@/features/auth/tokenStorage';
import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const httpClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Accept': 'application/json',
  },
});

httpClient.interceptors.request.use((config) => {
  const accessToken = tokenStorage.get();

  if (accessToken) {
    config.headers['Authorization'] = `Bearer ${accessToken}`;
  }

  return config;
});

httpClient.interceptors.response.use(
  
  (response) => response, 


  (error: unknown) => {
    if (!axios.isAxiosError(error)) {
      return Promise.reject(error)
    }

    if (error.response?.status === 401) {
      tokenStorage.clear();
    } 

    return Promise.reject(error);
  }
);

export default httpClient;
