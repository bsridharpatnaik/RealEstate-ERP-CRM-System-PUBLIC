import React from "react";
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
import Fab from "@material-ui/core/Fab";
import Grid from "@material-ui/core/Grid";
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
import moment from "moment";

class ReturnProduct extends AddForm {
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
    // batchConsumptions: { [productId]: [{ batchId, brand, lotNumber, expiryDate, qtyConsumed }] }
    batchConsumptions: {},
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

  /** Load batch consumptions for a product in this outward. Sets state.batchConsumptions[productId]. */
  async loadBatchConsumptions(productId) {
    if (!productId) return;
    const outwardId = this.props?.data?.outwardid;
    if (!outwardId) return;
    try {
      const response = await API.GET(apiEndpoints.getOutwardBatchConsumptions(outwardId));
      if (response.success && Array.isArray(response.data)) {
        // Filter to this product
        const forProduct = response.data.filter((c) => c.productId === productId);
        this.setState((prev) => ({
          batchConsumptions: { ...prev.batchConsumptions, [productId]: forProduct },
        }));
      }
    } catch (e) {
      // ignore — batch selection UI just won't show
    }
  }

  /**
   * Returns true if the product used multiple distinct batches in the outward
   * (i.e., batch selection is required for return).
   */
  hasMultipleBatches(productId) {
    const consumptions = this.state.batchConsumptions[productId] || [];
    const distinctBatches = new Set(consumptions.map((c) => c.batch?.batchId));
    return distinctBatches.size > 1;
  }

  checkValidation() {
    let isValid = this.key > 1 ? true : false;
    if (isValid) {
      const val = Object.values(this.state.noproduct);
      if (val.length === 0) {
        isValid = false;
      }
      Object.values(this.state.noproduct).forEach((item) => {
        if (item && item.productId && item.returnquantity && item.returnquantity > 0 && isValid) {
          // If multiple batches, all batch return qty must be > 0
          if (this.hasMultipleBatches(item.productId)) {
            const batchEntries = item.batchReturnQtys || {};
            const consumptions = this.state.batchConsumptions[item.productId] || [];
            // Ensure at least one batch qty is specified
            const hasAny = consumptions.some(
              (c) => batchEntries[c.batch?.batchId] && batchEntries[c.batch?.batchId] > 0
            );
            if (!hasAny) isValid = false;
          }
          isValid = true;
        } else {
          isValid = false;
        }
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

  renderBatchReturnSection(key, item) {
    const productId = item.productId;
    if (!productId) return null;
    const consumptions = this.state.batchConsumptions[productId] || [];
    if (!this.hasMultipleBatches(productId)) return null;

    // Group consumptions by batchId (from nested batch object)
    const batchMap = {};
    consumptions.forEach((c) => {
      const bId = c.batch?.batchId;
      if (!bId) return;
      if (!batchMap[bId]) {
        batchMap[bId] = {
          batchId: bId,
          brand: c.batch?.brand,
          lotNumber: c.batch?.lotNumber,
          expiryDate: c.batch?.expiryDate,
          totalConsumed: 0,
        };
      }
      batchMap[bId].totalConsumed += c.qtyConsumed;
    });

    return (
      <Grid item sm={12} style={{ marginTop: 8 }}>
        <div style={{ fontSize: 12, color: "#666", marginBottom: 4 }}>
          Multiple batches used — specify return qty per batch:
        </div>
        {Object.values(batchMap).map((b) => {
          const label = [
            b.brand,
            b.lotNumber,
            b.expiryDate ? "Exp: " + moment(b.expiryDate).format("DD-MM-YYYY") : null,
            `(available: ${b.totalConsumed})`,
          ]
            .filter(Boolean)
            .join(" | ");
          return (
            <div key={b.batchId} style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
              <span style={{ fontSize: 12, minWidth: 220 }}>{label || `Batch #${b.batchId}`}</span>
              <input
                type="number"
                min="0"
                max={b.totalConsumed}
                step="any"
                placeholder="Return qty"
                style={{
                  padding: "4px 8px",
                  border: "1px solid #ccc",
                  borderRadius: 4,
                  fontSize: 13,
                  width: 100,
                }}
                onChange={(e) => {
                  const p = this.state.noproduct;
                  if (!p[key].batchReturnQtys) p[key].batchReturnQtys = {};
                  p[key].batchReturnQtys[b.batchId] = parseFloat(e.target.value) || 0;
                  this.setState({ noproduct: { ...p } }, () => this.checkValidation());
                }}
              />
            </div>
          );
        })}
      </Grid>
    );
  }

  renderProduct(key) {
    const item = this.state.noproduct[key] || {};
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
            onChange: (e, selectedItem) => {
              const p = this.state.noproduct;
              p[key].productId = selectedItem.value || "";
              p[key].batchReturnQtys = {};
              this.setState({
                currentsourceDetails: this.state.currentsourceDetails.filter(
                  (x) => x.value !== selectedItem.value
                ),
              });
              if (selectedItem) {
                p[key].quantity = selectedItem.quantity;
                p[key].measurementUnit = selectedItem.measurementUnit;
              }
              // Load batch consumptions for this product
              this.loadBatchConsumptions(selectedItem.value);
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
        {this.renderBatchReturnSection(key, item)}
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
                p[key].productId &&
                updatedsourceDetails.push(
                  this.sourceDetails.filter(
                    (x) => x.value === p[key].productId
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
        <span className="filter-title">{messages.common.returnInventory}</span>
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
            if (response && response.success) {
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
      const entry = {
        productId: item.productId,
        quantity: Number(item.returnquantity),
      };
      // Include returnBatches if user specified per-batch qtys (multi-batch scenario)
      if (item.batchReturnQtys && Object.keys(item.batchReturnQtys).length > 0) {
        entry.returnBatches = Object.entries(item.batchReturnQtys)
          .filter(([, qty]) => qty > 0)
          .map(([batchId, qty]) => ({ batchId: Number(batchId), qty }));
      }
      return entry;
    });

    const params = {
      productWithQuantities: data,
    };
    const url =
      apiEndpoints.individualOutwardInventory +
      this.props?.data.outwardid +
      "?type=return";
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

export default withSnackbar(ReturnProduct);
