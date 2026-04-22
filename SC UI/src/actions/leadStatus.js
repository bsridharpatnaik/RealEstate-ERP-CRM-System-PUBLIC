import { apiEndpoints } from "./../endpoints";
import { API } from "./../axios";
export const getLeadStatus = () => {
  return async (dispatch) => {
    const response = await API.GET(apiEndpoints.getValidLeadStatus);
    if (response.success) {
      dispatch({
        type: "LEAD_STATUS",
        payload: response.data,
      });
    }
  };
};
