import React from "react";
import { connect } from "react-redux";
import PropertyTypesListItem from "./PropertyTypes/PropertyTypesListItem";
import { fetchPropertyTypes } from "../../../actions/propertyTypes";
import { apiEndpoints } from "../../../endpoints";
import { messages } from "../../../messages";
import { withSnackbar } from "notistack";
import ListCommon from "../../../Shared/List";
import DeleteConfirm from "../../../Shared/DeleteConfirm";

class List extends ListCommon {
  state = { deleteConfirmOpen: false, rowItem: null };
  async componentDidMount() {
    this.search();
  }

  async search() {
    this.props.fetchPropertyTypes();
  }

  render() {
    return (
      <div className="project-plan-container">
        {this.props.propertyTypesList.length > 0 ? (
          this.props.propertyTypesList.map((property, i) => (
            <PropertyTypesListItem
              key={i}
              property={property}
              editable={this.props.editable}
              onEdit={this.props.edit}
              onDelete={(type, rowItem) => {
                if (type === "MAIN_TYPE") {
                  this.deleteKey = "propertyTypeId";
                  this.deleteUrl = apiEndpoints.properyTypes;
                  this.title = messages.common.propertyType;
                } else {
                  this.deleteKey = "propertyNameId";
                  this.deleteUrl = apiEndpoints.properySubTypes;
                  this.title = messages.common.propertySubTypes;
                }
                // this.delete(row)
                this.setState({ deleteConfirmOpen: true, rowItem });
              }}
              subTypeEdit={this.props.subTypeEdit}
              fetchlist={this.props.fetchPropertyTypes}
            />
          ))
        ) : (
          <div>No Data Found</div>
        )}
        <DeleteConfirm
          open={this.state.deleteConfirmOpen}
          onConfirm={() => {
            this.delete(this.state.rowItem);
            this.setState({ deleteConfirmOpen: false });
          }}
          onCancel={() => {
            this.setState({ deleteConfirmOpen: false, rowItem: null });
          }}
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    propertyTypesList: state.propertyTypes.propertyTypesList,
  };
};
const ConnectedComponent = connect(
  mapStateToProps,
  { fetchPropertyTypes },
  null,
  {
    forwardRef: true,
  }
)(List);

export default withSnackbar(ConnectedComponent);
