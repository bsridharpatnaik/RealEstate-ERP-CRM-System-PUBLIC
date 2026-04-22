export function getCategories(data) {
  return async (dispatch) => {
    return dispatch({
      type: "CATEGORY_LIST",
      payload: data,
    });
  };
}
