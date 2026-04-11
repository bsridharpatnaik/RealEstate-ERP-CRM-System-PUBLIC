import { apiEndpoints } from "./../endpoints";
import { API } from "./../axios";
export const fetchUnit = () => {
  return async (dispatch) => {
    const response = await API.GET(apiEndpoints.getMeasurement);
    if (response.success) {
      dispatch({
        type: "UNIT_LIST",
        payload: response.data,
      });
    }
  };
};
