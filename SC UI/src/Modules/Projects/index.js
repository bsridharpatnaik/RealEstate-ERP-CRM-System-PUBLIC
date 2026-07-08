import React, { Component } from "react";
import { API } from "./../../axios";
import { apiEndpoints, appRoutes } from "./../../endpoints";
import "./style.scss";
import Image1 from "./image1.png";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import CallMadeIcon from "@material-ui/icons/CallMade";
import { setTenannt } from "./../../actions/tennant";
import { setInventoryEditDays } from "./../../helper";
import { connect } from "react-redux";

class Project extends Component {
  projects = [];
  images = {
    kalpavrish: "/kalpavrish.png",
    riddhisiddhi: "/riddhisiddhi.jpg",
    suncitynx: "/suncitynx.jpg",
    drgtrdcntr: "/drgtrdcntr.png",
    smartcity: "/smartcity.jpg",
    businesspark: "/businesspark.jpg",
    egcity: "/eg-login-logo.png",
    citycenter: "/citycentre.png",
    iseries: "/school.jpg",
    bhaavbhumi:"/bhaavbhumi.jpg",
    mnglmcity:"/mangalam.png",
    mhvrtrdcntr:"/mhvrtrdcntr.jpg",
    mgrental:"/mgrental.jpeg",
    dextension: "/dextension.png",
    bextension: "/bextension.png",
    anantamsamosharan: "anantamsamosharan.png",
    bbextension: "/bbextension.png",
  };
  async componentDidMount() {
    this.setState({ isLoading: true });
    if (this.props.allowedtenants.length === 0) {
      const response = await API.GET(apiEndpoints.getTenants);

      if (response.success) {
        this.projects = response.data;
      }
    } else {
      this.projects = this.props.allowedtenants;
    }
    this.setState({ isLoading: false });

    if (false && this.props.tennant && this.props.allowedtenants.length > 1) {
      this.setTennant(null);
    } else if (this.props.allowedtenants.length === 1) {
    }
  }
  async setTennant(tenantCode) {
    // Persist selected tenant (project) in redux + localStorage
    await this.props.setTenannt(tenantCode);

    localStorage.setItem("tennant-id", tenantCode);

    if (tenantCode) {
      // Once tenant-id is set, fetch inventory project constants
      // so that edit/create date restrictions are controlled by backend.
      const response = await API.GET(apiEndpoints.inventoryProjectConstants);
      if (response && response.success && Array.isArray(response.data)) {
        // Backend returns list like:
        // [
        //   { key: "INVENTORY_ALLOWED_DAYS_ADMIN", value: 100 },
        //   { key: "INVENTORY_ALLOWED_DAYS_MANAGER", value: 100 },
        //   { key: "INVENTORY_ALLOWED_DAYS_EXECUTIVE", value: 100 }
        // ]
        const configList = response.data;

        const getValueByKey = (key) => {
          const item = configList.find((c) => c.key === key);
          if (!item || item.value == null) return undefined;
          const num = Number(item.value);
          return Number.isFinite(num) ? num : undefined;
        };

        const adminDays = getValueByKey("INVENTORY_ALLOWED_DAYS_ADMIN");
        const managerDays = getValueByKey("INVENTORY_ALLOWED_DAYS_MANAGER");
        const generalDays = getValueByKey("INVENTORY_ALLOWED_DAYS_EXECUTIVE");

        setInventoryEditDays({ adminDays, managerDays, generalDays });
      }

      this.props.history.push(
        appRoutes.dashboard + "?tennant-id=" + tenantCode
      );
    }
  }
  render() {
    return (
      <div className="project-page">
        <Grid container spacing={3} alignItems="stretch">
          {this.projects.map((project) => {
            return (
              <Grid
                item
                xs={12}
                sm={6}
                md={4}
                lg={3}
                xl={3}
                key={project.tenantCode}
                style={{ height: "100%" }}
              >
                <Paper
                  className="project-wrapper"
                  onClick={() => {
                    this.setTennant(project.tenantCode);
                  }}
                >
                  <div className="project-card">
                    <div className="project-card-image">
                      <img
                        className="image"
                        src={
                          process.env.PUBLIC_URL +
                          this.images[project.tenantCode]
                        }
                        alt={project.tenantName}
                      />
                    </div>
                    <div className="project-card-footer">
                      <div className="project-card-title">
                        {project.tenantName}
                      </div>
                      <CallMadeIcon className="project-card-arrow" />
                    </div>
                  </div>
                </Paper>
              </Grid>
            );
          })}
        </Grid>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    tennant: state.tennant.tennant_id,
    allowedtenants: state.allTennant.allowedtenants,
  };
};
export default connect(mapStateToProps, { setTenannt }, null, {
  forwardRef: true,
})(Project);
