import { apiEndpoints } from "./../endpoints";
import { API } from "./../axios";
import { setSession, getSession } from "./../helper";
export const getDealLostReasons = () => {
  return async (dispatch) => {
    let response;
    response = getSession("dealLostReasons");
    if (response) {
      dispatch({
        type: "DEAL_LOST_REASONS",
        payload: response,
      });
      return;
    } else {
      response = await API.GET(apiEndpoints.dealLostReasons);
    }
    if (response.success) {
      setSession("dealLostReasons", response.data);

      dispatch({
        type: "DEAL_LOST_REASONS",
        payload: response.data,
      });
    }
  };
};
