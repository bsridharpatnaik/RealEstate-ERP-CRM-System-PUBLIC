import React from "react";
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
import Grid from "@material-ui/core/Grid";
import Fab from "@material-ui/core/Fab";
import AddIcon from "@material-ui/icons/Add";
import CloseIcon from "@material-ui/icons/Close";
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import Link from "@material-ui/core/Link";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";

class RejectProduct extends AddForm {
  // value must be unique per LINE, not per product — a product can appear on two lines
  // if the outward drew it from two different warehouses. Display name is disambiguated
  // with the warehouse name whenever the same product appears more than once.
  sourceDetails = (() => {
    const lines = (this.props?.data?.inwardOutwardList || []).map((item, idx) => ({
      value: `${item.product.productId}_${item.warehouse ? item.warehouse.warehouseId : idx}`,
      productId: item.product.productId,
      warehouseId: item.warehouse ? item.warehouse.warehouseId : null,
      warehouseName: item.warehouse ? item.warehouse.warehouseName : null,
      name: item.product.productName,
      measurementUnit: item.product.measurementUnit,
      quantity: item.quantity,
    }));
    const countByProduct = {};
    lines.forEach((l) => { countByProduct[l.productId] = (countByProduct[l.productId] || 0) + 1; });
    lines.forEach((l) => {
      if (countByProduct[l.productId] > 1 && l.warehouseName) {
        l.name = `${l.name} (${l.warehouseName})`;
      }
    });
    return lines;
  })();
  state = {
    value: 0,
    noproduct: {},
    enableSave: true,
    currentsourceDetails: this.sourceDetails,
  };
  key = 1;

  componentDidMount() {
    this.addDefault();
  }

  addDefault() {
    const p = this.state.noproduct;
    p[this.key++] = {};
    this.setState({ noproduct: { ...p } });
  }
  checkValidation() {
    let isValid = this.key > 1 ? true : false;
    if (isValid) {
      const val = Object.values(this.state.noproduct);
      if (val.length === 0) {
        isValid = false;
      }
      Object.values(this.state.noproduct).map((item) => {
        if (
          item &&
          item.productId &&
          item.returnquantity &&
          item.returnquantity > 0 &&
          item.remarks &&
          isValid
        ) {
          isValid = true;
        } else {
          isValid = false;
        }
        return null;
      });
    }
    this.setState({ enableSave: !isValid });
  }
  renderProductAddButton() {
    return (
      <Fab
        className="add-fab-button"
        color="primary"
        aria-label="add"
        disabled={
          Object.keys(this.state.noproduct).length === this.sourceDetails.length
        }
        onClick={() => {
          const p = this.state.noproduct;
          p[this.key++] = {};
          this.setState({ noproduct: { ...p } }, this.checkValidation());
        }}
      >
        <AddIcon />
      </Fab>
    );
  }

  renderProduct(key) {
    return (
      <Grid container className="product-item" spacing={3} key={key}>
        <Grid item sm={12} md={6}>
          {this.renderAutoComplete({
            fieldname: `productId_${key}`,
            placeholder: messages.common.inventory,
            options: this.state.currentsourceDetails,
            disableClearable: true,
            required: true,
            getOption: (option) => {
              return option["name"];
            },
            onChange: (e, item) => {
              const p = this.state.noproduct;
              // value is a composite per-line key (product can be on two lines from two
              // warehouses) — productId/warehouseId are the real identifiers sent to the backend.
              p[key].value = item.value || "";
              p[key].productId = item.productId || "";
              p[key].warehouseId = item.warehouseId || null;
              this.setState({
                currentsourceDetails: this.state.currentsourceDetails.filter(
                  (x) => x.value !== item.value
                ),
              });
              if (item) {
                p[key].quantity = item.quantity;
                p[key].measurementUnit = item.measurementUnit;
              }
              this.checkValidation();
            },
          })}
        </Grid>
        <Grid item sm={12} md={6}>
          {this.renderTextField({
            fieldname: `returnquantity_${key}`,
            placeholder: "Return Quantity",
            type: "number",
            required: true,
            validation: "nonegative",
            onChange: (value) => {
              const p = this.state.noproduct;
              p[key].returnquantity = value;
              this.checkValidation();
            },
          })}
        </Grid>
        <Grid item sm={12} md={12}>
          {this.renderTextField({
            fieldname: `remarks_${key}`,
            placeholder: "Remarks",
            required: true,
            validation: "maxlength",
            lengthConstraint: 100,
            onChange: (value) => {
              const p = this.state.noproduct;
              p[key].remarks = value;
              this.checkValidation();
            },
          })}
        </Grid>
        <Grid item container sm={12} md={12} justify="flex-end">
          <Link
            component="button"
            variant="body2"
            color="error"
            underline="none"
            onClick={() => {
              const p = this.state.noproduct;
              const updatedsourceDetails = this.state.currentsourceDetails;
              p &&
                p[key].value &&
                updatedsourceDetails.push(
                  this.sourceDetails.filter(
                    (x) => x.value === p[key].value
                  )[0]
                );
              this.setState({ currentsourceDetails: updatedsourceDetails });
              delete p[key];
              this.setState({ noproduct: { ...p } }, this.checkValidation());
            }}
          >
            Remove
          </Link>
        </Grid>
      </Grid>
    );
  }

  renderHeader() {
    return (
      <div className="filter-heading">
        <span className="filter-title">{"Reject Product"}</span>
        <IconButton
          aria-label="back"
          onClick={this.props.close}
          className="close-icon"
        >
          <CloseIcon />
        </IconButton>
      </div>
    );
  }

  renderFooter() {
    return (
      <div className="product-footer">
        <Button
          onClick={this.props.close}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          type="submit"
          buttonClass="blue"
          label={messages.common.save}
          disabled={this.state.enableSave}
          onClick={async () => {
            if (this.submited) {
              return;
            }
            this.submited = true;
            this.setState({ enableSave: true });
            const response = await this.saveReturn();
            this.setState({ enableSave: false });
            this.submited = false;
            if (response.success) {
              this.props.closeDetails();
              this.props.goToDetails();
            }
          }}
        />
      </div>
    );
  }

  async saveReturn() {
    if (Object.keys(this.state.noproduct).length === 0) {
      this.props.enqueueSnackbar("Add atleast one Product", {
        variant: "error",
      });
      return;
    }
    const data = Object.values(this.state.noproduct).map((item) => {
      return {
        productId: item.productId,
        warehouseId: item.warehouseId,
        quantity: Number(item.returnquantity),
        remarks: item.remarks,
      };
    });

    const params = {
      productWithQuantities: data,
    };
    const url =
      apiEndpoints.outwardReject + this.props?.data.outwardid + "?type=reject";
    const response = await API.PATCH(url, params);
    this.showToaster(response);
    return response;
  }

  showToaster(response) {
    if (!response.success) {
      const message = response.errorMessage || "Something went wrong";
      this.props.enqueueSnackbar(message, {
        variant: "error",
      });
    }
  }

  render() {
    return (
      // <div class="filter-container product-container reject-product">
      //   {this.renderHeader()}
      //   <div className="product-item-wrapper">
      //     {Object.keys(this.state.noproduct).map((key) =>
      //       this.renderProduct(key)
      //     )}
      //   </div>
      //   <div class="product-footer-container">{this.renderFooter()}</div>
      // </div>
      <Dialog
        open={this.props.open}
        disableBackdropClick={false}
        maxWidth="sm"
        fullWidth={true}
        className="reject-product"
        onClose={this.props.close}
      >
        <DialogTitle>{this.renderHeader()}</DialogTitle>
        <DialogContent dividers>
          <div className="product-item-wrapper">
            {Object.keys(this.state.noproduct).map((key) =>
              this.renderProduct(key)
            )}
          </div>
          <Grid container item sm={12} justify="flex-end">
            {this.renderProductAddButton()}
          </Grid>
        </DialogContent>
        <DialogActions>{this.renderFooter()}</DialogActions>
      </Dialog>
    );
  }
}

export default withSnackbar(RejectProduct);
