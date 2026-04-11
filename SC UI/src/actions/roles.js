export function getRoles(data) {
  return async (dispatch) => {
    return dispatch({
      type: "ROLES_LIST",
      payload: data,
    });
  };
}
