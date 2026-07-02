import Cookies from "js-cookie";
import moment from "moment";
import { constants } from "./messages";

const mainKey = process.env.REACT_APP_COOKIES_MAIN_KEY;
const tokenKey = `${mainKey}Token`;
const expiryTime = 0.5;
const roleKey = `${mainKey}Role`;
const userIdKey = `${mainKey}"UserId`;
const userKey = `${mainKey}ecUserName`;

// Stores per-role inventory edit day limits coming from backend
// Example payload: { adminDays: 7, managerDays: 5, generalDays: 3 }
const inventoryEditDaysKey = `${mainKey}InventoryEditDays`;
const rejectReturnDaysKey = `${mainKey}RejectReturnDays`;
const allRolesKey = `${mainKey}AllRoles`;

let _isAdmin;

export const checkifDateLessThan = (date, days) => {
  const dateo = new Date(
    moment(date, "DD-MM-YYYY").format("YYYY-MM-DD")
  ).getTime();
  const today = new Date().getTime();
  const diff = days * 24 * 60 * 60 * 1000;
  return today - dateo > diff;
};

// Save inventory edit-constraint days to cookie so that the UI can
// enforce date edit/create restrictions based on backend configuration.
export const setInventoryEditDays = ({ adminDays, managerDays, generalDays }) => {
  const payload = {
    adminDays:
      typeof adminDays === "number" ? adminDays : constants.adminDays,
    managerDays:
      typeof managerDays === "number" ? managerDays : constants.managerDays,
    generalDays:
      typeof generalDays === "number" ? generalDays : constants.generalDays,
  };

  Cookies.set(inventoryEditDaysKey, JSON.stringify(payload));
};

const getInventoryEditDaysFromCookie = () => {
  try {
    const value = Cookies.get(inventoryEditDaysKey);
    if (!value) {
      return null;
    }
    const parsed = JSON.parse(value);

    // Basic shape validation
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      ("adminDays" in parsed ||
        "managerDays" in parsed ||
        "generalDays" in parsed)
    ) {
      return parsed;
    }
    return null;
  } catch (e) {
    return null;
  }
};

export const setRejectReturnDays = ({ adminDays, managerDays, generalDays }) => {
  const payload = {
    adminDays: typeof adminDays === "number" ? adminDays : 90,
    managerDays: typeof managerDays === "number" ? managerDays : 90,
    generalDays: typeof generalDays === "number" ? generalDays : 90,
  };
  Cookies.set(rejectReturnDaysKey, JSON.stringify(payload));
};

export const getRoleRejectReturnConstraintDays = () => {
  const role = (getRole() || "").toLowerCase();
  let config = { adminDays: 90, managerDays: 90, generalDays: 90 };
  try {
    const value = Cookies.get(rejectReturnDaysKey);
    if (value) config = JSON.parse(value);
  } catch (e) {}

  if (!role) return config.generalDays;
  if (role === "admin") return config.adminDays;
  if (role === "purchase-manager") return config.managerDays;
  return config.generalDays;
};

export const getRoleEditConstraintDays = () => {
  const role = (getRole() || "").toLowerCase();

  const defaultDaysConfig = {
    adminDays: constants.adminDays,
    managerDays: constants.managerDays,
    generalDays: constants.generalDays,
  };

  const cookieDaysConfig = getInventoryEditDaysFromCookie();
  const daysConfig = cookieDaysConfig || defaultDaysConfig;

  if (!role) return defaultDaysConfig.generalDays;

  if (role === "admin") return daysConfig.adminDays;

  if (role === "purchase-manager") return daysConfig.managerDays;

  return daysConfig.generalDays;
};
export const getToken = () => {
  return Cookies.get(tokenKey);
};
export const removeToken = () => {
  Cookies.remove(tokenKey, { expires: expiryTime });
};

export const setToken = (token) => {
  Cookies.set(tokenKey, token, {
    expires: expiryTime,
  });
};

export const setUserId = (userId) => {
  Cookies.set(userIdKey, userId);
};

export const getUserId = () => {
  return Cookies.get(userIdKey);
};

export const setRole = (role) => {
  Cookies.set(roleKey, role);
};
export const getRole = () => {
  return Cookies.get(roleKey);
};
export const setAllRoles = (roles) => {
  Cookies.set(allRolesKey, JSON.stringify(roles || []));
};
export const getAllRoles = () => {
  try { return JSON.parse(Cookies.get(allRolesKey) || "[]"); }
  catch (e) { return []; }
};
export const hasRole = (role) => {
  return getAllRoles().map(r => r.toLowerCase()).includes(role.toLowerCase());
};
export const setUserName = (role) => {
  Cookies.set(userKey, role);
};
export const getUserName = () => {
  return Cookies.get(userKey);
};

export const clearCookies = () => {
  Cookies.remove(userKey);
  Cookies.remove(roleKey);
  Cookies.remove(allRolesKey);
  Cookies.remove(userIdKey);
  Cookies.remove(tokenKey);
}
export const isAdmin = () => {
  if (!_isAdmin) {
    const role = (getRole() || "").toLowerCase();
    _isAdmin = role === "admin";
  }
  return _isAdmin;
};

export const canEditInventoryModules = () => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  return role === "admin" || role === "purchase-manager";
};

export const canEditBOQ = () => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  return role === "admin" || role === "project-manager";
};

/**
 * Contact module write access: admin, purchase-manager, store-incharge.
 */
export const canEditContactModules = () => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  return role === "admin" || role === "purchase-manager" || role === "store-incharge";
};

/**
 * Indent creation: admin, purchase-manager, store-incharge can create new indents.
 * management and project-manager are read-only viewers.
 */
export const canCreateIndent = () => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  return role === "admin" || role === "purchase-manager" || role === "store-incharge";
};

/**
 * Inward / Outward write access: admin and store-incharge only.
 */
export const canCreateInward = () => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  return role === "admin" || role === "store-incharge";
};

/**
 * Indent edit permission by status and role:
 * - NEW: admin, purchase-manager, project-manager, or store-incharge can edit
 * - APPROVED: admin, purchase-manager, or project-manager can edit
 *   (store-incharge cannot edit once approved — record is owned by procurement)
 * - Any other status (PO Partial, PO Completed, Closed, etc.): no one can edit
 */
export const canEditIndentRecord = (indentStatus) => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  const status = (indentStatus || "").toString().trim().toLowerCase();
  // Terminal statuses — no one can edit
  const terminalStatuses = ["closed", "cancelled", "rejected"];
  if (terminalStatuses.includes(status)) return false;
  // Any non-terminal status — eligible roles can edit (backend enforces NEW line item check)
  return role === "admin" || role === "purchase-manager" || role === "project-manager" || role === "store-incharge";
};

/**
 * Store Incharge can cancel only NEW (pre-approval) indents.
 * Admin / Purchase Manager / Project Manager can cancel NEW or APPROVED indents.
 */
export const canCancelIndentRecord = (indentStatus) => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  const status = (indentStatus || "").toString().trim().toLowerCase();
  const canManage = role === "admin" || role === "purchase-manager" || role === "project-manager";
  if (status === "new" && role === "store-incharge") return true;
  if (status === "approved" && canManage) return true;
  return false;
};

/**
 * Admin / Purchase Manager / Project Manager can approve only NEW indents.
 */
export const canApproveIndentRecord = (indentStatus) => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  const status = (indentStatus || "").toString().trim().toLowerCase();
  return (
    status === "new" &&
    (role === "admin" || role === "purchase-manager" || role === "project-manager")
  );
};

/**
 * Admin / Purchase Manager / Project Manager can reject only NEW indents (before approval).
 */
const INDENT_TERMINAL_STATUSES = ["cancelled", "rejected", "closed"];
export const canManagerRejectIndentRecord = (indentStatus) => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  const status = (indentStatus || "").toString().trim().toLowerCase();
  return (
    status === "new" &&
    (role === "admin" || role === "purchase-manager" || role === "project-manager")
  );
};

/**
 * Anyone who can create indents can re-submit a REJECTED indent as a new pre-filled indent.
 * UI-only — opens prefilled new indent form, no separate backend endpoint.
 */
export const canResubmitIndentRecord = (indentStatus) => {
  const role = (getRole() || "").toLowerCase();
  if (!role) return false;
  const status = (indentStatus || "").toString().trim().toLowerCase();
  return (
    status === "rejected" &&
    role === "store-incharge"
  );
};

/**
 * @deprecated Use canApproveIndentRecord / canManagerRejectIndentRecord instead.
 */
export const canApproveRejectIndentRecord = (indentStatus) => {
  return canApproveIndentRecord(indentStatus);
};

// Returns true if the current user is allowed to view
// price / money related fields in the UI and print views.
// Requirement: project-manager and store-incharge must NOT see any price fields.
export const canViewMoneyFields = () => {
  const role = (getRole() || "").toLowerCase();
  // If role is not available, default to allowing visibility
  // so we don't accidentally hide data for valid users.
  if (!role) return true;
  return role !== "project-manager" && role !== "store-incharge";
};
export const stringNotNull = (str) => {
  return str !== null ? str : "";
};

export const setSession = (key, value) => {
  sessionStorage.setItem(key, JSON.stringify(value));
};

export const getSession = (key) => {
  if (
    sessionStorage.getItem(key) !== undefined &&
    sessionStorage.getItem(key) !== null
  ) {
    return JSON.parse(sessionStorage.getItem(key));
  } else {
    return null;
  }
};

export const clearSession = (key) => {
  sessionStorage.removeItem(key);
};

export const clearAllSession = () => {
  sessionStorage.clear();
};
export const getDiffInDays = (fromdate) => {
  return moment().diff(fromdate, "days");
};

export const getDiffInDaysv2 = (fromdate) => {
  // Set both dates to the start of their respective days
  const startOfToday = moment().startOf('day');
  const startOfFromDate = moment(fromdate).startOf('day');

  // Calculate the difference in days
  return startOfToday.diff(startOfFromDate, 'days');
};

export const getFormatedDateTime = (date) => {
  return moment(date, "DD-MM-YYYY hh:mm:ss").format("DD-MM-YYYY hh:mm:ss A");
};

export const getJson2csvCallback = (exportedFilename) => {
  let json2csvCallback = (err, csv) => {
    if (err) throw err;

    var blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    if (navigator.msSaveBlob) {
      // IE 10+
      navigator.msSaveBlob(blob, exportedFilename);
    } else {
      var link = document.createElement("a");
      if (link.download !== undefined) {
        // feature detection
        // Browsers that support HTML5 download attribute
        var url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", exportedFilename);
        link.style.visibility = "hidden";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
    }
  };
  return json2csvCallback;
};

/** Parse filename from Content-Disposition header (e.g. attachment; filename="export.xlsx"). */
export const getFilenameFromContentDisposition = (headers) => {
  if (!headers) return null;
  const cd =
    typeof headers.get === "function"
      ? headers.get("content-disposition")
      : headers["content-disposition"];
  if (!cd) return null;
  const match = /filename\*?=(?:UTF-8'')?["']?([^"'\s;]+)["']?/i.exec(cd);
  return match ? match[1].trim() : null;
};

/**
 * Resolve a tenant code to its human-readable name using the allTenant list
 * from Redux (state.allTennant.tennants). Falls back to the raw code if no
 * match is found so the UI never shows a blank value.
 *
 * @param {string} tenantCode  - The tenant schema / code (e.g. "smartcity")
 * @param {Array}  allTenants  - Array of { tenantCode, tenantName, ... } from Redux
 * @returns {string}
 */
export const getTenantName = (tenantCode, allTenants = []) => {
  if (!tenantCode) return tenantCode;
  if (!Array.isArray(allTenants) || !allTenants.length) return tenantCode;
  const found = allTenants.find((t) => t.tenantCode === tenantCode);
  return found ? found.tenantName : tenantCode;
};

/** Trigger browser download of a Blob so user can save to local machine. */
export const triggerBlobDownload = (blob, filename) => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename || "download";
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}