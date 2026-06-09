//react
import React from "react";
//third party
import Drawer from "@material-ui/core/Drawer";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import DashboardIcon from "@material-ui/icons/Dashboard";
import TreeView from "@material-ui/lab/TreeView";
import SettingsIcon from "@material-ui/icons/Settings";
import Avatar from "@material-ui/core/Avatar";
import ApartmentIcon from "@material-ui/icons/Apartment";
import { SvgIcon } from "@material-ui/core";
//images
import { ReactComponent as LogoutSVG } from "./logout.svg";
import { InventoryIcon, CRMIcon } from "./../Icons/Index";
//shared
import DashboardItem from "./../../Shared/DashboardItem";
//css
import "./style.scss";
import { removeToken, getRole, getUserName, setSession } from "./../../helper";
import { messages } from "./../../messages";
import { appRoutes } from "./../../endpoints";
import { connect } from "react-redux";
import IconButton from "@material-ui/core/IconButton";
import { withRouter } from "react-router-dom";
import { NavLink } from "react-router-dom";
import ChevronLeftIcon from "@material-ui/icons/ChevronLeft";
import ChangePasswordModal from "../../Modules/ChangePassword";
import { toggleChangePasswordModal } from "../../actions/settings";
import HomeWorkIcon from "@material-ui/icons/HomeWork";
import DescriptionIcon from "@material-ui/icons/Description";
import { setSideBarValue } from "../../actions/sideMenu";
function LogoutIcon(props) {
  return (
    <SvgIcon {...props}>
      <LogoutSVG />
    </SvgIcon>
  );
}

function SideMenu(props) {
  const currentUserRole = getRole() || "";
  let tennant = props.tennant;
  if (tennant && props.allTenant.length) {
    const selectenant = props.allTenant.filter((t) => t.tenantCode === tennant);
    tennant = selectenant[0] ? selectenant[0] : tennant;
  }
  const isCRM =
    (tennant?.crm === true || tennant?.crm === "true") &&
    (currentUserRole.toLowerCase().indexOf("crm") > -1 ||
      currentUserRole.toLowerCase().indexOf("crm-manager") > -1 ||
      currentUserRole.toLowerCase().indexOf("admin") > -1);
  const _role = currentUserRole.toLowerCase();
  const isInventory =
    (tennant?.inventory === true || tennant?.inventory === "true") &&
    (_role === "admin" ||
      _role === "purchase-manager" ||
      _role === "management" ||
      _role === "project-manager" ||
      _role === "store-incharge");
  const username = getUserName();
  const appendURL = (url) => {
    return url + "?tennant-id=" + tennant.tenantCode;
  };
  const isProjectSelectionPage = (
    props.history.location.pathname === "/project" ||
    props.history.location.pathname === "/globalDashboard" ||
    props.history.location.pathname === "/inventory" ||
    props.history.location.pathname === "/category" ||
    props.history.location.pathname === "/machinery" ||
    props.history.location.pathname === "/inventoryTransfer" ||
    props.history.location.pathname === "/stockSummary" ||
    props.history.location.pathname === "/purchaseOrder" ||
    props.history.location.pathname === "/globalIndent" ||
    props.history.location.pathname === "/contact" ||
    props.history.location.pathname === "/historicalPricing" ||
    props.history.location.pathname === "/user" ||
    props.history.location.pathname === "/firm" ||
    props.history.location.pathname === appRoutes.productMerge ||
    props.history.location.pathname === appRoutes.activityLog ||
    props.history.location.pathname === "/globalBOQ" ||
    props.history.location.pathname === appRoutes.fifoReport ||
    props.history.location.pathname === appRoutes.stockAgingReport
  )
  let isOpenMenu = props.sideMenu.isOpen;
  return (
    <>
      <Drawer
        variant="permanent"
        open={true}
        className={`side-menu ${isOpenMenu ? 'open':'close'}`}
        classes={{
          paper: "side-menu-wrapper",
        }}
      >
        <div className="user">
          <Avatar className="user-image">{username ? username[0] : ""}</Avatar>
          <div className="user-name">{username}</div>
        </div>
        <div className="logo">
          <img
            src={`${process.env.PUBLIC_URL}/${process.env.REACT_APP_LOGIN_LOGO}.png`}
            alt="company logo"
          />
        </div>
        {(tennant && !isProjectSelectionPage) ? (
          <React.Fragment>
            <div className="tennant" onClick={props.setSideBarValue}>
              <NavLink to={appRoutes.project} activeClassName="active">
                <IconButton aria-label="back">
                  <ChevronLeftIcon />
                </IconButton>
                {tennant.tenantName}
              </NavLink>
            </div>

            <TreeView
              defaultExpanded={["3"]}
              defaultExpandIcon={<ChevronRightIcon />}
              defaultCollapseIcon={<ExpandMoreIcon />}
            >
              {(isInventory || isCRM) && (
                  <DashboardItem
                    nodeId="1"
                    labelText={messages.common.dashboard}
                    labelIcon={DashboardIcon}
                    linkurl={appendURL("/dashboard")}
                    onClick={props.setSideBarValue}
                  />
                )}
              {isInventory && (
                <DashboardItem
                  nodeId="5"
                  labelText={messages.common.inventoryManagement}
                  labelIcon={InventoryIcon}
                >
                  <DashboardItem
                    nodeId="13"
                    labelText={messages.common.stock}
                    linkurl={appendURL("/stock")}
                    onClick={props.setSideBarValue}
                  />
                  <DashboardItem
                    nodeId="15"
                    labelText={messages.common.allInventory}
                    linkurl="/allInventory"
                    onClick={props.setSideBarValue}
                  />
                  <DashboardItem
                    nodeId="20"
                    labelText={messages.common.indent}
                    linkurl={appendURL("/indent")}
                    onClick={props.setSideBarValue}
                  />
                  <DashboardItem
                    nodeId="11"
                    labelText={messages.common.inwardInventory}
                    linkurl={appendURL("/inwardInventory")}
                    onClick={props.setSideBarValue}
                  />
                  <DashboardItem
                    nodeId="12"
                    labelText={messages.common.outwardInventory}
                    linkurl={appendURL("/outwardInventory")}
                    onClick={props.setSideBarValue}
                  />

                  <DashboardItem
                    nodeId="14"
                    labelText={messages.common.mor}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/machineryOnRent")}
                  />

                  <DashboardItem
                    nodeId="16"
                    onClick={props.setSideBarValue}
                    labelText={messages.common.lost}
                    linkurl={appendURL("/lost")}
                  />

                  {/* Pricing menu hidden — not in use
                  <DashboardItem
                      nodeId="19"
                      labelText={messages.common.pricingHeader}
                  >
                    <DashboardItem
                        nodeId="22"
                        labelText={messages.common.pricingInventory}
                        onClick={props.setSideBarValue}
                        linkurl={appendURL("/pricingInventory")}
                    ></DashboardItem>
                    <DashboardItem
                        nodeId="32"
                        labelText={messages.common.pricingReport}
                        onClick={props.setSideBarValue}
                        linkurl={appendURL("/pricingReport")}
                    ></DashboardItem>
                  </DashboardItem>
                  */}
                </DashboardItem>
              )}

              {/* {(currentUserRole.toLowerCase().indexOf("crm") > -1 ||
              currentUserRole.toLowerCase().indexOf("admin") > -1) && (
              <DashboardItem
                nodeId="20"
                labelText={messages.common.crm}
                labelIcon={CRMIcon}
              >
                <DashboardItem
                  nodeId="24"
                  labelText={messages.common.lead}
                  linkurl={appendURL(appRoutes.lead)}
                ></DashboardItem>
                <DashboardItem
                  nodeId="25"
                  labelText={messages.common.pipelines}
                  linkurl={appendURL(appRoutes.pipelines)}
                ></DashboardItem>
                <DashboardItem
                  nodeId="26"
                  labelText={messages.common.planner}
                  linkurl={appendURL(appRoutes.planner)}
                ></DashboardItem>
              </DashboardItem>
            )} */}
              {isInventory && (
                <DashboardItem
                  nodeId="14"
                  labelText={"BOQ"}
                  labelIcon={HomeWorkIcon}
                >
                  <DashboardItem
                    nodeId="16"
                    onClick={props.setSideBarValue}
                    labelText={"Input"}
                    linkurl={appendURL("/boq")}
                  />
                  <DashboardItem
                    nodeId="16"
                    onClick={props.setSideBarValue}
                    labelText={"Status"}
                    linkurl={appendURL("/boqStatus")}
                  />
                  <DashboardItem
                    nodeId="16"
                    onClick={props.setSideBarValue}
                    labelText={"History"}
                    linkurl={appendURL("/boqHistory")}
                  />
                  {/* <DashboardItem
                    nodeId="16"
                    onClick={props.setSideBarValue}
                    labelText={"Report"}
                    linkurl={appendURL("/boqReport")}
                  /> */}
                </DashboardItem>
              )}
              {isCRM && (
                <DashboardItem
                  nodeId="20"
                  labelText={messages.common.crm}
                  labelIcon={CRMIcon}
                >
                  <DashboardItem
                    nodeId="24"
                    labelText={messages.common.leadActivity}
                    linkurl={appRoutes.lead}
                    onClick={
                      () => {
                        let filterData = [];
                        filterData = {};
                        setSession("leadfilterdata", filterData);
                        setSession("showLoanStatus", false);
                        props.setSideBarValue();
                    }}
                  ></DashboardItem>
                  <DashboardItem
                    nodeId="25"
                    labelText={messages.common.pipelines}
                    linkurl={appRoutes.pipelines}
                    onClick={props.setSideBarValue}
                  ></DashboardItem>
                  <DashboardItem
                    nodeId="26"
                    labelText={messages.common.planner}
                    linkurl={appRoutes.planner}
                    onClick={props.setSideBarValue}
                  ></DashboardItem>
                </DashboardItem>
              )}
              {isCRM && (
                <DashboardItem
                  nodeId="31"
                  labelText={messages.common.aftersales}
                  labelIcon={ApartmentIcon}
                >
                  <DashboardItem
                    nodeId="29"
                    labelText={messages.common.postsales}
                    linkurl={appRoutes.lead}
                    onClick={
                      () => {
                        let filterData = [];
                        filterData = {
                          leadStatus: ["Deal_Closed"],
                          showOnlyLatest: {
                            name: "Latest Activity Only",
                            value: "true",
                          },
                        };
                        setSession("leadfilterdata", filterData);
                        setSession("showLoanStatus", true);
                        props.setSideBarValue();
                    }}
                  ></DashboardItem>
                  <DashboardItem
                    nodeId="30"
                    labelText={messages.common.payments}
                    linkurl={appRoutes.payments}
                    onClick={props.setSideBarValue}
                  ></DashboardItem>
                  <DashboardItem
                    nodeId="28"
                    labelText={messages.common.projectPlan}
                    onClick={props.setSideBarValue}
                    linkurl={appRoutes.projectPlan}
                  ></DashboardItem>
                </DashboardItem>
              )}
              <DashboardItem
                nodeId="17"
                labelText={messages.common.settings}
                labelIcon={SettingsIcon}
              >
                <DashboardItem
                  nodeId="3"
                  labelText={messages.common.changePassword}
                  // linkurl={appendURL(appRoutes.changePassword)}
                  onClick={() => props.toggleChangePasswordModal()}
                />
                {currentUserRole.toLowerCase() === "admin" && (
                  <DashboardItem nodeId="2" labelText={messages.common.admin}>
                    <DashboardItem
  nodeId="51"
  labelText={messages.common.user}
  onClick={props.setSideBarValue}
  linkurl="/user"
/>
                    {/* <DashboardItem
                    nodeId="4"
                    labelText={messages.common.role}
                    linkurl={appendURL("/role")}
                  /> */}
                    <DashboardItem
                      nodeId="54"
                      labelText="Activity Log"
                      onClick={props.setSideBarValue}
                      linkurl={appendURL(appRoutes.projectActivityLog)}
                    />
                  </DashboardItem>
                )}
                {isCRM && (
                  <DashboardItem nodeId="27" labelText={messages.common.crm}>
                    <DashboardItem
                      nodeId="23"
                    onClick={props.setSideBarValue}
                    labelText={messages.common.source}
                      linkurl={appendURL(appRoutes.source)}
                    ></DashboardItem>
                    <DashboardItem
                      nodeId="21"
                    onClick={props.setSideBarValue}
                    labelText={messages.common.broker}
                      linkurl={appendURL(appRoutes.broker)}
                    ></DashboardItem>
                  </DashboardItem>
                )}
                {isInventory && (
                  <DashboardItem
                    nodeId="18"
                    labelText={messages.common.inventoryManagement}
                  >
                    <DashboardItem
                      nodeId="6"
                      labelText={messages.common.warehouse}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/warehouse")}
                    />
                    <DashboardItem
                      nodeId="36"
                      labelText={messages.common.buildingType}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/buildingType")}
                    />
                    <DashboardItem
                      nodeId="10"
                      labelText={messages.common.location}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/usageLocation")}
                    />
                    <DashboardItem
                      nodeId="17"
                      labelText={messages.common.finalLocation}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/usageArea")}
                    />
                    {/* <DashboardItem
                      nodeId="7"
                      labelText={messages.common.category}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/category")}
                    />
                    <DashboardItem
                      nodeId="8"
                      labelText={messages.common.inventory}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/inventory")}
                    /> */}
                    <DashboardItem
                      nodeId="9"
                      labelText={messages.common.machinery}
                    onClick={props.setSideBarValue}
                    linkurl={appendURL("/machinery")}
                    />
                  </DashboardItem>
                )}
              </DashboardItem>
              <DashboardItem
                nodeId="18"
                labelText={messages.common.logout}
                labelIcon={LogoutIcon}
                onClick={() => {
                  removeToken();
                  window.location.href = window.location.origin + '/#' + appRoutes.login;
                }}
              />
            </TreeView>
          </React.Fragment>
        ) : (
          <TreeView
            defaultExpanded={["3"]}
            defaultExpandIcon={<ChevronRightIcon />}
            defaultCollapseIcon={<ExpandMoreIcon />}
          >
            <DashboardItem
              nodeId="37"
              labelText={messages.common.globalDashboard}
              labelIcon={DashboardIcon}
              onClick={props.setSideBarValue}
              linkurl={appRoutes.globalDashboard}
            />
            <DashboardItem
              nodeId="16"
              labelText={messages.common.projects}
              labelIcon={ApartmentIcon}
              onClick={props.setSideBarValue}
              linkurl="/project"
            />
                          <DashboardItem
  nodeId="38"
  labelText={messages.common.stockSummary}
  labelIcon={InventoryIcon}
  onClick={props.setSideBarValue}
  linkurl="/stockSummary"
/>
            {(_role === "admin" || _role === "project-manager") && (
              <DashboardItem
                nodeId="41"
                labelText={messages.common.globalBOQ}
                labelIcon={HomeWorkIcon}
                onClick={props.setSideBarValue}
                linkurl={appRoutes.globalBOQ}
              />
            )}
            <DashboardItem
              nodeId="39"
              labelText={messages.common.indentPO}
              labelIcon={DescriptionIcon}
            >
              {(currentUserRole.toLowerCase() === "admin" ||
                currentUserRole.toLowerCase() === "purchase-manager" ||
                currentUserRole.toLowerCase() === "project-manager" ||
                currentUserRole.toLowerCase() === "management") && (
                <DashboardItem
                  nodeId="12"
                  labelText={messages.common.indent}
                  onClick={props.setSideBarValue}
                  linkurl="/globalIndent"
                />
              )}
              <DashboardItem
                nodeId="11"
                labelText={messages.common.purchaseOrder}
                onClick={props.setSideBarValue}
                linkurl="/purchaseOrder"
              />
              <DashboardItem
                nodeId="10"
                labelText={messages.common.inventoryTransfer}
                onClick={props.setSideBarValue}
                linkurl="/inventoryTransfer"
              />
              {(currentUserRole.toLowerCase() === "admin" ||
                currentUserRole.toLowerCase() === "purchase-manager" ||
                currentUserRole.toLowerCase() === "management") && (
                <DashboardItem
                  nodeId="40"
                  labelText={messages.common.historicalPricing}
                  onClick={props.setSideBarValue}
                  linkurl={appRoutes.historicalPricing}
                />
              )}

            </DashboardItem>
            {(currentUserRole.toLowerCase() === "admin") && (
  <DashboardItem
    nodeId="50"
    labelText={messages.common.admin}
    labelIcon={SettingsIcon}
  >
    <DashboardItem
  nodeId="51"
  labelText={messages.common.user}
  onClick={() => {
    setSession("tennant-id", null);
    props.setSideBarValue();
  }}
  linkurl={appRoutes.user}
/>
    {username === 'sridhar' && <DashboardItem
  nodeId="52"
  labelText="Merge Products"
  onClick={props.setSideBarValue}
  linkurl={appRoutes.productMerge}
/>}
    <DashboardItem
  nodeId="53"
  labelText="Activity Log"
  onClick={props.setSideBarValue}
  linkurl={appRoutes.activityLog}
/>
  </DashboardItem>
)}
            {(currentUserRole.toLowerCase() === "admin") && (
              <DashboardItem
                nodeId="60"
                labelText="Reports"
                labelIcon={DescriptionIcon}
              >
                <DashboardItem
                  nodeId="61"
                  labelText="FIFO Override Report"
                  onClick={props.setSideBarValue}
                  linkurl={appRoutes.fifoReport}
                />
                <DashboardItem
                  nodeId="62"
                  labelText="Stock Aging Report"
                  onClick={props.setSideBarValue}
                  linkurl={appRoutes.stockAgingReport}
                />
              </DashboardItem>
            )}
            <DashboardItem
              nodeId="17"
              labelText="Global Config"
              labelIcon={SettingsIcon}
            >
              <DashboardItem
                nodeId="38"
                labelText={messages.common.firm}
                onClick={props.setSideBarValue}
                linkurl="/firm"
              />
              <DashboardItem
                nodeId="7"
                labelText={messages.common.category}
                onClick={props.setSideBarValue}
                linkurl="/category"
              />
              <DashboardItem
                nodeId="8"
                labelText={messages.common.inventory}
                onClick={props.setSideBarValue}
                linkurl="/inventory"
              />
              <DashboardItem
                nodeId="9"
                labelText={messages.common.machinery}
                onClick={props.setSideBarValue}
                linkurl="/machinery"
              />
              <DashboardItem
                nodeId="13"
                labelText={messages.common.contact}
                onClick={props.setSideBarValue}
                linkurl="/contact"
              />
            </DashboardItem>
            <DashboardItem
              nodeId="18"
              labelText={messages.common.logout}
              labelIcon={LogoutIcon}
              onClick={() => {
                removeToken();
                window.location.href = window.location.origin + '/#' + appRoutes.login;
              }}
            />
          </TreeView>
        )}
        <div className="sidebar-close-btn">
          <IconButton onClick={props.setSideBarValue} >
            <ChevronLeftIcon fontSize="large"  />
          </IconButton>
        </div>
      </Drawer>
      <div className={`sidebar-backdrop ${isOpenMenu && 'active'} `} onClick={props.setSideBarValue} ></div>
      <ChangePasswordModal
        open={props.openChangePassword}
        onClose={() => props.toggleChangePasswordModal()}
      />
    </>
  );
}
const mapStateToProps = (state) => {
  return {
    tennant: state.tennant.tennant_id,
    allTenant: state.allTennant.tennants,
    allowedtenants: state.allTennant.allowedtenants,
    openChangePassword: state.settings.changePassword,
    sideMenu:state.sideMenu
  };
};

export default connect(mapStateToProps, { toggleChangePasswordModal,setSideBarValue }, null, {
  forwardRef: true,
  setSideBarValue
})(withRouter(SideMenu));
