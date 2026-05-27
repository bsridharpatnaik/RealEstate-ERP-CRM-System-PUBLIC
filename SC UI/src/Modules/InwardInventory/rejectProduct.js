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
  submited = false;
  sourceDetails = this.props?.data?.inwardOutwardList.map((item) => {
    return {
      value: item.product.productId,
      name: item.product.productName,
      measurementUnit: item.product.measurementUnit,
      quantity: item.quantity,
    };
  });
  state = {
    value: 0,
    noproduct: {},
    enableSave: true,
    currentsourceDetails: this.sourceDetails,
    inwardBatches: [],   // all InventoryBatch records for this inward
  };
  key = 1;

  componentDidMount() {
    this.addDefault();
    this.loadInwardBatches();
  }

  async loadInwardBatches() {
    const inwardId = this.props?.data?.inwardId;
    if (!inwardId) return;
    const r = await API.GET(apiEndpoints.getInwardBatches(inwardId));
    if (r.success) {
      this.setState({ inwardBatches: r.data || [] });
    }
  }

  // Returns batches for a given productId (from loaded inward batches)
  getBatchesForProduct(productId) {
    return this.state.inwardBatches.filter(
      (b) => b.product && b.product.productId === productId && b.qtyRemaining > 0
    );
  }

  addDefault() {
    const p = this.state.noproduct;
    p[this.key++] = {};
    this.setState({ noproduct: { ...p } });
  }
  checkValidation() {
    const products = Object.values(this.state.noproduct || {});
    const hasProducts = products.length > 0;

    const allValid =
      hasProducts &&
      products.every((item) => {
        if (!item || !item.productId || !item.remarks) return false;
        const batches = this.getBatchesForProduct(item.productId);
        if (batches.length > 1) {
          // multi-batch: total qty must be > 0
          const total = Object.values(item.batchQtys || {}).reduce((s, v) => s + (v || 0), 0);
          return total > 0;
        }
        return item.returnquantity && Number(item.returnquantity) > 0;
      });

    this.setState({ enableSave: !allValid });
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
          const p = { ...this.state.noproduct };
          p[this.key++] = {};
          this.setState({ noproduct: p }, this.checkValidation);
        }}
      >
        <AddIcon />
      </Fab>
    );
  }

  renderProduct(key) {
    const item = this.state.noproduct[key] || {};
    const productId = item.productId;
    const productBatches = productId ? this.getBatchesForProduct(productId) : [];
    const hasMultipleBatches = productBatches.length > 1;

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
              if (!item) {
                const updatedProducts = { ...this.state.noproduct };
                if (updatedProducts[key]) {
                  updatedProducts[key] = {
                    ...updatedProducts[key],
                    productId: "",
                    quantity: undefined,
                    measurementUnit: undefined,
                    batchQtys: {},
                  };
                }
                this.setState({ noproduct: updatedProducts }, this.checkValidation);
                return;
              }

              const updatedProducts = { ...this.state.noproduct };
              const existing = updatedProducts[key] || {};
              updatedProducts[key] = {
                ...existing,
                productId: item.value,
                quantity: item.quantity,
                measurementUnit: item.measurementUnit,
                batchQtys: {},
              };

              this.setState(
                {
                  noproduct: updatedProducts,
                  currentsourceDetails: this.state.currentsourceDetails.filter(
                    (x) => x.value !== item.value
                  ),
                },
                this.checkValidation
              );
            },
          })}
        </Grid>
        {!hasMultipleBatches && (
          <Grid item sm={12} md={6}>
            {this.renderTextField({
              fieldname: `returnquantity_${key}`,
              placeholder: "Return Quantity",
              type: "number",
              required: true,
              validation: "nonegative",
              onChange: (value) => {
                const updatedProducts = { ...this.state.noproduct };
                const existing = updatedProducts[key] || {};
                updatedProducts[key] = { ...existing, returnquantity: value };
                this.setState({ noproduct: updatedProducts }, this.checkValidation);
              },
            })}
          </Grid>
        )}
        {hasMultipleBatches && (
          <Grid item sm={12} md={12}>
            <div style={{ fontSize: '12px', color: '#555', marginBottom: '6px', marginTop: '4px' }}>
              Multiple batches found — enter reject quantity per batch:
            </div>
            {productBatches.map((b) => (
              <div key={b.batchId} style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                <div style={{ minWidth: '200px', fontSize: '13px' }}>
                  <strong>#{b.batchId}</strong>
                  {b.lotNumber ? ` · Lot: ${b.lotNumber}` : ''}
                  {b.brand ? ` · ${b.brand}` : ''}
                  <span style={{ color: '#666', marginLeft: '6px' }}>
                    (avail: {b.qtyRemaining})
                  </span>
                </div>
                <input
                  type="number"
                  min="0"
                  max={b.qtyRemaining}
                  step="any"
                  placeholder="Qty"
                  style={{ width: '90px', padding: '6px 8px', border: '1px solid #ccc', borderRadius: '4px', fontSize: '13px' }}
                  onChange={(e) => {
                    const val = parseFloat(e.target.value) || 0;
                    const updatedProducts = { ...this.state.noproduct };
                    const existing = updatedProducts[key] || {};
                    const batchQtys = { ...(existing.batchQtys || {}), [b.batchId]: val };
                    const totalQty = Object.values(batchQtys).reduce((s, v) => s + (v || 0), 0);
                    updatedProducts[key] = { ...existing, batchQtys, returnquantity: totalQty };
                    this.setState({ noproduct: updatedProducts }, this.checkValidation);
                  }}
                />
              </div>
            ))}
          </Grid>
        )}
        <Grid item sm={12} md={12}>
          {this.renderTextArea({
            fieldname: `remarks_${key}`,
            placeholder: "Remarks",
            required: true,
            validation: "maxlength",
            lengthConstraint: 100,
            onChange: (value) => {
              const updatedProducts = { ...this.state.noproduct };
              const existing = updatedProducts[key] || {};
              updatedProducts[key] = {
                ...existing,
                remarks: value,
              };
              this.setState({ noproduct: updatedProducts }, this.checkValidation);
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
              const products = { ...this.state.noproduct };
              const updatedsourceDetails = [...this.state.currentsourceDetails];

              if (products && products[key] && products[key].productId) {
                const originalProduct = this.sourceDetails.filter(
                  (x) => x.value === products[key].productId
                )[0];
                if (originalProduct) {
                  updatedsourceDetails.push(originalProduct);
                }
              }

              delete products[key];

              this.setState(
                {
                  noproduct: products,
                  currentsourceDetails: updatedsourceDetails,
                },
                this.checkValidation
              );
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
            this.submited = false;
            this.setState({ enableSave: false });
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
      const batches = this.getBatchesForProduct(item.productId);
      const hasMultiple = batches.length > 1;
      const overrideBatches = hasMultiple
        ? Object.entries(item.batchQtys || {})
            .filter(([, qty]) => qty > 0)
            .map(([batchId, qty]) => ({ batchId: Number(batchId), qty }))
        : undefined;
      return {
        productId: item.productId,
        quantity: Number(item.returnquantity),
        remarks: item.remarks,
        ...(overrideBatches ? { overrideBatches } : {}),
      };
    });

    const params = {
      productWithQuantities: data,
    };
    const url = apiEndpoints.inwardReject + this.props?.data.inwardId;
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
        maxWidth="md"
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
