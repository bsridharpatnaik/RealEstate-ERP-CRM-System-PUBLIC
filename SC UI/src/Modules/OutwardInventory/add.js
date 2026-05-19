//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
import { API } from "./../../axios";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import DeleteIcon from "@material-ui/icons/Delete";
import Fab from "@material-ui/core/Fab";
import AddIcon from "@material-ui/icons/Add";
import {
  Dialog, DialogTitle, DialogContent, DialogContentText,
  DialogActions, Button as MuiButton,
} from "@material-ui/core";

//style
import "./style.scss";
//misc
import IconButton from "@material-ui/core/IconButton";
import { fetchUnit } from "./../../actions/measurementUnit";
import moment from "moment";
import { getRoleEditConstraintDays } from "./../../helper";

class Add extends AddForm {
  title = messages.common.outwardInventory;

  addurl = apiEndpoints.createOutwardInventory;
  state = {
    value: 0,
    noproduct: {},
    currentStock: {},
    boqQuantity: {},
    allProductsStockMap: {},
    selectedWarehouseId: null,
    boqViolationDialog: { open: false, violations: [] },
    availableBatches: {}, // productId → [batch]
    fifoConfirmModal: {
      open: false,
      productKey: null,
      selectedBatchId: null,
      fifoBatch: null,
      input: '',
      error: '',
    },
  };
  key = 1;
  componentDidMount() {
    const { dispatch } = this.props;
    dispatch(fetchUnit());
    this.fetchAllProductsStock();
  }

  async fetchAllProductsStock() {
    const response = await API.GET(apiEndpoints.getAllProductsStockSummary);
    if (response.success && Array.isArray(response.data)) {
      const stockMap = {};
      response.data.forEach(item => { stockMap[item.productId] = item; });
      this.setState({ allProductsStockMap: stockMap });
    }
  }
  
  renderProductAddButton() {
    return (
      <Fab
        className="add-product"
        color="primary"
        aria-label="add"
        disabled={Object.keys(this.state.noproduct).length === 50}
        onClick={() => {
          if (!this.formData.warehouseId) {
            this.props.enqueueSnackbar("Select Warehouse first", {
              variant: "error",
            });
            return;
          }
          if (!this.formData.usageLocationId) {
            this.props.enqueueSnackbar("Select Structure first", {
              variant: "error",
            });
            return;
          }
          if (!this.formData.usageAreaId) {
            this.props.enqueueSnackbar("Select Work Area first", {
              variant: "error",
            });
            return;
          }
          const p = this.state.noproduct;
          p[this.key++] = {};
          this.setState({ noproduct: { ...p } });
        }}
      >
        <AddIcon />
      </Fab>
    );
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
          getOption: (option) => {
            const { allProductsStockMap, selectedWarehouseId } = this.state;
            const stockInfo = allProductsStockMap[option.id];
            if (stockInfo && selectedWarehouseId) {
              const entry = stockInfo.warehouseStocks.find(
                ws => Number(ws.warehouseId) === Number(selectedWarehouseId)
              );
              const stock = entry ? Number(entry.stock.toFixed(2)) : 0;
              return `${option.name} (${stock} ${stockInfo.measurementUnit || ''})`;
            }
            return option.name;
          },
          onChange: (e, value) => {
            const p = this.state.noproduct;
            p[key].productId = value.id || "";
            if (value) {
              const { allProductsStockMap, selectedWarehouseId } = this.state;
              const stockInfo = allProductsStockMap[value.id];
              if (selectedWarehouseId) {
                if (!stockInfo || stockInfo.warehouseStocks.length === 0) {
                  this.props.enqueueSnackbar(
                    `${value.name} is out of stock in all warehouses`,
                    { variant: "warning" }
                  );
                } else {
                  const entry = stockInfo.warehouseStocks.find(
                    ws => Number(ws.warehouseId) === Number(selectedWarehouseId)
                  );
                  const stockInWarehouse = entry ? entry.stock : 0;
                  if (stockInWarehouse <= 0) {
                    const otherWarehouses = stockInfo.warehouseStocks.filter(ws => ws.stock > 0);
                    const selectedWarehouse = (this.props.dropdowns.warehouse || []).find(
                      w => Number(w.id) === Number(selectedWarehouseId)
                    );
                    const warehouseName = selectedWarehouse ? selectedWarehouse.name : selectedWarehouseId;
                    if (otherWarehouses.length > 0) {
                      const otherNames = otherWarehouses.map(ws => ws.warehouseName).join(', ');
                      this.props.enqueueSnackbar(
                        `${value.name} does not have stock in warehouse ${warehouseName}. It has stock in warehouse(s) - ${otherNames}`,
                        { variant: "warning" }
                      );
                    } else {
                      this.props.enqueueSnackbar(
                        `${value.name} is out of stock in all warehouses`,
                        { variant: "warning" }
                      );
                    }
                  }
                }
              }
              this.getCurrentStock(key);
              this.getBoqQuantity(key);
              this.fetchBatchesForProduct(value.id, this.formData.warehouseId);
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
          skipAdd: true,
          validation: "nonegative",
          onChange: (value) => {
            const p = this.state.noproduct;
            p[key].quantity = value;
            const productId = this.state.noproduct[key].productId;
            const { allProductsStockMap, selectedWarehouseId } = this.state;
            const stockInfo = allProductsStockMap[productId];
            if (stockInfo && selectedWarehouseId) {
              const entry = stockInfo.warehouseStocks.find(
                ws => Number(ws.warehouseId) === Number(selectedWarehouseId)
              );
              const stockInWarehouse = entry ? entry.stock : 0;
              if (stockInWarehouse > 0 && Number(value) > stockInWarehouse) {
                this.props.enqueueSnackbar(
                  `Quantity cannot exceed available stock of ${stockInWarehouse} ${stockInfo.measurementUnit || ''}`,
                  { variant: "error" }
                );
              }
            }
            this.getCurrentStock(key);
            const boqRemaining = this.state.boqQuantity[productId];
            if (boqRemaining !== undefined && boqRemaining !== null && Number(value) > Number(boqRemaining)) {
              const inWastage = Number(boqRemaining) < 0;
              this.props.enqueueSnackbar(
                inWastage
                  ? `BOQ Warning: Already in wastage buffer. Base BOQ fully consumed (${Math.abs(boqRemaining)} over base BOQ).`
                  : `BOQ Warning: Quantity exceeds base BOQ remaining (${boqRemaining} remaining). Wastage allowance may still permit save.`,
                { variant: "warning" }
              );
            }
          },
        })}

        {this.renderTextField({
          fieldname: "ClosingStock",
          placeholder: "Closing Stock",
          type: "number",
          disabled: true,
          value: this.state.currentStock[this.state.noproduct[key].productId],
        })}
        {this.renderTextField({
          fieldname: "boq",
          placeholder: "BOQ Remaining",
          disabled: true,
          value: this.state.boqQuantity[this.state.noproduct[key].productId],
        })}

        {/* Override FIFO batch selection */}
        <div style={{ display: 'flex', flexDirection: 'column', minWidth: '200px' }}>
          <label style={{ fontSize: '12px', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <input
              type="checkbox"
              checked={!!this.state.noproduct[key].overrideFifo}
              onChange={(e) => {
                const p = this.state.noproduct;
                p[key].overrideFifo = e.target.checked;
                if (!e.target.checked) {
                  p[key].overrideBatchId = null;
                  p[key].overrideComment = null;
                }
                this.setState({ noproduct: { ...p } });
              }}
            />
            Override FIFO batch
          </label>
          {this.state.noproduct[key].overrideFifo && (() => {
            const productId = this.state.noproduct[key].productId;
            const batches = this.state.availableBatches[productId] || [];
            return (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <select
                  style={{ padding: '4px', fontSize: '12px', borderRadius: '4px', border: '1px solid #ccc' }}
                  value={this.state.noproduct[key].overrideBatchId || ''}
                  onChange={(e) => {
                    const selectedId = e.target.value ? parseInt(e.target.value) : null;
                    if (!selectedId) {
                      const p = this.state.noproduct;
                      p[key].overrideBatchId = null;
                      this.setState({ noproduct: { ...p } });
                      return;
                    }
                    const fifoBatch = batches[0];
                    const isNonFifo = fifoBatch && fifoBatch.batchId !== selectedId;
                    if (isNonFifo) {
                      // Non-FIFO selection — require expiry date confirmation
                      this.setState({
                        fifoConfirmModal: {
                          open: true,
                          productKey: key,
                          selectedBatchId: selectedId,
                          fifoBatch,
                          input: '',
                          error: '',
                        },
                      });
                    } else {
                      // FIFO-correct selection — set directly, no confirmation needed
                      const p = this.state.noproduct;
                      p[key].overrideBatchId = selectedId;
                      this.setState({ noproduct: { ...p } });
                    }
                  }}
                >
                  <option value="">Select batch...</option>
                  {batches.map(b => (
                    <option key={b.batchId} value={b.batchId}>
                      {b.brand || 'No brand'} | Recv: {b.receivedDate ? new Date(b.receivedDate).toLocaleDateString('en-GB') : '-'} | Qty: {b.qtyRemaining}
                      {b.expiryDate ? ` | Exp: ${new Date(b.expiryDate).toLocaleDateString('en-GB')}` : ''}
                    </option>
                  ))}
                </select>
                <input
                  type="text"
                  placeholder="Reason for override (required) *"
                  maxLength={500}
                  style={{ padding: '4px', fontSize: '12px', borderRadius: '4px', border: '1px solid #ccc' }}
                  value={this.state.noproduct[key].overrideComment || ''}
                  onChange={(e) => {
                    const p = this.state.noproduct;
                    p[key].overrideComment = e.target.value;
                    this.setState({ noproduct: { ...p } });
                  }}
                />
              </div>
            );
          })()}
        </div>

        <IconButton
          aria-label="back"
          onClick={() => {
            const p = this.state.noproduct;
            delete p[key];
            this.setState({ noproduct: { ...p } });
          }}
          className="back-icon"
        >
          <DeleteIcon />
        </IconButton>
      </div>
    );
  }
  async fetchBatchesForProduct(productId, warehouseId) {
    if (!productId || !warehouseId) return;
    const response = await API.GET(apiEndpoints.getBatchesForProduct(productId, warehouseId));
    if (response.success && Array.isArray(response.data)) {
      const batches = this.state.availableBatches;
      batches[productId] = response.data.filter(b => b.qtyRemaining > 0);
      this.setState({ availableBatches: { ...batches } });
    }
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
        Number(response.data) - Number(this.state.noproduct[index].quantity);
      this.setState({ currentStock: { ...currentStock } });
    }
  }
  async getBoqQuantity(index) {
    const productId = this.state.noproduct[index].productId;
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
      const productIdKey = this.state.noproduct[index].productId;
      let value = 0;
      if (boqResponse.success && boqResponse.data != null && boqResponse.data !== "" && String(boqResponse.data).toUpperCase() !== "NA") {
        const num = Number(boqResponse.data);
        value = Number.isFinite(num) ? Math.round(num * 100) / 100 : 0;
      }
      boqQuantity[productIdKey] = value;
      this.setState({ boqQuantity });
    } catch (err) {
      const boqQuantity = this.state.boqQuantity;
      boqQuantity[this.state.noproduct[index].productId] = 0;
      this.setState({ boqQuantity });
    }
  }
  async add(event) {
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
    // Validate override: if overrideFifo is checked, batch + comment are required
    for (const product of Object.values(this.state.noproduct)) {
      if (product.overrideFifo) {
        if (!product.overrideBatchId) {
          this.props.enqueueSnackbar("Please select a batch for FIFO override.", { variant: "error" });
          return;
        }
        if (!product.overrideComment || !product.overrideComment.trim()) {
          this.props.enqueueSnackbar("A reason is required when overriding FIFO batch selection.", { variant: "error" });
          return;
        }
      }
    }

    const params = this.formData;
    this.setState({ isAdding: true });

    params.productWithQuantities = Object.values(this.state.noproduct).map(p => ({
      productId: p.productId,
      quantity: p.quantity,
      overrideBatchId: p.overrideFifo ? (p.overrideBatchId || null) : null,
      overrideComment: p.overrideFifo ? (p.overrideComment || null) : null,
    }));
    const response = await API.POST(this.addurl, params);
    this.setState({ isAdding: false });

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
    const days = getRoleEditConstraintDays();

    return (
      <div className="list-section add">
        {this.renderHeading()}

        <form onSubmit={(e) => this.add(e)}>
          <div className="flex width50">
            {this.renderDate({
              fieldname: "date",
              label: messages.fields.date,
              maxDate: moment(),
              minDate: moment().add(-days, "d"),
            })}

            {this.renderAutoComplete({
              fieldname: "warehouseId",
              placeholder: "Warehouse",
              options: this.props.dropdowns.warehouse,
              disableClearable: true,
              required: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.warehouseId = value.id;
                  this.setState({ selectedWarehouseId: value.id });
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
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.contractorId = value.id;
                }
              },
            })}
            {this.renderAutoComplete({
              fieldname: "usageLocationId",
              placeholder: messages.common.location,
              options: this.props.dropdowns.usagelocation,
              disableClearable: true,
              required: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.usageLocationId = value.id;
                }
              },
            })}
            {this.renderAutoComplete({
              fieldname: "usageAreaId",
              placeholder: messages.common.finalLocation,
              options: this.props.dropdowns.usageArea,
              disableClearable: true,
              required: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.usageAreaId = value.id;
                }
              },
            })}
          </div>
          <div className="width50 flex">
            {this.renderTextField({
              fieldname: "slipNo",
              placeholder: "Slip No",
            })}
            {this.renderTextField({
              fieldname: "purpose",
              placeholder: "Purpose",
            })}
          </div>
          <div class="flex">
            {this.renderTextArea({
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
            {this.renderProductAddButton()}
          </div>

          {this.renderFooter()}
        </form>

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
              Reduce the quantities and try again.
            </DialogContentText>
            <div style={{ border: '1px solid #ffcdd2', borderRadius: 6, background: '#fff8f8' }}>
              {this.state.boqViolationDialog.violations.map((v, i) => {
                const parts = v.split(':');
                const productName = parts[0]?.trim();
                const detail = parts.slice(1).join(':').trim();
                return (
                  <div
                    key={i}
                    style={{
                      padding: '10px 14px',
                      borderBottom: i < this.state.boqViolationDialog.violations.length - 1 ? '1px solid #ffcdd2' : 'none',
                    }}
                  >
                    <div style={{ fontWeight: 600, color: '#c62828', fontSize: 13 }}>{productName}</div>
                    <div style={{ fontSize: 12, color: '#555', marginTop: 2 }}>{detail}</div>
                  </div>
                );
              })}
            </div>
          </DialogContent>
          <DialogActions>
            <MuiButton
              onClick={() => this.setState({ boqViolationDialog: { open: false, violations: [] } })}
              color="secondary"
              variant="contained"
            >
              Close &amp; Correct
            </MuiButton>
          </DialogActions>
        </Dialog>

        {/* FIFO override confirmation modal */}
        {this.renderFifoConfirmModal()}

      </div>
    );
  }

  renderFifoConfirmModal() {
    const { fifoConfirmModal } = this.state;
    if (!fifoConfirmModal.open) return null;

    const { fifoBatch, input, error } = fifoConfirmModal;

    // Determine what the user must enter and how to display it
    const useExpiry = !!(fifoBatch && fifoBatch.expiryDate);
    const confirmDate = useExpiry
      ? new Date(fifoBatch.expiryDate).toLocaleDateString('en-GB')   // DD/MM/YYYY
      : new Date(fifoBatch.receivedDate).toLocaleDateString('en-GB');
    const confirmLabel = useExpiry ? 'expiry date' : 'received date';
    const confirmPlaceholder = 'DD/MM/YYYY';

    const handleConfirm = () => {
      const trimmed = input.trim().replace(/-/g, '/');
      if (trimmed !== confirmDate) {
        this.setState({
          fifoConfirmModal: {
            ...fifoConfirmModal,
            error: `Date does not match. Expected: ${confirmDate}`,
          },
        });
        return;
      }
      // Confirmed — apply the override
      const p = this.state.noproduct;
      p[fifoConfirmModal.productKey].overrideBatchId = fifoConfirmModal.selectedBatchId;
      this.setState({
        noproduct: { ...p },
        fifoConfirmModal: { open: false, productKey: null, selectedBatchId: null, fifoBatch: null, input: '', error: '' },
      });
    };

    const handleCancel = () => {
      this.setState({
        fifoConfirmModal: { open: false, productKey: null, selectedBatchId: null, fifoBatch: null, input: '', error: '' },
      });
    };

    return (
      <Dialog open maxWidth="xs" fullWidth>
        <DialogTitle style={{ background: '#fff3e0', color: '#e65100', fontSize: 16 }}>
          ⚠ FIFO Override — Confirmation Required
        </DialogTitle>
        <DialogContent style={{ paddingTop: 16 }}>
          <div style={{ marginBottom: 12, fontSize: 13, color: '#333' }}>
            You are skipping the oldest available batch:
          </div>
          <div style={{
            background: '#fff8e1', border: '1px solid #ffe082', borderRadius: 6,
            padding: '10px 14px', marginBottom: 16, fontSize: 13,
          }}>
            <div><strong>Brand:</strong> {fifoBatch.brand || '—'}</div>
            <div><strong>Received:</strong> {fifoBatch.receivedDate ? new Date(fifoBatch.receivedDate).toLocaleDateString('en-GB') : '—'}</div>
            {fifoBatch.expiryDate && (
              <div><strong>Expiry:</strong> {new Date(fifoBatch.expiryDate).toLocaleDateString('en-GB')}</div>
            )}
            <div><strong>Qty remaining:</strong> {fifoBatch.qtyRemaining}</div>
          </div>
          <div style={{ fontSize: 13, marginBottom: 8, color: '#555' }}>
            To confirm, enter the <strong>{confirmLabel}</strong> of the skipped batch ({confirmPlaceholder}):
          </div>
          <input
            type="text"
            autoFocus
            placeholder={confirmPlaceholder}
            value={input}
            maxLength={10}
            style={{
              width: '100%', padding: '8px 10px', fontSize: 14,
              border: error ? '1px solid #c62828' : '1px solid #ccc',
              borderRadius: 4, boxSizing: 'border-box',
              letterSpacing: 2,
            }}
            onChange={(e) =>
              this.setState({ fifoConfirmModal: { ...fifoConfirmModal, input: e.target.value, error: '' } })
            }
            onKeyDown={(e) => { if (e.key === 'Enter') handleConfirm(); }}
          />
          {error && (
            <div style={{ color: '#c62828', fontSize: 12, marginTop: 6 }}>{error}</div>
          )}
        </DialogContent>
        <DialogActions>
          <MuiButton onClick={handleCancel} style={{ color: '#757575' }}>
            Cancel
          </MuiButton>
          <MuiButton
            onClick={handleConfirm}
            variant="contained"
            style={{ background: '#e65100', color: '#fff' }}
          >
            Confirm Override
          </MuiButton>
        </DialogActions>
      </Dialog>
    );
  }
}
const mapStateToProps = (state) => {
  return {
    units: state.units.units,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Add)
);
