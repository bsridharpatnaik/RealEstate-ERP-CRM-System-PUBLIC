const INITIAL_STATE = { propertyTypesList: [] };

export const PropertyTypes = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "FETCH_ALL_PROPERTY_TYPES":
      return {
        ...state,
        propertyTypesList: action.payload,
      };
    default:
      return state;
  }
};
