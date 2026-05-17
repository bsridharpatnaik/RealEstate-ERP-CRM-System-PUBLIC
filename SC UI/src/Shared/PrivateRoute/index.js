import React from "react";
import { Route, Redirect } from "react-router-dom";
import { appRoutes } from "../../endpoints";
import { getToken, isAdmin } from "./../../helper";
import { connect } from "react-redux";

const PrivateRoute = (props) => {
  const { component: Component, onlyAdmin, tennant } = props;

  const token = getToken();
  const bAdmin = isAdmin();
  const hasTennant = !!tennant;
  const isProjectPage = props.location.pathname === "/project";
  const renderComponent = hasTennant || isProjectPage;
  let routeEnable = token;
  if (onlyAdmin) {
    routeEnable = token && bAdmin;
  }

  return (
    <Route
      render={(props) =>
        routeEnable ? (
          renderComponent ? (
            <Component {...props} />
          ) : (
            <Redirect to={appRoutes.project} />
          )
        ) : (
          <Redirect to={appRoutes.login} />
        )
      }
    />
  );
};

const mapStateToProps = (state) => {
  return {
    tennant: state.tennant.tennant_id,
  };
};
export default connect(mapStateToProps, {}, null, {
  forwardRef: true,
})(PrivateRoute);
