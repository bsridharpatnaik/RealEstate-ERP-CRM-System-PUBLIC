const INITIAL_STATE = { tennant_id: null };

export const Tennant = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "TENNANT_ID":
      return {
        ...state,
        tennant_id: action.payload,
      };
    default:
      return state;
  }
};
