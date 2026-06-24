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
    selectedStructureTypeId: "ALL",
    filteredStructures: [],
    // Batch re-allocation modal — shown when an override outward's qty is changed
    batchReassignModal: {
      open: false,
      productKey: null,
      productId: null,
      productName: '',
      newQty: 0,
      entries: [],          // [{batchId, label, consumed, reassignQty:''}]
      overrideComment: '',
    },
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
      this.formData.slipNo = data.slipNo;
      this.formData.requestedBy = data.requestedBy;
      this.formData.issuedBy = data.issuedBy;
      this.formData.additionalInfo = data.additionalInfo;
      this.formData.date = data.date;
      this.formData.fileInformations = data.fileInformations;

      // Derive structure type from the selected location
      const allWithType = this.props.dropdowns?.usagelocationWithType || [];
      const matchedLocation = allWithType.find(
        (l) => l.id === data.usageLocation.locationId
      );
      const structureTypeId = matchedLocation && matchedLocation.typeId ? matchedLocation.typeId : "ALL";
      const filteredStructures = structureTypeId === "ALL"
        ? allWithType
        : allWithType.filter((l) => l.typeId === structureTypeId);
      this.formData.structureTypeId = structureTypeId;

      const currentStock = {};
      // Keyed by (productId, warehouseId) — the same product can appear on two lines if
      // it was originally outwarded from two different warehouses in this transaction.
      this.originalQtyMap = {};   // track original qty per line for override preservation
      for (let i = 0; i < data.inwardOutwardList.length; i++) {
        const item = data.inwardOutwardList[i];
        const pid = item.product.productId;
        const whId = item.warehouse.warehouseId;
        const rowKey = this.key++;
        p[rowKey] = {
          quantity: item.quantity,
          productId: pid,
          warehouseId: whId,
          warehouseName: item.warehouse.warehouseName,
        };
        currentStock[rowKey] = item.closingStock;
        this.originalQtyMap[this.lineKey(pid, whId)] = item.quantity;
        this.getBoqQuantity(pid);
      }
      this.oldStock = currentStock;
      this.setState({
        isLoaded: true,
        noproduct: { ...p },
        currentStock: currentStock,
        selectedStructureTypeId: structureTypeId,
        filteredStructures: filteredStructures,
      });

      // Load batch consumption data so overrides can be preserved when qty is unchanged.
      // Keyed by (productId, warehouseId) for the same reason as originalQtyMap above.
      this.batchConsumptionData = {};
      const bcResp = await API.GET(apiEndpoints.getOutwardBatchConsumptions(this.props.id));
      if (bcResp.success) {
        (bcResp.data || []).forEach(c => {
          const key = this.lineKey(c.productId, c.warehouseId);
          if (!this.batchConsumptionData[key]) this.batchConsumptionData[key] = [];
          this.batchConsumptionData[key].push(c);
        });
        // Force re-render so hasOverride is evaluated with loaded data immediately,
        // preventing race condition where quantity field appears enabled then snaps to disabled on first keystroke.
        this.setState({});
      }
    }
  }

  lineKey(productId, warehouseId) {
    return `${productId}_${warehouseId}`;
  }
  renderProduct(key) {
    // Product list isn't editable here (field is disabled below), but the same product
    // can legitimately appear on two rows if the original outward drew it from two
    // different warehouses — so no cross-row exclusion is needed.
    const remainingProducts = this.props.dropdowns?.product ?? [];
    const productId = this.state.noproduct[key].productId;
    const warehouseName = this.state.noproduct[key].warehouseName;
    const unit = this.props.units[productId] || '—';
    // Stock keyed per row (not per product) — the same product can be on two rows
    // with two different warehouses and two different stock levels.
    const closingStock = productId ? (this.state.currentStock[key] ?? '—') : '—';
    const boqRemaining = productId ? (this.state.boqQuantity[productId] ?? '—') : '—';

    return (
      <div key={key} style={{
        border: '1px solid #dce3ec', borderRadius: '8px', marginBottom: '12px',
        overflow: 'hidden', backgroundColor: '#fff',
        boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
      }}>
        {/* Card header */}
        <div style={{
          background: '#f5f7fa', borderBottom: '1px solid #dce3ec',
          padding: '4px 8px 4px 14px',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          minHeight: '32px',
        }}>
          <span style={{ fontSize: '11px', fontWeight: 600, color: '#888', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Product
          </span>
          {/* Delete disabled in edit mode */}
          <IconButton
            size="small"
            disabled
            style={{ color: '#ccc' }}
            title="Products cannot be removed in edit mode"
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </div>

        <div style={{ padding: '10px 12px 4px' }}>
          {/* Row 1: Product (disabled) + Quantity */}
          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
            <div style={{ flex: 3 }}>
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
                getOption: (option) => option["name"],
                onChange: (e, value) => {
                  const p = this.state.noproduct;
                  p[key].productId = value.id || "";
                  if (value) {
                    this.getCurrentStock(key);
                    this.getBoqQuantity(key);
                  }
                },
              })}
            </div>
            <div style={{ flex: 1 }}>
              {this.renderTextField({
                fieldname: "quantity",
                placeholder: "Quantity",
                type: "number",
                required: true,
                defaultKey: "quantity",
                data: this.state.noproduct[key],
                skipAdd: true,
                validation: "nonegative",
                disabled: false,
                onChange: (value) => {
                  const p = this.state.noproduct;
                  p[key].quantity = value;
                  this.getCurrentStock(key);
                },
              })}
            </div>
          </div>

          {/* Row 2: read-only info strip */}
          <div style={{
            display: 'flex', gap: 24, padding: '5px 10px',
            background: '#f5f7fa', borderRadius: 4, fontSize: 12,
            color: '#555', marginBottom: 6, marginTop: 2,
          }}>
            <span><span style={{ color: '#999' }}>Warehouse:</span> <strong>{warehouseName || '—'}</strong></span>
            <span><span style={{ color: '#999' }}>Unit:</span> <strong>{unit}</strong></span>
            <span><span style={{ color: '#999' }}>Closing Stock:</span> <strong>{closingStock}</strong></span>
            <span><span style={{ color: '#999' }}>BOQ Remaining:</span> <strong>{boqRemaining}</strong></span>
          </div>
        </div>
      </div>
    );
  }
  async getCurrentStock(index) {
    const warehouseId = this.state.noproduct[index].warehouseId;
    const productId = this.state.noproduct[index].productId;
    if (!productId || !warehouseId) {
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
      // Keyed per row, not per product — see renderProduct() for why.
      const currentStock = this.state.currentStock;
      currentStock[index] =
        Number(response.data) -
        Number(this.state.noproduct[index].quantity) +
        (this.oldStock[index] || 0);
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

    // Batch override handling for edit — keyed by (productId, warehouseId) since the
    // same product can be on two lines with two different warehouses.
    params.productWithQuantities = Object.values(this.state.noproduct).map(p => {
      const result = { ...p };
      const lineKey = this.lineKey(p.productId, p.warehouseId);
      const originalQty = (this.originalQtyMap || {})[lineKey];
      const consumptions = (this.batchConsumptionData || {})[lineKey] || [];
      const overriddenConsumptions = consumptions.filter(c => c.fifoOverridden === true);
      const hasOverride = overriddenConsumptions.length > 0;
      const qtyChanged = originalQty != null && Math.abs(p.quantity - originalQty) > 0.001;

      if (!hasOverride) {
        // No override — FIFO re-applies automatically. Safe: user never chose specific batches.
        return result;
      }

      if (!qtyChanged) {
        // Qty unchanged — re-send same override batches to preserve them.
        result.overrideBatches = consumptions.map(c => ({
          batchId: c.batch ? c.batch.batchId : c.batchId,
          qty: c.qtyConsumed,
        }));
        result.overrideComment = consumptions.find(c => c.overrideComment)?.overrideComment || 'Override preserved';
        return result;
      }

      // Qty changed with override.
      const isSingleBatchOverride = consumptions.length === 1 && overriddenConsumptions.length === 1;
      if (isSingleBatchOverride) {
        // Single batch — auto-adjust: same batch, new qty. Unambiguous.
        const c = overriddenConsumptions[0];
        const batchId = c.batch ? c.batch.batchId : c.batchId;
        result.overrideBatches = [{ batchId, qty: parseFloat(p.quantity) }];
        result.overrideComment = c.overrideComment || 'Qty adjusted';
        return result;
      }

      // Multi-batch override + qty changed: user explicitly chose batches — NEVER silently reassign.
      // If user has already confirmed re-allocation via the modal, overrideBatches is set on p.
      // If not, we need to open the modal — handled below before API call.
      return result;
    });

    // Check if any product with multi-batch override + qty change still needs re-allocation
    const needsRealloc = Object.keys(this.state.noproduct).find(key => {
      const p = this.state.noproduct[key];
      const lineKey = this.lineKey(p.productId, p.warehouseId);
      const originalQty = (this.originalQtyMap || {})[lineKey];
      const consumptions = (this.batchConsumptionData || {})[lineKey] || [];
      const overriddenConsumptions = consumptions.filter(c => c.fifoOverridden === true);
      const hasMultiOverride = overriddenConsumptions.length > 1 || (consumptions.length > 1 && overriddenConsumptions.length > 0);
      const qtyChanged = originalQty != null && Math.abs(p.quantity - originalQty) > 0.001;
      const alreadySet = p.overrideBatches && p.overrideBatches.length > 0;
      return hasMultiOverride && qtyChanged && !alreadySet;
    });

    if (needsRealloc) {
      // Open re-allocation modal for this product before proceeding with save
      this.openBatchReassignModal(needsRealloc);
      this.setState({ isUpdating: false });
      return;
    }

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
  openBatchReassignModal(productKey) {
    const p = this.state.noproduct[productKey];
    const consumptions = (this.batchConsumptionData || {})[this.lineKey(p.productId, p.warehouseId)] || [];
    const overriddenConsumptions = consumptions.filter(c => c.fifoOverridden === true);
    const productEntry = (this.props.dropdowns?.product || []).find(pr => pr.id === p.productId);
    const productName = (productEntry?.name || `Product ${p.productId}`) + (p.warehouseName ? ` (${p.warehouseName})` : '');
    const entries = consumptions.map(c => {
      const batch = c.batch || {};
      const parts = [batch.brand, batch.lotNumber, batch.expiryDate].filter(Boolean);
      const label = parts.length > 0 ? parts.join(' · ') : `Batch #${batch.batchId || c.batchId}`;
      return {
        batchId: batch.batchId || c.batchId,
        label,
        consumed: c.qtyConsumed,
        reassignQty: '',
      };
    });
    const existingComment = overriddenConsumptions.find(c => c.overrideComment)?.overrideComment || '';
    this.setState({
      batchReassignModal: {
        open: true,
        productKey,
        productId: p.productId,
        productName,
        newQty: parseFloat(p.quantity),
        entries,
        overrideComment: existingComment,
      },
    });
  }

  confirmBatchReassign() {
    const { productKey, newQty, entries, overrideComment } = this.state.batchReassignModal;
    const total = entries.reduce((s, e) => s + (parseFloat(e.reassignQty) || 0), 0);
    if (Math.abs(total - newQty) > 0.001) {
      this.props.enqueueSnackbar(
        `Total allocated (${total}) must equal new quantity (${newQty})`,
        { variant: 'error' }
      );
      return;
    }
    if (!overrideComment.trim()) {
      this.props.enqueueSnackbar('Override comment is required', { variant: 'error' });
      return;
    }
    const overrideBatches = entries
      .filter(e => parseFloat(e.reassignQty) > 0)
      .map(e => ({ batchId: e.batchId, qty: parseFloat(e.reassignQty) }));
    const noproduct = { ...this.state.noproduct };
    noproduct[productKey] = { ...noproduct[productKey], overrideBatches, overrideComment };
    this.setState({
      noproduct,
      batchReassignModal: {
        open: false, productKey: null, productId: null,
        productName: '', newQty: 0, entries: [], overrideComment: '',
      },
    }, () => {
      this.update({ preventDefault: () => {} });
    });
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
                fieldname: "structureTypeId",
                placeholder: "Structure Type",
                options: [
                  { id: "ALL", name: "All Structures" },
                  ...(this.props.dropdowns.buildingtype || []),
                ],
                disableClearable: true,
                required: false,
                disabled: !this.isAdmin,
                skipAdd: true,
                getOption: (option) => option["name"],
                getDefaultValue: () => {
                  const typeId = this.state.selectedStructureTypeId;
                  if (!typeId || typeId === "ALL") return { id: "ALL", name: "All Structures" };
                  const allTypes = this.props.dropdowns.buildingtype || [];
                  return allTypes.find((t) => t.id === typeId) || { id: "ALL", name: "All Structures" };
                },
                onChange: (e, value) => {
                  const typeId = value ? value.id : "ALL";
                  const allWithType = this.props.dropdowns.usagelocationWithType || [];
                  const filtered = typeId === "ALL"
                    ? allWithType
                    : allWithType.filter((l) => l.typeId === typeId);
                  this.formData.usageLocationId = null;
                  this.formData.structureTypeId = typeId;
                  this.setState({ selectedStructureTypeId: typeId, filteredStructures: filtered });
                },
              })}
              {this.renderAutoComplete({
                fieldname: "usageLocationId",
                placeholder: messages.common.location,
                options: this.state.selectedStructureTypeId === "ALL"
                  ? (this.props.dropdowns.usagelocationWithType || this.props.dropdowns.usagelocation || [])
                  : this.state.filteredStructures,
                disableClearable: true,
                required: true,
                disabled: !this.isAdmin,
                getOption: (option) => option["name"],
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
            <div className="width50 flex">
              {this.renderAutoComplete({
                fieldname: "requestedBy",
                placeholder: "Requested By",
                options: this.props.dropdowns.requestedByOptions || [],
                freeSolo: true,
                helperText: "Type to search existing, or enter a new name",
                getOption: (option) =>
                  typeof option === "string" ? option : option["name"] || "",
              })}
              {this.renderAutoComplete({
                fieldname: "issuedBy",
                placeholder: "Issued By",
                options: this.props.dropdowns.issuedByOptions || [],
                freeSolo: true,
                helperText: "Type to search existing, or enter a new name",
                getOption: (option) =>
                  typeof option === "string" ? option : option["name"] || "",
              })}
            </div>
            <div className="flex">
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
        {/* Batch re-allocation modal */}
        {(() => {
          const m = this.state.batchReassignModal;
          if (!m.open) return null;
          const total = m.entries.reduce((s, e) => s + (parseFloat(e.reassignQty) || 0), 0);
          const remaining = Math.round((m.newQty - total) * 1000) / 1000;
          const isValid = Math.abs(total - m.newQty) < 0.001;
          return (
            <Dialog open maxWidth="sm" fullWidth onClose={() =>
              this.setState({ batchReassignModal: { ...m, open: false } })
            }>
              <DialogTitle style={{ color: '#1565c0' }}>
                Re-allocate Batches — {m.productName}
              </DialogTitle>
              <DialogContent>
                <DialogContentText style={{ marginBottom: 12 }}>
                  New quantity is <strong>{m.newQty}</strong>. Specify how much to draw from each batch.
                </DialogContentText>
                {m.entries.map((entry, i) => (
                  <div key={entry.batchId} style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    background: '#f5f7fa', border: '1px solid #dce3ec',
                    borderRadius: 6, padding: '8px 12px', marginBottom: 8,
                  }}>
                    <div style={{ flex: 1, fontSize: 13 }}>
                      <strong>{entry.label}</strong>
                      <div style={{ color: '#888', fontSize: 11, marginTop: 2 }}>
                        Previously consumed: {entry.consumed}
                      </div>
                    </div>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      placeholder="Qty"
                      value={entry.reassignQty}
                      style={{
                        width: 80, padding: '5px 8px', border: '1px solid #bbb',
                        borderRadius: 4, fontSize: 13,
                      }}
                      onChange={e => {
                        const entries = m.entries.map((en, idx) =>
                          idx === i ? { ...en, reassignQty: e.target.value } : en
                        );
                        this.setState({ batchReassignModal: { ...m, entries } });
                      }}
                    />
                  </div>
                ))}
                <div style={{
                  display: 'flex', justifyContent: 'flex-end', gap: 16,
                  fontSize: 13, color: remaining < 0 ? '#c62828' : remaining === 0 ? '#2e7d32' : '#555',
                  marginBottom: 12,
                }}>
                  <span>Allocated: <strong>{total}</strong></span>
                  <span>Remaining: <strong>{remaining}</strong></span>
                </div>
                <div>
                  <label style={{ fontSize: 12, color: '#555', display: 'block', marginBottom: 4 }}>
                    Override comment *
                  </label>
                  <input
                    type="text"
                    placeholder="Reason for batch selection"
                    value={m.overrideComment}
                    style={{
                      width: '100%', padding: '7px 10px', border: '1px solid #bbb',
                      borderRadius: 4, fontSize: 13, boxSizing: 'border-box',
                    }}
                    onChange={e =>
                      this.setState({ batchReassignModal: { ...m, overrideComment: e.target.value } })
                    }
                  />
                </div>
              </DialogContent>
              <DialogActions>
                <MuiButton onClick={() =>
                  this.setState({ batchReassignModal: { ...m, open: false } })
                }>
                  Cancel
                </MuiButton>
                <MuiButton
                  variant="contained"
                  color="primary"
                  disabled={!isValid || !m.overrideComment.trim()}
                  onClick={() => this.confirmBatchReassign()}
                >
                  Confirm &amp; Save
                </MuiButton>
              </DialogActions>
            </Dialog>
          );
        })()}
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
