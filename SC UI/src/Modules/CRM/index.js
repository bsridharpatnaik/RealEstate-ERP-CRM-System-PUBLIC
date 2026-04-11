import React, { Component } from "react";
import { Route, Switch } from "react-router-dom";
import Broker from "./Broker";
import Sentiment from "./Sentiment";
import Source from "./Source";
import Lead from "./Lead";
import LeadInfo from "./LeadInfo";
import CustomerInfo from "./LeadInfo/CustomerInfo";
import Pipeline from "./Pipeline";
import Planner from "./Planner";
import ProjectPlan from "./ProjectPlan";
import CRMDashboard from './Dashboard';
import PostSales from './PostSales';
import Payments from './Payments';
import { appRoutes } from "./../../endpoints";
class index extends Component {
  render() {
    return (
      <Switch>
        <Route path={appRoutes.broker} component={Broker} />
        <Route path={appRoutes.sentiment} component={Sentiment} />
        <Route path={appRoutes.source} component={Source} />
        <Route path={appRoutes.lead} component={Lead} />
        <Route path={appRoutes.pipelines} component={Pipeline} />
        <Route path={appRoutes.planner} component={Planner} />
        <Route path={appRoutes.projectPlan} component={ProjectPlan} />
        <Route path={appRoutes.leadinfo} component={LeadInfo} />
        <Route path={appRoutes.customerinfo} component={CustomerInfo} />
        <Route path={appRoutes.crmDashboard} component={CRMDashboard} />
        <Route path={appRoutes.postsales} component={PostSales} />
        <Route path={appRoutes.payments} component={Payments} />
      </Switch>
    );
  }
}

export default index;
