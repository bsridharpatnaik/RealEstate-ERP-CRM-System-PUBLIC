import { API } from "./axios";
import { apiEndpoints } from "./endpoints";

/**
 * Fetch a single entity by relation (from status-history relations or inward links).
 * @param {{ relationType: 'INDENT'|'PO'|'INWARD', referenceId: string, tenant?: string }} relation
 * @returns {Promise<{ success: boolean, data?: object, errorMessage?: string }>}
 */
export async function fetchEntityByRelation(relation) {
  const { relationType, referenceId, tenant } = relation || {};
  if (!relationType || !referenceId) {
    return { success: false, errorMessage: "Invalid relation" };
  }

  if (relationType === "INDENT") {
    const response = await API.GET(apiEndpoints.getIndentDetail + referenceId);
    if (!response.success) return { success: false, errorMessage: response.errorMessage };
    const d = response.data || {};
    return {
      success: true,
      data: { 
        ...d, 
        status: d.status != null ? d.status : d.indentStatus,
        inventoryItems: d.inventoryList || [],
        inventoryCount: d.inventoryList?.length || 0,
      },
    };
  }

  if (relationType === "PO") {
    const response = await API.GET(apiEndpoints.getPurchaseOrderDetail(referenceId));
    if (!response.success) return { success: false, errorMessage: response.errorMessage };
    return { success: true, data: response.data || {} };
  }

  if (relationType === "INWARD") {
    const url = apiEndpoints.getInwardInventoryDetail + referenceId;
    const config = tenant ? { headers: { "tenant-id": tenant } } : {};
    const response = await API.GET(url, config);
    if (!response.success) return { success: false, errorMessage: response.errorMessage };
    return { success: true, data: response.data };
  }

  return { success: false, errorMessage: "Unknown relation type" };
}
