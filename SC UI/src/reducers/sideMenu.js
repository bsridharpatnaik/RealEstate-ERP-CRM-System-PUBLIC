const INITIAL_STATE = { isOpen: false };

export const sideMenu = (state = INITIAL_STATE, action) => {
  switch (action.type) {
      case "SET_SIDE_BAR_VALUE":
      return {
        ...state,
        isOpen: !state.isOpen,
      };
    default:
      return state;
  }
};
