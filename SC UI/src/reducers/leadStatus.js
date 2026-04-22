const INITIAL_STATE = { leadStatus: null };

export const LeadStatus = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "LEAD_STATUS":
      return {
        leadStatus: action.payload,
      };
    default:
      return state;
  }
};
