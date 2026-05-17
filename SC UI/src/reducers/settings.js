const INITIAL_STATE = { changePassword: false };

export const Settings = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "TOGGLE_CHANGE_PASSWORD_MODAL":
      return {
        ...state,
        changePassword: !state.changePassword,
      };
    default:
      return state;
  }
};
