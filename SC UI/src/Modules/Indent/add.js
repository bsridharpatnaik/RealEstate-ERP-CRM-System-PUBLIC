//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import moment from "moment";
import AddForm from "./../../Shared/AddForm";
import { API } from "./../../axios";
import Tooltip from "@material-ui/core/Tooltip";
import { withStyles } from "@material-ui/core/styles";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import trashRedIcon from "./../../Shared/Icons/trash-red.png";
import AddIcon from "@material-ui/icons/Add";
import Fab from "@material-ui/core/Fab";
//style
import "./style.scss";
import "../PurchaseOrder/add/stepStyles.scss";
import Step2ReviewIndent from "./step2ReviewIndent";
import { fetchUnit } from "./../../actions/measurementUnit";
//misc
import Button from "./../../Shared/Button";
import CircularProgress from "@material-ui/core/CircularProgress";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";

const HtmlTooltip = withStyles((theme) => ({
  tooltip: {
    backgroundColor: "#ffffff",
    color: "rgba(0, 0, 0, 0.87)",
    maxWidth: 300,
    fontSize: theme.typography.pxToRem(12),
    border: "1px solid #e0e0e0",
    boxShadow: "0px 2px 8px rgba(0, 0, 0, 0.15)",
    padding: "12px",
  },
}))(Tooltip);

class Add extends AddForm {
  title = messages.common.indent;
  addurl = apiEndpoints.createIndent;
  rowRefs = {};
  state = {
    value: 0,
    noinventory: {},
    showValidation: false,
    categories: [],
    selectedFileName: "",
    selectedFilePreview: null,
    isFileUploading: false,
    draftDialogOpen: false,
    pendingDraftData: null,
    isSavingDraft: false,
    isLoadingDraft: false,
    currentStep: 1,
  };
  fileInputRef = React.createRef();
  key = 2;

  async fetchDraft() {
    const response = await API.GET(apiEndpoints.getDraft("INDENT"));
    if (response.success && response.data) {
      this.setState({ draftDialogOpen: true, pendingDraftData: response.data });
    }
    // On 500 or error, ignore - no draft
  }

  componentDidMount() {
    const { dispatch } = this.props;
    dispatch(fetchUnit());
    this.fetchCategories();

    // Initialize with one empty inventory option
    this.setState(
      {
        noinventory: {
          1: {}
        }
      },
      () => {
        if (this.props.prefillData) {
          // Pre-fill from a rejected indent — skip the draft prompt
          this.loadDraftData(this.props.prefillData);
        } else {
          this.fetchDraft();
        }
      }
    );
  }

  componentWillUnmount() {
    // Clean up blob URLs to prevent memory leaks
    if (this.state.selectedFilePreview) {
      URL.revokeObjectURL(this.state.selectedFilePreview);
    }
    if (this.formData.fileInformations) {
      this.formData.fileInformations.forEach(file => {
        if (file.previewUrl) {
          URL.revokeObjectURL(file.previewUrl);
        }
      });
    }
  }

  async fetchCategories() {
    const response = await API.GET(apiEndpoints.getCategoryIdAndNames);
    if (response.success && Array.isArray(response.data)) {
      const categories = response.data.map((cat) => ({
        id: cat.categoryId ?? cat.id,
        name: cat.categoryName ?? cat.name,
      }));
      this.setState({ categories });
    }
  }

  buildDraftPayload() {
    const inventoryList = Object.values(this.state.noinventory).map((item) => ({
      productId: item.productId,
      quantity: parseFloat(item.quantity) || 0,
      specification: item.specification || "",
      remarks: item.remarks || "",
      measurementUnit: item.unit || "",
    }));
    return {
      indentDate: moment().format("DD-MM-YYYY"),
      requiredBy: this.formData.requiredBy,
      fileInformations: (this.formData.fileInformations || []).map((f) => ({
        fileUUId: f.fileUUId,
        fileName: f.fileName || "file",
      })),
      inventoryList,
    };
  }

  saveDraft = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    const payload = this.buildDraftPayload();
    this.setState({ isSavingDraft: true });
    const response = await API.POST(apiEndpoints.saveDraft("INDENT"), payload);
    this.setState({ isSavingDraft: false });
    if (response.success) {
      const msg =
        (response.data && response.data.message) ||
        "Successfully saved draft. Note: Saved drafts are available only for the current day.";
      this.props.enqueueSnackbar(msg, { variant: "success" });
      this.props.back();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to save draft", {
        variant: "error",
      });
    }
  };

  loadDraftData = async (draft) => {
    if (!draft || !Array.isArray(draft.inventoryList)) {
      this.setState({ draftDialogOpen: false, pendingDraftData: null });
      return;
    }
    this.setState({ isLoadingDraft: true, draftDialogOpen: false, pendingDraftData: null });
    this.formData.fileInformations = draft.fileInformations || [];
    const newNoinventory = {};
    let key = 1;
    const categories = this.state.categories || [];
    for (const item of draft.inventoryList) {
      if (!item.productId) continue;
      try {
        const productRes = await API.GET(apiEndpoints.individualProduct + item.productId);
        if (productRes.success && productRes.data) {
          const product = productRes.data;
          // Prefill category from product response (category has categoryId, categoryName)
          const categoryFromProduct =
            product.category &&
            (product.category.categoryId != null || product.category.categoryName != null)
              ? {
                  id: product.category.categoryId ?? product.category.id,
                  name: product.category.categoryName ?? product.category.name ?? "",
                }
              : null;
          const categoryId =
            categoryFromProduct?.id ?? product.categoryId ?? product.category?.categoryId ?? product.category?.id;
          const category = categoryFromProduct || categories.find((c) => String(c.id) === String(categoryId));
          let products = [];
          let selectedProduct = {
            id: product.productId,
            name: product.productName,
            productCode: product.productCode || "",
            measurementUnit: product.measurementUnit || "",
          };
          if (categoryId) {
            const productsRes = await API.GET(
              apiEndpoints.getProductForIndentByCategory(categoryId)
            );
            if (productsRes.success && Array.isArray(productsRes.data)) {
              products = productsRes.data.map((p) => ({
                id: p.productId,
                name: p.productName,
                measurementUnit: p.measurementUnit,
                productCode: p.productCode,
                isManagedInventory: p.isManagedInventory,
                leadTimeDays: p.leadTimeDays ?? null,
              }));
            }
          }
          const [deadStockData, currentStockData, boqData] = await Promise.all([
            this.fetchDeadStockForProduct(item.productId),
            this.fetchCurrentStockForProduct(item.productId),
            this.fetchBOQForProduct(item.productId),
          ]);
          newNoinventory[key] = {
            productId: item.productId,
            quantity: item.quantity,
            specification: item.specification || "",
            remarks: item.remarks || "",
            unit: item.measurementUnit || "",
            selectedCategory: category || null,
            selectedProduct,
            products,
            deadStock: deadStockData,
            deadStockData,
            currentStockData,
            boqData,
            leadTimeDays: item.leadTimeDays ?? null,
          };
        } else {
          const [deadStockData, currentStockData, boqData] = await Promise.all([
            this.fetchDeadStockForProduct(item.productId),
            this.fetchCurrentStockForProduct(item.productId),
            this.fetchBOQForProduct(item.productId),
          ]);
          newNoinventory[key] = {
            productId: item.productId,
            quantity: item.quantity,
            specification: item.specification || "",
            remarks: item.remarks || "",
            unit: item.measurementUnit || "",
            deadStock: deadStockData,
            deadStockData,
            currentStockData,
            boqData,
          };
        }
      } catch (_) {
        const [deadStockData, currentStockData, boqData] = await Promise.all([
          this.fetchDeadStockForProduct(item.productId).catch(() => null),
          this.fetchCurrentStockForProduct(item.productId).catch(() => null),
          this.fetchBOQForProduct(item.productId).catch(() => null),
        ]);
        newNoinventory[key] = {
          productId: item.productId,
          quantity: item.quantity,
          specification: item.specification || "",
          remarks: item.remarks || "",
          unit: item.measurementUnit || "",
          deadStock: deadStockData || { toalDeadStock: 0, detailedDeadStock: [] },
          deadStockData: deadStockData || { toalDeadStock: 0, detailedDeadStock: [] },
          currentStockData: currentStockData || { totalCurrentStock: 0, warehouseWiseStock: [] },
          boqData: boqData || null,
        };
      }
      key++;
    }
    this.key = key;
    this.setState({ noinventory: Object.keys(newNoinventory).length ? newNoinventory : { 1: {} } });
    this.setState({ isLoadingDraft: false });
    this.forceUpdate();
  };

  clearRowProductSelection(key) {
    const p = { ...this.state.noinventory };
    if (p[key]) {
      p[key] = {
        ...p[key],
        productId: "",
        productCode: "",
        unit: "",
        selectedProduct: null,
        deadStock: null,
        deadStockData: null,
        currentStockData: null
      };
      this.setState({ noinventory: p });
    }
  }

  async fetchProductsByCategoryForRow(key, categoryValue) {
    const p = { ...this.state.noinventory };
    if (!p[key]) return;
    p[key] = {
      ...p[key],
      selectedCategory: categoryValue,
      productId: "",
      productCode: "",
      unit: "",
      selectedProduct: null,
      deadStock: null,
      deadStockData: null,
      currentStockData: null,
      products: [],
    };
    this.setState({ noinventory: p });
    if (!categoryValue?.id) return;
    const response = await API.GET(apiEndpoints.getProductForIndentByCategory(categoryValue.id));
    const np = { ...this.state.noinventory };
    if (!np[key]) return;
    if (response.success && Array.isArray(response.data)) {
      if (response.data.length === 0) {
        this.props.enqueueSnackbar(
          "There are no inventories under selected category. Please select a different category.",
          { variant: "warning" }
        );
        np[key] = { ...np[key], products: [] };
        this.setState({ noinventory: np });
        return;
      }
      const transformedProducts = response.data.map((product) => ({
        id: product.productId,
        name: product.productName,
        measurementUnit: product.measurementUnit,
        productCode: product.productCode,
        isManagedInventory: product.isManagedInventory,
        leadTimeDays: product.leadTimeDays ?? null,
      }));
      np[key] = { ...np[key], products: transformedProducts };
      this.setState({ noinventory: np });
    } else {
      np[key] = { ...np[key], products: [] };
      this.setState({ noinventory: np });
    }
  }

renderCurrentStockField(key) {
  const currentStockData = this.state.noinventory[key]?.currentStockData;
  const unit = this.state.noinventory[key]?.unit || "";
  let currentStockValue = "";
  let hasBreakdown = false;

  if (currentStockData && currentStockData.totalCurrentStock != null) {
    const stockValue = currentStockData.totalCurrentStock;
    if (typeof stockValue === 'number') {
      currentStockValue = `${stockValue.toLocaleString('en-IN')} ${unit}`;
    } else if (typeof stockValue === 'string') {
      const numValue = parseFloat(stockValue);
      if (!isNaN(numValue)) {
        currentStockValue = `${numValue.toLocaleString('en-IN')} ${unit}`;
      }
    }

    if (currentStockData.warehouseWiseStock &&
        Array.isArray(currentStockData.warehouseWiseStock) &&
        currentStockData.warehouseWiseStock.length > 0 &&
        currentStockValue !== "") {
      hasBreakdown = true;
    }
  }

  const currentStockField = (
    <div className="current-stock-field-wrapper" key={`currentStock_${key}_${currentStockValue}`}>
      {this.renderTextField({
        fieldname: `currentStock_${key}`,
        placeholder: "Current Stock",
        disabled: true,
        value: currentStockValue,
        skipAdd: true,
      })}
    </div>
  );

  if (hasBreakdown) {
    return (
      <HtmlTooltip
        title={
          <div style={{ textAlign: 'left' }}>
            <div style={{ fontWeight: 'bold', marginBottom: '8px', fontSize: '13px', color: '#323c47' }}>
              Current Stock Breakdown:
            </div>
            {currentStockData.warehouseWiseStock.map((item, idx) => {
              const warehouseName = Object.keys(item)[0];
              const amount = item[warehouseName];
              if (amount === null || amount === undefined) return null;
              const formattedAmount = typeof amount === 'number'
                ? `${amount.toLocaleString('en-IN')} ${unit}`
                : `${amount} ${unit}`;
              return (
                <div key={idx} style={{ marginBottom: '4px', fontSize: '12px', color: '#666' }}>
                  <span style={{ fontWeight: '500', color: '#323c47' }}>{warehouseName}:</span> {formattedAmount}
                </div>
              );
            })}
          </div>
        }
        placement="top"
        arrow
      >
        <span style={{ display: 'block', width: '100%' }}>
          {currentStockField}
        </span>
      </HtmlTooltip>
    );
  }

  return currentStockField;
}

  renderBOQField(key) {
    const boqData = this.state.noinventory[key]?.boqData;
    const unit = this.state.noinventory[key]?.unit || "";
    if (!boqData || !boqData.hasBOQ) return null;

    const { remaining, totalPlanned, totalConsumed } = boqData;
    const remainingVal = typeof remaining === 'number' ? remaining : 0;

    const displayValue = remainingVal < 0
      ? `Exceeded by ${Math.abs(remainingVal).toLocaleString('en-IN', { maximumFractionDigits: 2 })} ${unit}`
      : `${remainingVal.toLocaleString('en-IN', { maximumFractionDigits: 2 })} ${unit}`;

    const tooltipContent = (
      <div style={{ textAlign: 'left' }}>
        <div style={{ fontWeight: 'bold', marginBottom: 6, fontSize: 13, color: '#323c47' }}>BOQ Summary:</div>
        <div style={{ fontSize: 12, color: '#666', marginBottom: 3 }}>
          <span style={{ fontWeight: 500, color: '#323c47' }}>Planned:</span>{' '}
          {(totalPlanned || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })} {unit}
        </div>
        <div style={{ fontSize: 12, color: '#666', marginBottom: 3 }}>
          <span style={{ fontWeight: 500, color: '#323c47' }}>Already Indented:</span>{' '}
          {(totalConsumed || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })} {unit}
        </div>
        <div style={{ fontSize: 12, fontWeight: 600 }}>
          Remaining: {remainingVal.toLocaleString('en-IN', { maximumFractionDigits: 2 })} {unit}
        </div>
      </div>
    );

    const accentColor = remainingVal < 0 ? '#e53935' : '#27ae60';

    const boqField = (
      <div className="boq-field-wrapper" key={`boq_${key}_${remainingVal}`}
        style={{ borderLeft: `3px solid ${accentColor}`, borderRadius: 4 }}>
        {this.renderTextField({
          fieldname: `boqRemaining_${key}`,
          placeholder: "BOQ Remaining",
          disabled: true,
          value: displayValue,
          skipAdd: true,
        })}
      </div>
    );

    return (
      <HtmlTooltip title={tooltipContent} placement="top" arrow>
        <span style={{ display: 'block', width: '100%' }}>{boqField}</span>
      </HtmlTooltip>
    );
  }

  renderDeadStockField(key) {
    const deadStockData = this.state.noinventory[key]?.deadStockData;
    const unit = this.state.noinventory[key]?.unit || "";
    let deadStockValue = "";
    let hasBreakdown = false;

    if (deadStockData && deadStockData.toalDeadStock !== null && deadStockData.toalDeadStock !== undefined) {
      const stockValue = deadStockData.toalDeadStock;
      if (typeof stockValue === 'number') {
        deadStockValue = `${stockValue.toLocaleString('en-IN')} ${unit}`;
      } else if (typeof stockValue === 'string') {
        const numValue = parseFloat(stockValue);
        if (!isNaN(numValue)) {
          deadStockValue = `${numValue.toLocaleString('en-IN')} ${unit}`;
        } else {
          deadStockValue = stockValue.trim();
        }
      }

      if (deadStockData.detailedDeadStock &&
          Array.isArray(deadStockData.detailedDeadStock) &&
          deadStockData.detailedDeadStock.length > 0 &&
          deadStockValue !== "") {
        hasBreakdown = true;
      }
    }

    const deadStockField = (
      <div className="dead-stock-field-wrapper" key={`deadStock_${key}_${deadStockValue}`}>
        {this.renderTextField({
          fieldname: `deadStock_${key}`,
          placeholder: "Dead Stock",
          disabled: true,
          value: deadStockValue,
          skipAdd: true,
        })}
      </div>
    );

    if (hasBreakdown) {
      return (
        <HtmlTooltip
          title={
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '8px', fontSize: '13px', color: '#323c47' }}>
                Dead Stock Breakdown:
              </div>
              {deadStockData.detailedDeadStock.map((item, idx) => {
                const tenantName = Object.keys(item)[0];
                const amount = item[tenantName];
                if (amount === null || amount === undefined) return null;
                const formattedAmount = typeof amount === 'number'
                  ? `${amount.toLocaleString('en-IN')} ${unit}`
                  : `${amount} ${unit}`;
                return (
                  <div key={idx} style={{ marginBottom: '4px', fontSize: '12px', color: '#666' }}>
                    <span style={{ fontWeight: '500', color: '#323c47' }}>{tenantName}:</span> {formattedAmount}
                  </div>
                );
              })}
            </div>
          }
          placement="top"
          arrow
        >
          <span style={{ display: 'block', width: '100%' }}>
            {deadStockField}
          </span>
        </HtmlTooltip>
      );
    }

    return deadStockField;
  }

  /** Fetches dead stock for a product; returns data only (no state update). Used when loading draft/resubmit. */
  async fetchDeadStockForProduct(productId) {
    if (!productId) return { toalDeadStock: 0, detailedDeadStock: [] };
    try {
      const response = await API.GET(`${apiEndpoints.getDeadStockByProduct}?productId=${productId}`);
      if (response.success && response.data) return response.data;
      return { toalDeadStock: 0, detailedDeadStock: [] };
    } catch (_) {
      return { toalDeadStock: 0, detailedDeadStock: [] };
    }
  }

  /** Fetches current stock for a product; returns data only (no state update). Used when loading draft/resubmit. */
  async fetchCurrentStockForProduct(productId) {
    if (!productId) return { totalCurrentStock: 0, warehouseWiseStock: [] };
    try {
      const response = await API.GET(`${apiEndpoints.getCurrentStockForIndent}?productId=${productId}`);
      if (response.success && response.data) return response.data;
      return { totalCurrentStock: 0, warehouseWiseStock: [] };
    } catch (_) {
      return { totalCurrentStock: 0, warehouseWiseStock: [] };
    }
  }

  /** Fetches BOQ summary for a product; returns null on error or no BOQ. Used when loading draft/resubmit. */
  async fetchBOQForProduct(productId) {
    if (!productId) return null;
    try {
      const response = await API.GET(`${apiEndpoints.getProductBOQSummary}?productId=${productId}`);
      if (response.success && response.data && response.data.hasBOQ) return response.data;
      return null;
    } catch (_) {
      return null;
    }
  }

  async fetchDeadStock(productId, key) {
    if (!productId) {
      const p = this.state.noinventory;
      if (p[key]) {
        p[key].deadStock = null;
        p[key].deadStockData = null;
        p[key].currentStockData = null;
        p[key].boqData = null;
        this.setState({ noinventory: { ...p } });
      }
      return;
    }

    try {
      // All three calls fire at the same time
      const [deadStockRes, currentStockRes, boqRes] = await Promise.allSettled([
        API.GET(`${apiEndpoints.getDeadStockByProduct}?productId=${productId}`),
        API.GET(`${apiEndpoints.getCurrentStockForIndent}?productId=${productId}`),
        API.GET(`${apiEndpoints.getProductBOQSummary}?productId=${productId}`),
      ]);

      const deadData = (deadStockRes.status === 'fulfilled' && deadStockRes.value?.success)
        ? deadStockRes.value.data
        : { toalDeadStock: 0, detailedDeadStock: [] };

      const currentData = (currentStockRes.status === 'fulfilled' && currentStockRes.value?.success)
        ? currentStockRes.value.data
        : { totalCurrentStock: 0, warehouseWiseStock: [] };

      const boqData = (boqRes.status === 'fulfilled' && boqRes.value?.success)
        ? boqRes.value.data
        : null;

      const p = this.state.noinventory;
      if (p[key]) {
        p[key].deadStock = deadData;
        p[key].deadStockData = deadData;
        p[key].currentStockData = currentData;
        p[key].boqData = boqData;
        this.setState({ noinventory: { ...p } });
      }
    } catch (error) {
      const p = this.state.noinventory;
      if (p[key]) {
        p[key].deadStock = { toalDeadStock: 0, detailedDeadStock: [] };
        p[key].deadStockData = { toalDeadStock: 0, detailedDeadStock: [] };
        p[key].currentStockData = { totalCurrentStock: 0, warehouseWiseStock: [] };
        p[key].boqData = null;
        this.setState({ noinventory: { ...p } });
      }
    }
  }

  renderInventory(key) {
    const currentInventoryId = this.state.noinventory?.[key]?.productId;
    const selectedInventories = Object.keys(this.state.noinventory).map(index => this?.state?.noinventory?.[index]?.productId);
    const productList = this.state.noinventory?.[key]?.products || [];
    const remainingInventories = productList.filter(item => (!selectedInventories.includes(item.id) || currentInventoryId === item.id));
    const inventoryNumber = Number(key);

    const quantityMissing = this.state.showValidation &&
      (!this.state.noinventory[key]?.quantity || Number(this.state.noinventory[key]?.quantity) <= 0);

    return (
      <div
        className="inventory-item"
        key={key}
        ref={(el) => { this.rowRefs[key] = el; }}
      >
        <div className="inventory-item-header">
          <span>Inventory {inventoryNumber}</span>
          <IconButton
            aria-label="delete"
            onClick={() => {
              if (Object.keys(this.state.noinventory).length <= 1) {
                this.props.enqueueSnackbar("At least one inventory option must be present", {
                  variant: "warning",
                });
                return;
              }
              const p = this.state.noinventory;
              delete p[key];
              this.setState({ noinventory: { ...p } }, () => {
                if (this.props.onValidationChange) {
                  this.props.onValidationChange();
                }
              });
            }}
            className="delete-icon"
          >
            <img src={trashRedIcon} alt="Delete" className="trash-red-icon" />
          </IconButton>
        </div>
        <div className="flex inventory-row-first">
          {this.renderAutoComplete({
            fieldname: `category_${key}`,
            placeholder: "Category Name",
            options: this.state.categories,
            disableClearable: false,
            required: true,
            value: this.state.noinventory[key]?.selectedCategory || null,
            getOption: (option) => option.name,
            onChange: (e, value) => {
              this.fetchProductsByCategoryForRow(key, value);
              if (this.props.onValidationChange) this.props.onValidationChange();
            },
          })}
          {this.renderAutoComplete({
            fieldname: "productId",
            placeholder: "Inventory Name",
            options: remainingInventories,
            disableClearable: true,
            required: true,
            value: this.state.noinventory[key]?.selectedProduct || null,
            getOption: (option) => {
              return option["name"];
            },
            onChange: (e, value) => {
              const p = this.state.noinventory;
              p[key].productId = value?.id || "";
              p[key].productCode = value?.productCode || "";
              p[key].unit = value?.measurementUnit || "";
              p[key].leadTimeDays = value?.leadTimeDays ?? null;
              p[key].selectedProduct = value;
              if (value) {
                this.setState({ noinventory: { ...p } }, () => {
                  if (value?.id) {
                    this.fetchDeadStock(value.id, key);
                  }
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              } else {
                p[key].deadStock = null;
                p[key].deadStockData = null;
                this.setState({ noinventory: { ...p } });
              }
            },
          })}
          {this.renderAutoComplete({
            fieldname: "productCode",
            placeholder: "Inventory Code",
            options: remainingInventories,
            disableClearable: true,
            required: true,
            value: this.state.noinventory[key]?.selectedProduct || null,
            getOption: (option) => {
              return option["productCode"];
            },
            onChange: (e, value) => {
              const p = this.state.noinventory;
              p[key].productId = value?.id || "";
              p[key].productCode = value?.productCode || "";
              p[key].unit = value?.measurementUnit || "";
              p[key].leadTimeDays = value?.leadTimeDays ?? null;
              p[key].selectedProduct = value;
              if (value) {
                this.setState({ noinventory: { ...p } }, () => {
                  if (value?.id) {
                    this.fetchDeadStock(value.id, key);
                  }
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              } else {
                p[key].deadStock = null;
                p[key].deadStockData = null;
                this.setState({ noinventory: { ...p } });
              }
            },
          })}
        </div>
        <div className={`flex inventory-row-second${quantityMissing ? " quantity-row-error" : ""}`}>
          <div className={`quantity-field-wrapper${quantityMissing ? " quantity-error" : ""}`}>
            {this.renderTextField({
              fieldname: "quantity",
              placeholder: "Enter Quantity",
              type: "number",
              required: true,
              skipAdd: true,
              validation: "nonegative",
              value: this.state.noinventory[key]?.quantity || "",
              onChange: (value) => {
                const p = this.state.noinventory;
                p[key].quantity = value;
                this.setState({ noinventory: { ...p } }, () => {
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              },
            })}
            {quantityMissing && (
              <span className="quantity-error-msg">Quantity is required</span>
            )}
          </div>
          {this.renderDeadStockField(key)}
          {this.renderCurrentStockField(key)}
          {this.renderBOQField(key)}
          {this.renderTextField({
            fieldname: `unit_${key}`,
            placeholder: "Measurement Unit",
            disabled: true,
            value: this.state.noinventory[key]?.unit || "",
            skipAdd: true,
          })}
        </div>
        <div className="flex">
          {this.renderTextField({
            fieldname: "specification",
            placeholder: "Enter Specification",
            skipAdd: true,
            validation: "maxlength",
            lengthConstraint: 100,
            errorMessage: "only 100 characters allowed",
            value: this.state.noinventory[key]?.specification ?? "",
            onChange: (value) => {
              const p = this.state.noinventory;
              p[key].specification = value;
              this.setState({ noinventory: { ...p } });
            },
          })}
          {this.renderTextField({
            fieldname: "remarks",
            placeholder: "Enter Remarks",
            skipAdd: true,
            validation: "maxlength",
            lengthConstraint: 100,
            errorMessage: "only 100 characters allowed",
            value: this.state.noinventory[key]?.remarks ?? "",
            onChange: (value) => {
              const p = this.state.noinventory;
              p[key].remarks = value;
              this.setState({ noinventory: { ...p } });
            },
          })}
          {this.state.noinventory[key]?.leadTimeDays != null && (
            <div className="lead-time-chip">
              <span className="lead-time-chip-icon">⏱</span>
              Lead Time: <strong>{this.state.noinventory[key].leadTimeDays} days</strong>
            </div>
          )}
        </div>
      </div>
    );
  }

  autoSaveDraftSilently = async () => {
    try {
      const payload = this.buildDraftPayload();
      await API.POST(apiEndpoints.saveDraft("INDENT"), payload);
    } catch (_) {
      // best-effort — ignore draft save errors
    }
  };

  async add(event) {
    event.preventDefault();

    if (Object.keys(this.state.noinventory).length === 0) {
      this.props.enqueueSnackbar("Add atleast one Inventory", {
        variant: "error",
      });
      return;
    }

    // Validate specification and remarks length (max 100 chars)
    const invalidItems = Object.values(this.state.noinventory).filter(
      (item) =>
        (item.specification && item.specification.length > 100) ||
        (item.remarks && item.remarks.length > 100)
    );
    if (invalidItems.length > 0) {
      this.props.enqueueSnackbar("only 100 characters allowed", {
        variant: "error",
      });
      return;
    }

    const inventoryList = Object.values(this.state.noinventory).map((item) => ({
      productId: item.productId,
      quantity: parseFloat(item.quantity) || 0,
      specification: item.specification || "",
      remarks: item.remarks || "",
      measurementUnit: item.unit || "",
    }));

    const params = {
      indentDate: moment().format("DD-MM-YYYY"),
      requiredBy: this.formData.requiredBy,
      fileInformations: this.formData.fileInformations || [],
      inventoryList: inventoryList,
    };

    this.setState({ isAdding: true });
    try {
      const response = await API.POST(this.addurl, params);
      this.setState({ isAdding: false });
      if (response.success) {
        this.showToaster(response);
      } else {
        await this.autoSaveDraftSilently();
        this.props.enqueueSnackbar(
          (response.errorMessage || "Save failed.") + " Your data has been saved as a draft automatically.",
          { variant: "warning" }
        );
        this.setState({ currentStep: 1 });
      }
    } catch (error) {
      this.setState({ isAdding: false });
      await this.autoSaveDraftSilently();
      this.props.enqueueSnackbar(
        "An error occurred. Your data has been saved as a draft automatically.",
        { variant: "warning" }
      );
      this.setState({ currentStep: 1 });
    }
  }

  handleSave = (e) => {
    e.preventDefault();
    this.add(e);
  }

  submitForm = () => {
    if (Object.keys(this.state.noinventory).length === 0) {
      this.props.enqueueSnackbar("Add at least one item.", { variant: "error" });
      return;
    }

    // Find first item missing a product selection
    const firstMissingProduct = Object.keys(this.state.noinventory).find(key =>
      !this.state.noinventory[key]?.productId
    );
    if (firstMissingProduct) {
      this.props.enqueueSnackbar("Please select a product for all items.", { variant: "error" });
      this.setState({ showValidation: true }, () => {
        const ref = this.rowRefs[firstMissingProduct];
        if (ref) ref.scrollIntoView({ behavior: "smooth", block: "center" });
      });
      return;
    }

    // Find first item missing a valid quantity
    const firstInvalidKey = Object.keys(this.state.noinventory).find(key => {
      const qty = this.state.noinventory[key]?.quantity;
      return !qty || isNaN(Number(qty)) || Number(qty) <= 0;
    });
    if (firstInvalidKey) {
      this.setState({ showValidation: true }, () => {
        const ref = this.rowRefs[firstInvalidKey];
        if (ref) ref.scrollIntoView({ behavior: "smooth", block: "center" });
      });
      return;
    }

    // Validation passed — advance to review step
    this.setState({ showValidation: false, currentStep: 2 });
  }

  handleConfirmSave = () => {
    const syntheticEvent = { preventDefault: () => {} };
    this.add(syntheticEvent);
  }

  getSaveButtonDisabled = () => {
    return (
      this.state.isAdding ||
      this.state.isFileUploading ||
      Object.keys(this.formValidation).length > 0
    );
  }

  renderBreadcrumbs() {
    const { currentStep } = this.state;
    const steps = [
      messages.common.indent,
      messages.common.add + " " + messages.common.indent,
      currentStep === 2 ? "Review" : null,
    ].filter(Boolean);
    return (
      <Breadcrumbs
        separator={<NavigateNextIcon />}
        aria-label="breadcrumb"
        className="breadcrumbs"
      >
        {steps.map((step, index) => (
          <span key={index}>{step}</span>
        ))}
      </Breadcrumbs>
    );
  }

  renderHeaderActions() {
    const { currentStep } = this.state;
    if (currentStep === 2) {
      return (
        <div className="po-action-buttons">
          <Button
            onClick={() => this.setState({ currentStep: 1 })}
            buttonClass="grey"
            label="← Back to Edit"
            disabled={this.state.isAdding}
          />
          <Button
            onClick={this.handleConfirmSave}
            buttonClass="blue"
            label={this.state.isAdding ? "Saving..." : "Confirm & Save"}
            disabled={this.state.isAdding}
          />
        </div>
      );
    }
    return (
      <div className="po-action-buttons">
        <Button
          onClick={this.props.back}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          onClick={this.saveDraft}
          buttonClass="grey"
          label="Save as Draft"
          disabled={this.state.isSavingDraft}
        />
        <Button
          onClick={() => this.submitForm()}
          buttonClass="blue"
          label="Review"
          disabled={this.getSaveButtonDisabled()}
        />
      </div>
    );
  }

  renderAddHeading() {
    return (
      <div className="add-heading-wrapper">
        <IconButton
          aria-label="back"
          onClick={this.props.back}
          className="back-icon"
        >
          <KeyboardBackspaceIcon />
        </IconButton>
        <span className="add-heading">
          {messages.common.add} {messages.common.indent}
        </span>
      </div>
    );
  }

  hasEmptyInventoryRecords = () => {
    const inventories = Object.values(this.state.noinventory || {});
    if (inventories.length === 0) return true;

    return inventories.some(inventory => {
      if (!inventory) return true;
      const hasProduct = inventory.productId && inventory.productId !== "" && inventory.productId !== null && inventory.productId !== undefined;
      const quantityValue = inventory.quantity;
      const hasQuantity = quantityValue !== undefined &&
                         quantityValue !== null &&
                         quantityValue !== "" &&
                         !isNaN(Number(quantityValue)) &&
                         Number(quantityValue) > 0;
      return !hasProduct || !hasQuantity;
    });
  }

  componentDidUpdate(prevProps, prevState) {
    const prevKeys = Object.keys(prevState.noinventory || {});
    const currentKeys = Object.keys(this.state.noinventory || {});

    const keysChanged = prevKeys.length !== currentKeys.length;

    const dataChanged = prevKeys.some(key => {
      if (!this.state.noinventory[key]) return true;
      const prev = prevState.noinventory[key];
      const curr = this.state.noinventory[key];
      return (prev?.productId !== curr?.productId) || (prev?.quantity !== curr?.quantity);
    });

    const newKeysAdded = currentKeys.some(key => !prevKeys.includes(key));

    if ((keysChanged || dataChanged || newKeysAdded) && this.props.onValidationChange) {
      setTimeout(() => {
        this.props.onValidationChange();
      }, 0);
    }
  }

  handleFileSelect(e) {
    const file = e.target.files[0];
    if (!file) {
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      return;
    }

    const fileSize = file.size / 1024 / 1024;
    if (fileSize > 2) {
      this.props.enqueueSnackbar("File upload is restricted to 2MB", {
        variant: "error",
      });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) {
        this.fileInputRef.current.value = "";
      }
      return;
    }

    const validTypes = [
      "image/jpeg", "image/jpg", "image/png",
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/msword",
      "application/vnd.ms-excel",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ];
    if (!validTypes.includes(file.type)) {
      this.props.enqueueSnackbar("Only JPG, PNG, PDF, Word, and Excel files are allowed", {
        variant: "error",
      });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) {
        this.fileInputRef.current.value = "";
      }
      return;
    }

    const isImage = file.type.startsWith("image/");
    const previewUrl = isImage ? URL.createObjectURL(file) : null;
    this.setState({
      selectedFileName: file.name,
      selectedFilePreview: previewUrl
    });
  }

  async handleFileSubmit() {
    if (!this.fileInputRef.current || !this.fileInputRef.current.files[0]) {
      this.props.enqueueSnackbar("Please select a file first", {
        variant: "error",
      });
      return;
    }

    const file = this.fileInputRef.current.files[0];
    const formData = new FormData();
    formData.append("file", file, file.name);

    this.setState({ isFileUploading: true });

    try {
      const response = await API.POST(apiEndpoints.masterFileUpload, formData);
      if (response.success) {
        if (!this.formData.fileInformations) {
          this.formData.fileInformations = [];
        }
        const fileData = {
          ...response.data,
          previewUrl: this.state.selectedFilePreview,
        };
        this.formData.fileInformations.push(fileData);
        this.setState({
          fileUploadKey: Date.now(),
          selectedFileName: "",
          selectedFilePreview: null,
          isFileUploading: false
        });
        this.props.enqueueSnackbar("File uploaded successfully", {
          variant: "success",
        });
        if (this.fileInputRef.current) {
          this.fileInputRef.current.value = "";
        }
      } else {
        if (this.state.selectedFilePreview) {
          URL.revokeObjectURL(this.state.selectedFilePreview);
        }
        this.setState({ isFileUploading: false });
        this.props.enqueueSnackbar(response.errorMessage || "Upload failed", {
          variant: "error",
        });
      }
    } catch (error) {
      if (this.state.selectedFilePreview) {
        URL.revokeObjectURL(this.state.selectedFilePreview);
      }
      this.setState({ isFileUploading: false });
      this.props.enqueueSnackbar("Upload failed", {
        variant: "error",
      });
    }
  }

  handleFileRemove(fileToRemove) {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    if (fileToRemove.previewUrl) {
      URL.revokeObjectURL(fileToRemove.previewUrl);
    }
    this.formData.fileInformations = this.formData.fileInformations.filter(
      (file) => file.fileUUId !== fileToRemove.fileUUId
    );
    this.forceUpdate();
  }

  renderFileArea() {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    const files = this.formData.fileInformations || [];

    return (
      <div className="indent-upload-section">
        <div className="upload-controls">
          <div className="upload-left">
            <div className="upload-buttons-row">
              <input
                ref={this.fileInputRef}
                type="file"
                id="indent-file-upload"
                accept="image/jpeg,image/jpg,image/png,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(e) => this.handleFileSelect(e)}
                style={{ display: "none" }}
              />
              <label
                htmlFor="indent-file-upload"
                className="file-name-input-label"
                onClick={(e) => {
                  e.preventDefault();
                  if (this.fileInputRef.current) {
                    this.fileInputRef.current.click();
                  }
                }}
              >
                <input
                  type="text"
                  className="file-name-input"
                  value={this.state.selectedFileName}
                  placeholder="choose file"
                  readOnly
                />
              </label>
              <Button
                buttonClass="blue"
                label="Submit"
                onClick={() => this.handleFileSubmit()}
                disabled={this.state.isFileUploading}
              />
              {this.state.isFileUploading && (
                <CircularProgress
                  size={20}
                  style={{ marginLeft: "10px", color: "#1976d2" }}
                />
              )}
              <div className="upload-thumbnails">
                {files.map((file, index) => (
                  <div key={file.fileUUId || index} className="upload-thumbnail">
                    <IconButton
                      className="thumbnail-remove"
                      onClick={() => this.handleFileRemove(file)}
                    >
                      <img src={trashRedIcon} alt="Remove" className="trash-red-icon" />
                    </IconButton>
                    <div className="thumbnail-preview">
                      {file.previewUrl ? (
                        <img
                          src={file.previewUrl}
                          alt={file.fileName}
                        />
                      ) : (
                        <div className="thumbnail-placeholder">
                          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.89 22 5.99 22H18C19.1 22 20 21.1 20 20V8L14 2ZM18 20H6V4H13V9H18V20Z" fill="#999"/>
                          </svg>
                        </div>
                      )}
                    </div>
                    {file.fileName && (
                      <div className="thumbnail-filename" title={file.fileName}>
                        {file.fileName.length > 15 ? file.fileName.substring(0, 13) + "…" : file.fileName}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
            <div className="upload-hint">
              Upload a file here, Max 2 MB (JPG, PNG, PDF, Word, Excel)
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderInventoryAddButton() {
    return (
      <div className="add-inventory-button-wrapper">
        <Button
          onClick={() => {
            const p = this.state.noinventory;
            p[this.key++] = {};
            this.setState({ noinventory: { ...p } }, () => {
              if (this.props.onValidationChange) {
                this.props.onValidationChange();
              }
            });
          }}
          buttonClass="grey"
          label="+ Add Inventory"
          disabled={Object.keys(this.state.noinventory).length === 50}
        />
      </div>
    );
  }

  renderDraftDialog() {
    return (
      <Dialog
        open={this.state.draftDialogOpen}
        onClose={() => this.setState({ draftDialogOpen: false, pendingDraftData: null })}
        maxWidth="xs"
        fullWidth
        aria-labelledby="draft-dialog-title"
      >
        <DialogTitle id="draft-dialog-title">Load draft?</DialogTitle>
        <DialogContent>
          There is a saved draft, do you want to load it?
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => this.setState({ draftDialogOpen: false, pendingDraftData: null })}
            buttonClass="grey"
            label="No"
          />
          <Button
            onClick={() => this.state.pendingDraftData && this.loadDraftData(this.state.pendingDraftData)}
            buttonClass="blue"
            label="Yes"
          />
        </DialogActions>
      </Dialog>
    );
  }

  render() {
    const { currentStep } = this.state;
    return (
      <div className="list-section add create-po-wrapper">
        {this.renderDraftDialog()}
        <div className="create-po-header">
          <div className="create-po-header-row">
            {this.renderAddHeading()}
            {this.renderHeaderActions()}
          </div>
          {this.renderBreadcrumbs()}
        </div>
        <div className="create-po-content">
          {currentStep === 2 ? (
            <Step2ReviewIndent
              noinventory={this.state.noinventory}
              fileInformations={this.formData.fileInformations || []}
              indentDate={require("moment")().format("DD-MM-YYYY")}
              isSaving={this.state.isAdding}
              onBack={() => this.setState({ currentStep: 1 })}
              onConfirm={this.handleConfirmSave}
            />
          ) : (
            <>
              {this.state.isLoadingDraft && (
                <div style={{ display: "flex", justifyContent: "center", padding: "24px" }}>
                  <CircularProgress />
                </div>
              )}
              {!this.state.isLoadingDraft && (
                <form onSubmit={(e) => this.add(e)}>
                  <div className="indent-header-card">
                    <div className="header-required-by">
                      {this.renderAutoComplete({
                        fieldname: "requiredBy",
                        placeholder: "Required By",
                        options: this.props.dropdowns?.requiredByOptions || [],
                        freeSolo: true,
                        helperText: "Type to search existing, or enter a new name",
                        getOption: (option) =>
                          typeof option === "string" ? option : option["name"] || "",
                        onChange: (e, value) => {
                          this.formData.requiredBy =
                            typeof value === "string" ? value : value ? value.name : undefined;
                        },
                      })}
                    </div>
                    <div className="header-divider" />
                    <div className="header-upload-documents">
                      <div className="upload-documents-heading">Upload Documents</div>
                      {this.renderFileArea()}
                    </div>
                  </div>
                  {this.renderInventoryAddButton()}
                  <div className="inventories-list">
                    {Object.keys(this.state.noinventory).sort((a, b) => Number(b) - Number(a)).map((key) =>
                      this.renderInventory(key)
                    )}
                  </div>
                </form>
              )}
            </>
          )}
        </div>
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
  withSnackbar(Add)
);