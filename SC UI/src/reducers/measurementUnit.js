const INITIAL_STATE = { units: [] };

export const Units = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case "UNIT_LIST":
      const data = action.payload;
      const obj = {};
      data.forEach((element) => {
        obj[element.productId] = element.measurementUnit;
      });
      return {
        ...state,
        units: { ...obj },
      };
    default:
      return state;
  }
};
