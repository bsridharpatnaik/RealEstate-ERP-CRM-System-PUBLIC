import { combineReducers } from "redux";
import { Roles } from "./roles";
import { Categories } from "./categories";
import { Units } from "./measurementUnit";
import { LeadStatus } from "./leadStatus";
import { Tennant } from "./tennant";
import { AllTennants } from "./allTennant";
import { Settings } from "./settings";
import { PropertyTypes } from "./propertyTypes";
import { DealLostReasons } from "./dealLostReasons";
import { sideMenu } from "./sideMenu";
export default combineReducers({
  roles: Roles,
  sideMenu: sideMenu,
  categories: Categories,
  units: Units,
  leadStatus: LeadStatus,
  tennant: Tennant,
  allTennant: AllTennants,
  settings: Settings,
  propertyTypes: PropertyTypes,
  dealLostReasons: DealLostReasons,
});
