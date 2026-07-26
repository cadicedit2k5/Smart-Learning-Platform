import axios from "axios";

const BASE_URL = '';

export const endpoints = {

}

export const authApi = () => {
    const token = null;
    return axios.create({
        baseURL: BASE_URL,
        headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
}

export default axios.create({
    baseURL: BASE_URL,
});