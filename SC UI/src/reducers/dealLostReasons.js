const INITIAL_STATE = { dealLostReasons: null };

export const DealLostReasons = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "DEAL_LOST_REASONS":
      return {
        dealLostReasons: action.payload,
      };
    default:
      return state;
  }
};
