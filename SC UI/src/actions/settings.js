export function toggleChangePasswordModal() {
    return async (dispatch) => {
        return dispatch({
            type: "TOGGLE_CHANGE_PASSWORD_MODAL",
        });
    };
}
