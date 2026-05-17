import axios from "axios";
import { getToken, removeToken } from "./helper";
import { store } from "./index";
import { appRoutes } from "./endpoints";
export const instance = axios.create({
  baseURL: process.env.REACT_APP_BASE_URL,
});
instance.interceptors.request.use(function (config) {
  const token = getToken();
  if (token) {
    const state = store.getState();
    config.headers.Authorization = `Bearer ${token}`;
    // Use explicit tenant-id from config if provided (e.g. for cross-tenant inward fetch); else use state
    if (!config.skipTenantId) {
      const tenantId = config.headers && config.headers["tenant-id"] != null
        ? config.headers["tenant-id"]
        : state.tennant.tennant_id;
      if (tenantId) {
        config.headers["tenant-id"] = tenantId;
      }
    }
  }
  return config;
});
const checkToken = (error) => {
  if (error && error.response && error.response.status === 401) {
    removeToken();
    window.location.href = window.location.origin + '/#' + appRoutes.login;
  }
};
export const API = {
  POST: async (url, params, config = {}) => {
    try {
      const response = await instance.post(url, params, config);

      return { data: response.data, success: true };
    } catch (error) {
      checkToken(error);
      return {
        success: false,
        status: error?.response?.status,
        errorMessage:
          error?.response?.data?.message ||
          error?.response?.data?.apierror?.message ||
          error?.response?.data?.apierror?.debugMessage,
      };
    }
  },

  GETBlob: async (url, config = {}) => {
    try {
      const response = await instance.get(url, {
        ...config,
        responseType: "blob",
      });
      return { success: true, data: response.data, headers: response.headers };
    } catch (error) {
      checkToken(error);
      return { success: false };
    }
  },

  /** POST that returns binary (e.g. Excel). Use for export/download. config is merged with axios config (e.g. skipTenantId). */
  POSTBlob: async (url, params, config = {}) => {
    try {
      const response = await instance.post(url, params, {
        ...config,
        responseType: "blob",
      });
      return { success: true, data: response.data, headers: response.headers };
    } catch (error) {
      checkToken(error);
      let errorMessage =
        error?.response?.data?.message ||
        (error?.response?.data && typeof error.response.data === "string"
          ? error.response.data
          : undefined);
      if (error?.response?.data instanceof Blob) {
        try {
          const text = await error.response.data.text();
          const json = JSON.parse(text);
          errorMessage = json.message || text;
        } catch (_) {
          errorMessage = "Download failed";
        }
      }
      return {
        success: false,
        errorMessage:
          errorMessage ||
          (error?.response?.data && typeof error.response.data === "object" && error.response.data?.message) ||
          "Download failed",
      };
    }
  },
  PATCH: async (url, params) => {
    try {
      const response = await instance.patch(url, params);
      return { data: response.data, success: true };
    } catch (error) {
      checkToken(error);
      return {
        success: false,
        errorMessage:
          error &&
          error.response &&
          error.response.data &&
          error.response.data.message,
      };
    }
  },
  PUT: async (url, params) => {
    try {
      const response = await instance.put(url, params);
      return { data: response.data, success: true };
    } catch (error) {
      checkToken(error);
      return {
        success: false,
        errorMessage:
          error &&
          error.response &&
          error.response.data &&
          error.response.data.message,
      };
    }
  },
  GET: async (url, config) => {
    try {
      const response = await instance.get(url, config);
      if (response.config.responseType === "blob") {
        return response;
      }
      return { data: response.data, success: true };
    } catch (error) {
      checkToken(error);
      return {
        success: false,
        errorMessage:
          error &&
          error.response &&
          error.response.data &&
          error.response.data.message,
      };
    }
  },
  /** POST FormData (multipart/form-data) — used for file uploads (e.g. import). */
  POSTMultipart: async (url, formData) => {
    try {
      const response = await instance.post(url, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return { data: response.data, success: true };
    } catch (error) {
      checkToken(error);
      return {
        success: false,
        errorMessage:
          error?.response?.data?.message ||
          (typeof error?.response?.data === "string" ? error.response.data : undefined) ||
          "Upload failed",
      };
    }
  },
  DELETE: async (url, data) => {
    try {
      const response = await instance.delete(url, data ? { data } : undefined);
      return { data: response.data, success: true };
    } catch (error) {
      checkToken(error);
      return {
        success: false,
        errorMessage:
          error &&
          error.response &&
          error.response.data &&
          error.response.data.message,
      };
    }
  },
};
