import React, { Component } from "react";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import Dashboard from "./index";
import CRMDashboard from "../CRM/Dashboard/index";
import { getRole } from "../../helper";
import { connect } from "react-redux";
import { withRouter } from "react-router";

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <Typography
      component="div"
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box className="padding-0" p={3}>
          {children}
        </Box>
      )}
    </Typography>
  );
}

class DashboardwithTabs extends Component {
  state = {
    value: 0,
    isCRM: false,
    isInventory: false,
  };

  async componentDidMount() {
    this.updateTenantState();
  }

  async componentDidUpdate(prevProps) {
    if (this.props.tennant !== prevProps.tennant || this.props.allTenant !== prevProps.allTenant) {
      this.updateTenantState();
    }
  }

  updateTenantState = async () => {
    let tennant = this.props.tennant;
    if (tennant && this.props.allTenant.length) {
      const selectenant = this.props.allTenant.filter((t) => t.tenantCode === tennant);
      tennant = selectenant[0] ? selectenant[0] : tennant;
    }
    const isCRM = tennant?.crm ?? false;
    const isInventory = tennant?.inventory ?? false;

    if (eval(isCRM)) {
      const role = getRole().toLowerCase();
      const isInventoryRole = role === "admin" || role === "purchase-manager" ||
        role === "management" || role === "project-manager" || role === "store-incharge";
      const isCRMRole = role.indexOf("crm") > -1 || role.indexOf("crm-manager") > -1;
      if (role === "admin") {
        this.setState({ isCRM: true, isInventory: true });
      } else if (isCRMRole) {
        this.setState({ isCRM: true, isInventory: false });
      } else if (isInventoryRole) {
        this.setState({ isCRM: false, isInventory: true });
      }
    } else {
      this.setState({ isCRM, isInventory });
    }
  };

  render() {
    return (
      <div className="">
        <Tabs
          indicatorColor="primary"
          textColor="primary"
          onChange={(e, value) => this.setState({ value: value })}
          value={this.state.value}
        >
          {this.state.isCRM && <Tab label="CRM Dashboard" id="simple-tabpanel-0" />}
          {this.state.isInventory && <Tab label="Inventory Dashboard" id="simple-tabpanel-1" />}
        </Tabs>
        {this.state.isInventory && this.state.isCRM && (
          <TabPanel value={this.state.value} index={0}>
            <CRMDashboard history={this.props.history} />
          </TabPanel>
        )}
        {this.state.isCRM && this.state.isInventory && (
          <TabPanel value={this.state.value} index={1}>
            <Dashboard />
          </TabPanel>
        )}
        {this.state.isInventory && !this.state.isCRM && (
          <TabPanel value={this.state.value} index={0}>
            <Dashboard />
          </TabPanel>
        )}
        {this.state.isCRM && !this.state.isInventory && (
          <TabPanel value={this.state.value} index={0}>
            <CRMDashboard history={this.props.history} />
          </TabPanel>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    tennant: state.tennant.tennant_id,
    allTenant: state.allTennant.tennants,
    allowedtenants: state.allTennant.allowedtenants,
  };
};

export default connect(mapStateToProps, {}, null, { forwardRef: true })(withRouter(DashboardwithTabs));
