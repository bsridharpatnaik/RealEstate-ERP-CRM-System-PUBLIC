const INITIAL_STATE = { tennants: [], allowedtenants: [] };

export const AllTennants = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "TENNANT_LIST":
      return {
        ...state,
        tennants: action.payload,
      };
    case "ALLOWED_LIST":
      return {
        ...state,
        allowedtenants: action.payload,
      };
    default:
      return state;
  }
};
