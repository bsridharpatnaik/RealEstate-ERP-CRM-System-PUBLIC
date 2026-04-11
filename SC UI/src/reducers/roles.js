const INITIAL_STATE = { roles: [] };

export const Roles = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "ROLES_LIST":
      return {
        ...state,
        roles: [...action.payload],
      };
    default:
      return state;
  }
};
