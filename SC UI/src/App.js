//libraries
import React from "react";
import {
  HashRouter as Router,
  Route,
  Switch,
  Redirect,
} from "react-router-dom";

//misc
import { appRoutes } from "./endpoints";
import PrivateRoute from "./Shared/PrivateRoute";
//routes
import Login from "./Modules/Login";
import Home from "./Modules/Home";
//scss
import "./App.scss";
import { MuiPickersUtilsProvider } from "@material-ui/pickers";
import MomentUtils from "@date-io/moment";
import { connect } from "react-redux";
import { setTenannt, getAllTennants } from "./actions/tennant";
import { API } from "./axios";
import { apiEndpoints } from "./endpoints";
import { getToken, isAdmin } from "./helper";

function App(props) {
  const getAllTenant = async () => {
    const response = await API.GET(apiEndpoints.getAllTennants);
    if (response.success) {
      props.getAllTennants(response.data);
    }
  };
  const token = getToken();
  if (token && props.allTenants.length === 0) {
    getAllTenant();
  }
  let tennant = props.tennant;
  if (!tennant) {
    let params = new URLSearchParams(window.location.hash);
    tennant = params.get("tennant-id");
    if (!tennant) {
      tennant = localStorage.getItem("tennant-id");
    }
    if (tennant && tennant !== "null") {
      props.setTenannt(tennant);
    }
  }

  return (
    <div className="App">
      <React.Fragment>
        <MuiPickersUtilsProvider utils={MomentUtils}>
          <Router basename="/">
            <Switch>
              <Route path={appRoutes.login} component={Login} />
              <PrivateRoute component={Home} path={appRoutes.home} />
              <Redirect from="/" to={appRoutes.home} />
            </Switch>
          </Router>
        </MuiPickersUtilsProvider>
      </React.Fragment>
    </div>
  );
}
const mapStateToProps = (state) => {
  return {
    tennant: state.tennant.tennant_id,
    allTenants: state.allTennant.tennants,
  };
};
export default connect(mapStateToProps, { setTenannt, getAllTennants }, null, {
  forwardRef: true,
})(App);
