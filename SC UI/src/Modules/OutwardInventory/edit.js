//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
import { API } from "./../../axios";
import {
  Dialog, DialogTitle, DialogContent, DialogContentText,
  DialogActions, Button as MuiButton,
} from "@material-ui/core";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import DeleteIcon from "@material-ui/icons/Delete";
//style
import "./style.scss";
//misc
import IconButton from "@material-ui/core/IconButton";
import { fetchUnit } from "./../../actions/measurementUnit";
import moment from "moment";
class Edit extends EditForm {
  title = messages.common.outwardInventory;
  updateUrl = apiEndpoints.individualOutwardInventory;
  state = {
    value: 0,
    noproduct: {},
    currentStock: {},
    boqQuantity: {},
    boqViolationDialog: { open: false, violations: [] },
  };
  key = 1;

  componentDidMount() {
    this.search();
    this.updateUrl = this.updateUrl + this.props.id;
    const { dispatch } = this.props;
    dispatch(fetchUnit());
  }
  async search() {
    const response = await API.GET(
      apiEndpoints.individualOutwardInventory + this.props.id
    );
    if (response.success) {
      const data = response.data;
      const p = this.state.noproduct;

      this.formData.purpose = data.purpose;
      this.formData.usageLocationId = data.usageLocation.locationId;
      this.formData.usageAreaId = data.usageArea.usageAreaId;
      this.formData.contractorId = data.contractor.contactId;
      this.formData.warehouseId = data.warehouse.warehouseId;
      this.formData.slipNo = data.slipNo;
      this.formData.additionalInfo = data.additionalInfo;
      this.formData.date = data.date;
      this.formData.fileInformations = data.fileInformations;

      const currentStock = {};
      this.originalQtyMap = {};   // track original qty per product for override preservation
      for (let i = 0; i < data.inwardOutwardList.length; i++) {
        const item = data.inwardOutwardList[i];
        const pid = item.product.productId;
        p[this.key++] = {
          quantity: item.quantity,
          productId: pid,
        };
        currentStock[pid] = item.closingStock;
        this.originalQtyMap[pid] = item.quantity;
        this.getBoqQuantity(pid);
      }
      this.oldStock = currentStock;
      this.setState({
        isLoaded: true,
        noproduct: { ...p },
        currentStock: currentStock,
      });

      // Load batch consumption data so overrides can be preserved when qty is unchanged
      this.batchConsumptionData = {};
      const bcResp = await API.GET(apiEndpoints.getOutwardBatchConsumptions(this.props.id));
      if (bcResp.success) {
        (bcResp.data || []).forEach(c => {
          const pid = c.productId;
          if (!this.batchConsumptionData[pid]) this.batchConsumptionData[pid] = [];
          this.batchConsumptionData[pid].push(c);
        });
      }
    }
  }
  renderProduct(key) {
    const currentProductId = this.state.noproduct?.[key]?.productId;
    const selectedProducts = Object.keys(this.state.noproduct).map(index => this?.state?.noproduct?.[index]?.productId);
    const remainingProducts = (this.props.dropdowns?.product??[]).filter(item => (!selectedProducts.includes(item.id) || currentProductId === item.id));
    return (
      <div className="flex" key={key}>
        {this.renderAutoComplete({
          fieldname: "productId",
          placeholder: messages.common.inventory,
          options: remainingProducts,
          disableClearable: true,
          required: true,
          defaultKey: "productId",
          data: this.state.noproduct[key],
          skipAdd: true,
          disabled: true,
          getOption: (option) => {
            return option["name"];
          },
          onChange: (e, value) => {
            const p = this.state.noproduct;
            p[key].productId = value.id || "";
            if (value) {
              this.getCurrentStock(key);
              this.getBoqQuantity(key);
            }
          },
        })}
        {this.renderTextField({
          fieldname: "measurementUnit",
          placeholder: "Measurement Unit",
          disabled: true,
          value: this.props.units[this.state.noproduct[key].productId],
        })}
        {this.renderTextField({
          fieldname: "quantity",
          placeholder: "Quantity",
          type: "number",
          required: true,
          defaultKey: "quantity",
          data: this.state.noproduct[key],
          skipAdd: true,
          validation: "nonegative",
          onChange: (value) => {
            const p = this.state.noproduct;
            p[key].quantity = value;
            this.getCurrentStock(key);
          },
        })}

        {this.renderTextField({
          fieldname: "ClosingStock",
          placeholder: "Closing Stock",
          type: "number",
          disabled: true,
          data: this.state.noproduct[key],
          value: this.state.currentStock[this.state.noproduct[key].productId],
        })}
        {this.renderTextField({
          fieldname: "boq",
          placeholder: "BOQ Remaining",
          disabled: true,
          data: this.state.noproduct[key],
          value: this.state.boqQuantity[this.state.noproduct[key].productId],
        })}
        <IconButton
          aria-label="back"
          onClick={() => {
            const p = this.state.noproduct;
            delete p[key];
            this.setState({ noproduct: { ...p } });
          }}
          disabled={true}
          className="back-icon"
        >
          <DeleteIcon />
        </IconButton>
      </div>
    );
  }
  async getCurrentStock(index) {
    const warehouseId = this.formData.warehouseId;
    const productId = this.state.noproduct[index].productId;
    if (!productId) {
      return;
    }
    const response = await API.GET(
      apiEndpoints.getCurrentStock +
        "productId=" +
        productId +
        "&warehouseId=" +
        warehouseId
    );
    if (response.success) {
      const currentStock = this.state.currentStock;
      const productId = this.state.noproduct[index].productId;
      currentStock[productId] =
        Number(response.data) -
        Number(this.state.noproduct[index].quantity) +
        (this.oldStock[productId] || 0);
      this.setState({ currentStock: { ...currentStock } });
    }
  }
  async getBoqQuantity(productId) {
    const locationId = this.formData.usageLocationId;
    const finalLocationId = this.formData.usageAreaId;
    if (!productId) {
      return;
    }
    try {
      const boqResponse = await API.GET(
          apiEndpoints.getBoqQuantity +
          "productId=" +
          productId +
          "&locationId=" +
          locationId +
          "&finalLocationId=" +
          finalLocationId
      );
      const boqQuantity = this.state.boqQuantity;
      let value = 0;
      if (boqResponse.success && boqResponse.data != null && boqResponse.data !== "" && String(boqResponse.data).toUpperCase() !== "NA") {
        const num = Number(boqResponse.data);
        value = Number.isFinite(num) ? Math.round(num * 100) / 100 : 0;
      }
      boqQuantity[productId] = value;
      this.setState({ boqQuantity });
    } catch (err) {
      const boqQuantity = this.state.boqQuantity;
      boqQuantity[productId] = 0;
      this.setState({ boqQuantity });
    }
  }
  async update(event) {
    event.preventDefault();
    if (Object.keys(this.state.noproduct).length === 0) {
      this.props.enqueueSnackbar("Add atleast one Product", {
        variant: "error",
      });
      return;
    }
    if (
      !this.formData.fileInformations ||
      this.formData.fileInformations.length === 0
    ) {
      this.props.enqueueSnackbar("Add atleast one file", {
        variant: "error",
      });
      return;
    }
    this.setState({ isUpdating: true });

    const params = this.formData;

    // #7: Preserve batch override data when qty is unchanged for a product
    params.productWithQuantities = Object.values(this.state.noproduct).map(p => {
      const result = { ...p };
      const originalQty = (this.originalQtyMap || {})[p.productId];
      const consumptions = (this.batchConsumptionData || {})[p.productId] || [];
      const hasOverride = consumptions.some(c => c.fifoOverridden);

      if (hasOverride && originalQty != null && Math.abs(p.quantity - originalQty) <= 0.001) {
        // Qty unchanged — re-send the same override batches so the re-created consumptions match
        result.overrideBatches = consumptions.map(c => ({
          batchId: c.batch ? c.batch.batchId : c.batchId,
          qty: c.qtyConsumed,
        }));
        const comment = consumptions.find(c => c.overrideComment)?.overrideComment;
        if (comment) result.overrideComment = comment;
      }
      // If qty changed: drop override — backend re-runs pure FIFO with new qty
      return result;
    });

    const response = await API.PUT(this.updateUrl, params);
    this.setState({ isUpdating: false });

    if (response.success) {
      this.showToaster(response);
    } else {
      const msg = response.errorMessage || '';
      if (msg.startsWith('BOQ_LIMIT_EXCEEDED:')) {
        const violations = msg.replace('BOQ_LIMIT_EXCEEDED:', '').split('|').filter(Boolean);
        this.setState({ boqViolationDialog: { open: true, violations } });
      } else {
        this.showToaster(response);
      }
    }
  }
  async updateStockInfo(id) {
    const params = {};
    params.warehouseId = id;
    params.productIds = Object.values(this.state.noproduct).map(
      (p) => p.productId
    );
    const response = await API.POST(apiEndpoints.getMultiStock, params);
    if (response.success) {
      const data = response.data;
      const currentStock = {};
      const products = Object.values(this.state.noproduct);
      data.forEach((element) => {
        const productId = element.productId;
        let product = products.filter((p) => p.productId === productId);
        product = product[0];
        currentStock[productId] = element.stock - Number(product.quantity);
      });
      this.setState({ currentStock: currentStock });
    }
  }
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
            <div className="flex width50">
              {this.renderDate({
                defaultKey: "date",
                fieldname: "date",
                //disabled: !this.isAdmin,
                disabled: true,
                maxDate: moment(),
                //minDate: moment(this.formData.date).add(-3, 'd'),
              })}

              {this.renderAutoComplete({
                fieldname: "warehouseId",
                placeholder: "Warehouse",
                options: this.props.dropdowns.warehouse,
                disableClearable: true,
                required: true,
                skipAdd: true,
                disabled: true,
                getOption: (option) => {
                  return option["name"];
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData.warehouseId = value.id;
                    this.updateStockInfo(value.id);
                  }
                },
              })}
            </div>
            <div className="flex width50">
              {this.renderAutoComplete({
                fieldname: "contractorId",
                placeholder: "Contractor",
                options: this.props.dropdowns.contractor,
                disableClearable: true,
                required: true,
                disabled: true,
                getOption: (option) => {
                  return option["name"];
                },
              })}
              {this.renderAutoComplete({
                fieldname: "usageLocationId",
                placeholder: messages.common.location,
                options: this.props.dropdowns.usagelocation,
                disableClearable: true,
                required: true,
                disabled: !this.isAdmin,
                getOption: (option) => {
                  return option["name"];
                },
              })}
              {this.renderAutoComplete({
                fieldname: "usageAreaId",
                placeholder: messages.common.finalLocation,
                options: this.props.dropdowns.usageArea,
                disableClearable: true,
                required: true,
                disabled: !this.isAdmin,
                getOption: (option) => {
                  return option["name"];
                },
              })}
            </div>
            <div className="width50 flex">
              {this.renderTextField({
                fieldname: "slipNo",
                placeholder: "Slip no",
              })}
              {this.renderTextField({
                fieldname: "purpose",
                placeholder: "Purpose",
              })}
            </div>
            <div class="flex">
              {this.renderTextArea({
                defaultKey: "additionalInfo",
                fieldname: "additionalInfo",
                placeholder: "Additional Comments",
              })}
            </div>
            {this.renderFileArea()}

            <div className="products-list">
              <div className="product-heading">
                <div>Products</div>
              </div>
              {Object.keys(this.state.noproduct).map((key) =>
                this.renderProduct(key)
              )}
              {this.renderProductAddButton(true)}
            </div>

            {this.renderFooter()}
          </form>
        )}

        {/* BOQ violation dialog */}
        <Dialog
          open={this.state.boqViolationDialog.open}
          onClose={() => this.setState({ boqViolationDialog: { open: false, violations: [] } })}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle style={{ color: '#c62828' }}>⚠ BOQ Limit Exceeded — Save Blocked</DialogTitle>
          <DialogContent>
            <DialogContentText style={{ marginBottom: 12 }}>
              The following products exceed their BOQ limit (including wastage allowance).
            </DialogContentText>
            {this.state.boqViolationDialog.violations.map((v, i) => {
              const parts = v.split(':');
              const product = parts[0] || v;
              const detail  = parts.slice(1).join(':').trim();
              return (
                <div key={i} style={{
                  background: '#fff8f8', border: '1px solid #ffcdd2', borderRadius: 6,
                  padding: '8px 12px', marginBottom: 8,
                  borderBottom: i < this.state.boqViolationDialog.violations.length - 1 ? '1px solid #ffcdd2' : 'none',
                }}>
                  <strong style={{ color: '#c62828' }}>{product}</strong>
                  {detail && <div style={{ fontSize: 12, color: '#555', marginTop: 2 }}>{detail}</div>}
                </div>
              );
            })}
          </DialogContent>
          <DialogActions>
            <MuiButton
              variant="contained" color="primary"
              onClick={() => this.setState({ boqViolationDialog: { open: false, violations: [] } })}
            >
              OK
            </MuiButton>
          </DialogActions>
        </Dialog>
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
  withSnackbar(Edit)
);
