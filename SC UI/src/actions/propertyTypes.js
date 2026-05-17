import { apiEndpoints } from "./../endpoints";
import { API } from "./../axios";

export const fetchPropertyTypes = () => {
    return async (dispatch) => {
        const response = await API.GET(apiEndpoints.properyTypes);
        if (response.success) {
            dispatch({
                type: "FETCH_ALL_PROPERTY_TYPES",
                payload: response.data,
            });
        }
    };
};
