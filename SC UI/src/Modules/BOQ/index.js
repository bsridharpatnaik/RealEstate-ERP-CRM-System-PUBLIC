import React, { Component } from "react";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import TopMenu from "./TopMenu";
import BuildingUnit from "./BuildingUnits";
import BOQTable from "./BOQTable";
import { fetchUnit } from "./../../actions/measurementUnit";
import { connect } from "react-redux";
import Skeleton from "@material-ui/lab/Skeleton";
import BOQInputScreen from "./BOQInputScreen";

//third party
import { withSnackbar } from "notistack";

class BOQ extends Component {
  state = {
    typeLoading: false,
    inventoryLoading: false,
  };
  title = "BOQInput";
  types = [];
  units = [];
  inventoryData = {};
  page = 0;
  componentDidMount() {
    const { dispatch } = this.props;
    dispatch(fetchUnit());
    this.getTypes();
  }
  async getTypes() {
    this.setState({ typeLoading: true });
    const response = await API.GET(apiEndpoints.boqTypes);
    if (response.success) {
      this.types = response.data;
      if (response.data[0]) {
        this.getBuildingsUnderBOQ(response.data[0].id);
        this.selectType(response.data[0]);
      }
    }
    this.setState({ typeLoading: false });
  }
  selectType(type) {
    this.setState({ selectedType: type }, () => this.getTypeInventory());
  }
  selectUnit(unit) {
    this.setState({ selectedUnit: unit }, () => {
      if (unit) {
        this.getUnitInventory();
      } else {
        this.getTypeInventory();
      }
    });
  }
  async getBuildingsUnderBOQ(id) {
    this.setState({ unitLoading: true });
    const response = await API.GET(apiEndpoints.buildingsUnderBOQ + id);
    setTimeout(() => {
      this.setState({ unitLoading: false });
    }, 200);
    if (response.success) {
      this.units = response.data;
    }
  }
  async getTypeInventory() {
    this.setState({ inventoryLoading: true });
    const response = await API.GET(
      apiEndpoints.boqInventoryType +
        this.state.selectedType.id +
        "?page=" +
        this.page +
        "&size=" +
        10
    );
    if (response.success) {
      this.inventoryData = response.data;
    }
    setTimeout(() => {
      this.setState({ inventoryLoading: false });
    }, 200);
  }
  async getUnitInventory() {
    this.setState({ inventoryLoading: true });
    const response = await API.GET(
      apiEndpoints.boqInventoryUnit +
        this.state.selectedUnit.id +
        "?page=" +
        this.page +
        "&size=" +
        10
    );
    if (response.success) {
      this.inventoryData = response.data;
    }
    this.setState({ inventoryLoading: false });
  }
  render() {
    return (
      <div className="boq page">
        <BOQInputScreen/>

        {/* {!this.state.typeLoading ? (
          <TopMenu
            data={this.types}
            activeItem={this.state.selectedType}
            onSelect={(item) => {
              this.getBuildingsUnderBOQ(item);
              let selectedItem = this.types.filter((el) => el.id == item);
              selectedItem = selectedItem[0];
              this.selectType(selectedItem);
            }}
          />
        ) : (
          <Skeleton variant="rect" height={54} />
        )}
        <BuildingUnit
          data={this.units}
          selected={this.state.selectedUnit}
          isLoading={this.state.unitLoading}
          onSelect={(item) => {
            if (
              this.state.selectedUnit &&
              item.id === this.state.selectedUnit.id
            ) {
              this.selectUnit(null);
            } else {
              this.selectUnit(item);
            }
          }}
        />

        <BOQTable
          isLoading={this.state.inventoryLoading}
          units={this.props.units}
          data={this.inventoryData}
          selectedType={this.state.selectedType}
          selectedUnit={this.state.selectedUnit}
          refresh={(page = 0) => {
            this.page = page;
            if (this.state.selectedUnit) {
              this.getUnitInventory();
            } else {
              this.getTypeInventory();
            }
          }}
        /> */}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    units: state.units.units,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(BOQ)
);
