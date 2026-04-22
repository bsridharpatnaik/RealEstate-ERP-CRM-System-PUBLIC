const INITIAL_STATE = { categories: [] };

export const Categories = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "CATEGORY_LIST":
      return {
        ...state,
        categories: [...action.payload],
      };
    default:
      return state;
  }
};
