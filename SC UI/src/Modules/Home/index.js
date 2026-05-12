import React from "react";
import "./style.scss";
import MenuIcon from '@material-ui/icons/Menu';
import { IconButton } from "@material-ui/core";
import {
  HashRouter as Router,
  Route,
  Switch,
  Redirect,
} from "react-router-dom";
import {
  User,
  Role,
  Contact,
  Warehouse,
  Category,
  Product,
  Machinery,
  UsageLocation,
  InwardInventory,
  OutwardInventory,
  Stock,
  MOR,
  AllInventory,
  Lost,
  UsageArea,
  Project,
  BuildingType,
  ChangePassword,
  BOQ,
  BOQStatus,
  BOQHistory,
  Activity,
  Indent,
  PurchaseOrder,
  InventoryTransfer,
  DeadStock,
  HistoricalPricing,
  Firm,
  ProductMerge,
} from "./../index";
import BOQReport from "../BOQReport";

import CRM from "./../CRM";
import DashboardwithTabs from "./../Dashboard/Dashboard";
import GlobalDashboard from "./../GlobalDashboard";
import SideMenu from "./../../Shared/SideMenu";
import PrivateRoute from "./../../Shared/PrivateRoute";
import { appRoutes } from "./../../endpoints";
import { setSideBarValue } from "../../actions/sideMenu";
import { connect } from "react-redux";
import Pricing from "../Pricing";
import PricingReport from "../PricingReport";
class Home extends React.Component {
  render() {
    const { props } = this;
    return (
      <React.Fragment>
        <Router basename="/" forceRefresh>
          <div className="home">
            <SideMenu />
            <div className="mobile-header">
              <div>
                <IconButton onClick={this.props.setSideBarValue}>
                  <MenuIcon fontSize="large" />
                </IconButton>
              </div>
            </div>
            <main>
              <Switch>
                <PrivateRoute
                  component={(props) => <Project {...props} key={new Date()} />}
                  path={appRoutes.project}
                />
                <Route path={appRoutes.globalDashboard}>
                  <GlobalDashboard key={new Date()} {...props} />
                </Route>
                <PrivateRoute
                  component={(props) => <User {...props} key={new Date()} />}
                  path={appRoutes.user}
                  onlyAdmin={true}
                />
                <PrivateRoute
                  component={(props) => <Role key={new Date()} {...props} />}
                  path={appRoutes.role}
                  onlyAdmin={true}
                />
                <Route path={appRoutes.changePassword}>
                  <ChangePassword key={new Date()} {...props} />
                </Route>
                <Route path={appRoutes.boq}>
                  <BOQ key={new Date()} {...props} />
                </Route>
                <Route path={appRoutes.boqStatus}>
                  <BOQStatus key={new Date()} {...props} />
                </Route>
                <Route path={appRoutes.boqHistory}>
                  <BOQHistory key={new Date()} {...props} />
                </Route>
                <Route path={appRoutes.activityLog}>
                  <Activity key={new Date()} {...props} />
                </Route>
                <Route path={"/boqReport"}>
                  <BOQReport key={new Date()} {...props} />
                </Route>
                <Route path={"/contact"}>
                  <Contact key={new Date()} {...props} />
                </Route>
                <Route path={"/category"}>
                  <Category key={new Date()} {...props} />
                </Route>
                <Route path={"/buildingType"}>
                  <BuildingType key={new Date()} {...props} />
                </Route>
                <Route path={"/warehouse"}>
                  <Warehouse key={new Date()} {...props} />
                </Route>
                <Route path={"/firm"}>
                  <Firm key={new Date()} {...props} />
                </Route>
                <Route path={appRoutes.productMerge}>
                  <ProductMerge key={new Date()} {...props} />
                </Route>
                <Route path={"/inventory"}>
                  <Product key={new Date()} {...props} />
                </Route>
                <Route path={"/machinery"}>
                  <Machinery key={new Date()} {...props} />
                </Route>
                <Route path={"/usageLocation"}>
                  <UsageLocation key={new Date()} {...props} />
                </Route>
                <Route path={"/pricingInventory"}>
                  <Pricing key={new Date()} {...props} />
                </Route>
                <Route path={"/pricingReport"}>
                  <PricingReport key={new Date()} {...props} />
                </Route>
                <Route path={"/indent"}>
                  <Indent key={new Date()} {...props} />
                </Route>
                <Route path={"/globalIndent"}>
                  <Indent key={new Date()} {...props} isGlobal={true} />
                </Route>
                <Route path={"/purchaseOrder"}>
                  <PurchaseOrder key={new Date()} {...props} />
                </Route>
                <Route path={"/inventoryTransfer"}>
                  <InventoryTransfer key={new Date()} {...props} />
                </Route>
                <Route path={"/deadStock"}>
                  <DeadStock key={new Date()} {...props} />
                </Route>
                <Route path={appRoutes.historicalPricing}>
                  <HistoricalPricing key={new Date()} {...props} />
                </Route>
                <Route path={"/inwardInventory"}>
                  <InwardInventory key={new Date()} {...props} />
                </Route>
                <Route path={"/outwardInventory"}>
                  <OutwardInventory key={new Date()} {...props} />
                </Route>
                <Route path={"/stock"}>
                  <Stock key={new Date()} {...props} />
                </Route>
                <Route path={"/machineryOnRent"}>
                  <MOR key={new Date()} {...props} />
                </Route>
                <Route path={"/allInventory/:filter?"}>
                  <AllInventory key={new Date()} {...props} />
                </Route>
                <Route path={"/lost"}>
                  <Lost key={new Date()} {...props} />
                </Route>
                <Route path={"/usageArea"}>
                  <UsageArea key={new Date()} {...props} />
                </Route>
                <Route path={"/dashboard"}>
                  <DashboardwithTabs key={new Date()} {...props} />
                </Route>
                <Route path={"/crm"}>
                  <CRM key={new Date()} {...props} />
                </Route>
                <Redirect from="/" to={appRoutes.globalDashboard} />
              </Switch>
            </main>
          </div>
        </Router>
      </React.Fragment>
    );
  }
}

const mapStateToProps = (state) => ({
  sideMenu: state.sideMenu
});

export default connect(mapStateToProps, {
  setSideBarValue
})(Home);
