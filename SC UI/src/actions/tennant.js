export function setTenannt(tennantid) {
  return async (dispatch) => {
    return dispatch({
      type: "TENNANT_ID",
      payload: tennantid,
    });
  };
}
export function getAllTennants(data) {
  return async (dispatch) => {
    return dispatch({
      type: "TENNANT_LIST",
      payload: data,
    });
  };
}
export function getAllowedTennants(data) {
  return async (dispatch) => {
    return dispatch({
      type: "ALLOWED_LIST",
      payload: data,
    });
  };
}
