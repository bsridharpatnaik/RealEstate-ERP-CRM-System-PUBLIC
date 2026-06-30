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
import EditIcon from "@material-ui/icons/Edit";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import trashRedIcon from "./../../Shared/Icons/trash-red.png";
import AddIcon from "@material-ui/icons/Add";
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
  state = {
    value: 0,
    noinventory: {},
    categories: [],
    selectedFileName: "",
    selectedFilePreview: null,
    isFileUploading: false,
    draftDialogOpen: false,
    pendingDraftData: null,
    isSavingDraft: false,
    isLoadingDraft: false,
    currentStep: 1,
    // Item add/edit dialog — items are only added to noinventory once
    // validated and saved here, never edited inline in the list.
    itemDialogOpen: false,
    dialogItem: {},
    dialogEditingKey: null,
    dialogValidation: false,
  };
  fileInputRef = React.createRef();
  key = 1;

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

    if (this.props.prefillData) {
      // Pre-fill from a rejected indent — skip the draft prompt
      this.loadDraftData(this.props.prefillData);
    } else {
      this.fetchDraft();
    }
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
    this.setState({ noinventory: newNoinventory });
    this.setState({ isLoadingDraft: false });
    this.forceUpdate();
  };

  /** Item add/edit dialog — opens empty for a new item. */
  openAddItemDialog = () => {
    this.setState({
      itemDialogOpen: true,
      dialogItem: {},
      dialogEditingKey: null,
      dialogValidation: false,
    });
  };

  /** Item add/edit dialog — opens pre-filled with an existing item's data. */
  openEditItemDialog = (key) => {
    this.setState({
      itemDialogOpen: true,
      dialogItem: { ...this.state.noinventory[key] },
      dialogEditingKey: key,
      dialogValidation: false,
    });
  };

  closeItemDialog = () => {
    this.setState({
      itemDialogOpen: false,
      dialogItem: {},
      dialogEditingKey: null,
      dialogValidation: false,
    });
  };

  /** Products already used by other items, excluded from the dialog's product list (unless editing that same item). */
  getDialogRemainingProducts() {
    const item = this.state.dialogItem || {};
    const productList = item.products || [];
    const editingKey = this.state.dialogEditingKey;
    const selectedElsewhere = Object.keys(this.state.noinventory)
      .filter((k) => String(k) !== String(editingKey))
      .map((k) => this.state.noinventory[k]?.productId);
    return productList.filter(
      (p) => !selectedElsewhere.includes(p.id) || item.productId === p.id
    );
  }

  handleDialogCategoryChange = async (categoryValue) => {
    this.setState((prev) => ({
      dialogItem: {
        ...prev.dialogItem,
        selectedCategory: categoryValue,
        productId: "",
        productCode: "",
        unit: "",
        selectedProduct: null,
        deadStockData: null,
        currentStockData: null,
        boqData: null,
        leadTimeDays: null,
        products: [],
      },
    }));
    if (!categoryValue?.id) return;
    const response = await API.GET(apiEndpoints.getProductForIndentByCategory(categoryValue.id));
    if (!this.state.itemDialogOpen) return;
    if (response.success && Array.isArray(response.data)) {
      if (response.data.length === 0) {
        this.props.enqueueSnackbar(
          "There are no inventories under selected category. Please select a different category.",
          { variant: "warning" }
        );
      }
      const transformedProducts = response.data.map((product) => ({
        id: product.productId,
        name: product.productName,
        measurementUnit: product.measurementUnit,
        productCode: product.productCode,
        isManagedInventory: product.isManagedInventory,
        leadTimeDays: product.leadTimeDays ?? null,
      }));
      this.setState((prev) =>
        prev.itemDialogOpen
          ? { dialogItem: { ...prev.dialogItem, products: transformedProducts } }
          : prev
      );
    } else {
      this.setState((prev) =>
        prev.itemDialogOpen ? { dialogItem: { ...prev.dialogItem, products: [] } } : prev
      );
    }
  };

  handleDialogProductChange = async (value) => {
    this.setState((prev) => ({
      dialogItem: {
        ...prev.dialogItem,
        productId: value?.id || "",
        productCode: value?.productCode || "",
        unit: value?.measurementUnit || "",
        leadTimeDays: value?.leadTimeDays ?? null,
        selectedProduct: value || null,
        deadStockData: null,
        currentStockData: null,
        boqData: null,
      },
    }));
    if (!value?.id) return;
    const [deadStockData, currentStockData, boqData] = await Promise.all([
      this.fetchDeadStockForProduct(value.id),
      this.fetchCurrentStockForProduct(value.id),
      this.fetchBOQForProduct(value.id),
    ]);
    this.setState((prev) =>
      prev.itemDialogOpen
        ? { dialogItem: { ...prev.dialogItem, deadStockData, currentStockData, boqData } }
        : prev
    );
  };

  saveDialogItem = () => {
    const item = this.state.dialogItem || {};
    const hasProduct = !!item.productId;
    const qty = item.quantity;
    const hasQty = qty !== undefined && qty !== null && qty !== "" && !isNaN(Number(qty)) && Number(qty) > 0;
    const specTooLong = item.specification && item.specification.length > 100;
    const remarksTooLong = item.remarks && item.remarks.length > 100;

    if (!hasProduct || !hasQty || specTooLong || remarksTooLong) {
      this.setState({ dialogValidation: true });
      if (!hasProduct) {
        this.props.enqueueSnackbar("Please select a product", { variant: "error" });
      } else if (!hasQty) {
        this.props.enqueueSnackbar("Please enter a valid quantity", { variant: "error" });
      } else {
        this.props.enqueueSnackbar("Only 100 characters allowed", { variant: "error" });
      }
      return;
    }

    const key = this.state.dialogEditingKey !== null ? this.state.dialogEditingKey : this.key++;
    const updated = { ...this.state.noinventory, [key]: { ...item } };
    this.setState(
      {
        noinventory: updated,
        itemDialogOpen: false,
        dialogItem: {},
        dialogEditingKey: null,
        dialogValidation: false,
      },
      () => {
        if (this.props.onValidationChange) {
          this.props.onValidationChange();
        }
      }
    );
  };

  deleteItem = (key) => {
    const p = { ...this.state.noinventory };
    delete p[key];
    this.setState({ noinventory: p }, () => {
      if (this.props.onValidationChange) {
        this.props.onValidationChange();
      }
    });
  };

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

  /**
   * Compact read-only stock/BOQ chips shown inside the item dialog only — kept out of
   * the main list per request, since they're informational, not data the user edits.
   */
  renderDialogStockChips(item) {
    if (!item.productId) return null;
    const unit = item.unit || "";
    const chips = [];

    if (item.currentStockData && item.currentStockData.totalCurrentStock != null) {
      const raw = item.currentStockData.totalCurrentStock;
      const num = typeof raw === "number" ? raw : parseFloat(raw);
      if (!isNaN(num)) {
        const breakdown = (item.currentStockData.warehouseWiseStock || []).filter(
          (w) => Object.values(w)[0] != null
        );
        const chip = (
          <div className="stock-chip" key="current">
            <span className="stock-chip-label">Current Stock</span>
            <span className="stock-chip-value">{num.toLocaleString("en-IN")} {unit}</span>
          </div>
        );
        chips.push(
          breakdown.length > 0 ? (
            <HtmlTooltip
              key="current"
              placement="top"
              arrow
              title={
                <div style={{ textAlign: "left" }}>
                  <div style={{ fontWeight: "bold", marginBottom: 8, fontSize: 13, color: "#323c47" }}>
                    Current Stock Breakdown:
                  </div>
                  {breakdown.map((w, idx) => {
                    const name = Object.keys(w)[0];
                    const amt = w[name];
                    const formatted = typeof amt === "number" ? `${amt.toLocaleString("en-IN")} ${unit}` : `${amt} ${unit}`;
                    return (
                      <div key={idx} style={{ marginBottom: 4, fontSize: 12, color: "#666" }}>
                        <span style={{ fontWeight: 500, color: "#323c47" }}>{name}:</span> {formatted}
                      </div>
                    );
                  })}
                </div>
              }
            >
              <span style={{ display: "block", flex: "1 1 0%", minWidth: 0 }}>{chip}</span>
            </HtmlTooltip>
          ) : (
            chip
          )
        );
      }
    }

    if (item.deadStockData && item.deadStockData.toalDeadStock != null) {
      const raw = item.deadStockData.toalDeadStock;
      const num = typeof raw === "number" ? raw : parseFloat(raw);
      if (!isNaN(num)) {
        const breakdown = (item.deadStockData.detailedDeadStock || []).filter(
          (d) => Object.values(d)[0] != null
        );
        const chip = (
          <div className={`stock-chip${num > 0 ? " stock-chip-warning" : ""}`} key="dead">
            <span className="stock-chip-label">Dead Stock</span>
            <span className="stock-chip-value">{num.toLocaleString("en-IN")} {unit}</span>
          </div>
        );
        chips.push(
          breakdown.length > 0 ? (
            <HtmlTooltip
              key="dead"
              placement="top"
              arrow
              title={
                <div style={{ textAlign: "left" }}>
                  <div style={{ fontWeight: "bold", marginBottom: 8, fontSize: 13, color: "#323c47" }}>
                    Dead Stock Breakdown:
                  </div>
                  {breakdown.map((d, idx) => {
                    const name = Object.keys(d)[0];
                    const amt = d[name];
                    const formatted = typeof amt === "number" ? `${amt.toLocaleString("en-IN")} ${unit}` : `${amt} ${unit}`;
                    return (
                      <div key={idx} style={{ marginBottom: 4, fontSize: 12, color: "#666" }}>
                        <span style={{ fontWeight: 500, color: "#323c47" }}>{name}:</span> {formatted}
                      </div>
                    );
                  })}
                </div>
              }
            >
              <span style={{ display: "block", flex: "1 1 0%", minWidth: 0 }}>{chip}</span>
            </HtmlTooltip>
          ) : (
            chip
          )
        );
      }
    }

    if (item.boqData && item.boqData.hasBOQ) {
      const { remaining, totalPlanned, totalConsumed } = item.boqData;
      const remainingVal = typeof remaining === "number" ? remaining : 0;
      const exceeded = remainingVal < 0;
      const chip = (
        <div className={`stock-chip ${exceeded ? "stock-chip-danger" : "stock-chip-success"}`} key="boq">
          <span className="stock-chip-label">BOQ Remaining</span>
          <span className="stock-chip-value">
            {exceeded
              ? `Exceeded by ${Math.abs(remainingVal).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`
              : remainingVal.toLocaleString("en-IN", { maximumFractionDigits: 2 })}{" "}
            {unit}
          </span>
        </div>
      );
      chips.push(
        <HtmlTooltip
          key="boq"
          placement="top"
          arrow
          title={
            <div style={{ textAlign: "left" }}>
              <div style={{ fontWeight: "bold", marginBottom: 6, fontSize: 13, color: "#323c47" }}>BOQ Summary:</div>
              <div style={{ fontSize: 12, color: "#666", marginBottom: 3 }}>
                <span style={{ fontWeight: 500, color: "#323c47" }}>Planned:</span>{" "}
                {(totalPlanned || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })} {unit}
              </div>
              <div style={{ fontSize: 12, color: "#666", marginBottom: 3 }}>
                <span style={{ fontWeight: 500, color: "#323c47" }}>Already Indented:</span>{" "}
                {(totalConsumed || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })} {unit}
              </div>
              <div style={{ fontSize: 12, fontWeight: 600 }}>
                Remaining: {remainingVal.toLocaleString("en-IN", { maximumFractionDigits: 2 })} {unit}
              </div>
            </div>
          }
        >
          <span style={{ display: "block", flex: "1 1 0%", minWidth: 0 }}>{chip}</span>
        </HtmlTooltip>
      );
    } else {
      chips.push(
        <div className="stock-chip" key="boq">
          <span className="stock-chip-label">BOQ Remaining</span>
          <span className="stock-chip-value">No BOQ</span>
        </div>
      );
    }

    if (chips.length === 0) return null;
    return <div className="item-dialog-stock-chips">{chips}</div>;
  }

  /** Small inline flags shown on each list row — BOQ status and lead time only. */
  renderItemRowBadges(item) {
    const badges = [];
    if (item.boqData && item.boqData.hasBOQ) {
      const remaining = typeof item.boqData.remaining === "number" ? item.boqData.remaining : 0;
      const exceeded = remaining < 0;
      badges.push(
        <span key="boq" className={`item-badge ${exceeded ? "item-badge-danger" : "item-badge-success"}`}>
          {exceeded ? "BOQ exceeded" : `BOQ ${remaining.toLocaleString("en-IN", { maximumFractionDigits: 0 })} left`}
        </span>
      );
    }
    if (item.leadTimeDays != null) {
      badges.push(
        <span key="lead" className="item-badge item-badge-neutral">
          ⏱ {item.leadTimeDays}d
        </span>
      );
    }
    return badges;
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
    const keys = Object.keys(this.state.noinventory);
    if (keys.length === 0) {
      this.props.enqueueSnackbar("Add at least one item.", { variant: "error" });
      return;
    }

    // Items are only ever written to noinventory via saveDialogItem, which already
    // enforces product + quantity — this only catches stale/incomplete draft data.
    const invalidKey = keys.find((key) => {
      const item = this.state.noinventory[key];
      const qty = item?.quantity;
      return !item?.productId || !qty || isNaN(Number(qty)) || Number(qty) <= 0;
    });
    if (invalidKey) {
      this.props.enqueueSnackbar("Please complete this item's details before proceeding.", {
        variant: "error",
      });
      this.openEditItemDialog(invalidKey);
      return;
    }

    this.setState({ currentStep: 2 });
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
                  placeholder="Upload Documents"
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

  renderItemRow(key, item) {
    const productName = item?.selectedProduct?.name || "—";
    const productCode = item?.productCode || "";
    const categoryName = item?.selectedCategory?.name || "—";
    const qty = item?.quantity || 0;
    const unit = item?.unit || "";
    const spec = item?.specification || "";
    const remarks = item?.remarks || "";
    const truncate = (text) => (text.length > 28 ? text.slice(0, 26) + "…" : text);
    const badges = this.renderItemRowBadges(item || {});

    return (
      <div
        className="indent-item-row"
        key={key}
        onClick={() => this.openEditItemDialog(key)}
      >
        <div className="item-col item-col-product">
          <div className="item-product-name">{productName}</div>
          {productCode && <div className="item-product-code">{productCode}</div>}
        </div>
        <div className="item-col item-col-category">{categoryName}</div>
        <div className="item-col item-col-qty">
          {qty} {unit}
        </div>
        <div className="item-col item-col-spec" title={spec}>
          {spec ? truncate(spec) : <span className="item-col-empty">—</span>}
        </div>
        <div className="item-col item-col-remarks" title={remarks}>
          {remarks ? truncate(remarks) : <span className="item-col-empty">—</span>}
        </div>
        <div className="item-col item-col-badges">
          {badges.length > 0 ? badges : <span className="item-col-empty">—</span>}
        </div>
        <div className="item-col item-col-actions">
          <IconButton
            size="small"
            aria-label="edit"
            onClick={(e) => {
              e.stopPropagation();
              this.openEditItemDialog(key);
            }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            aria-label="delete"
            onClick={(e) => {
              e.stopPropagation();
              this.deleteItem(key);
            }}
          >
            <img src={trashRedIcon} alt="Delete" className="trash-red-icon" />
          </IconButton>
        </div>
      </div>
    );
  }

  renderItemsSection() {
    const keys = Object.keys(this.state.noinventory).sort((a, b) => Number(a) - Number(b));
    return (
      <div className="indent-items-section">
        <div className="indent-items-header-row">
          <div className="indent-items-title">
            Items{keys.length > 0 ? ` (${keys.length})` : ""}
          </div>
          {keys.length > 0 && (
            <Button
              onClick={this.openAddItemDialog}
              buttonClass="blue"
              label="Add Product"
              startIcon={<AddIcon />}
            />
          )}
        </div>
        {keys.length === 0 ? (
          <div className="indent-items-empty">
            <div className="indent-items-empty-text">No items added yet.</div>
            <Button onClick={this.openAddItemDialog} buttonClass="blue" label="+ Add Product" />
          </div>
        ) : (
          <div className="indent-items-table">
            <div className="indent-items-table-head">
              <div className="item-col item-col-product">Product</div>
              <div className="item-col item-col-category">Category</div>
              <div className="item-col item-col-qty">Qty</div>
              <div className="item-col item-col-spec">Specification</div>
              <div className="item-col item-col-remarks">Remarks</div>
              <div className="item-col item-col-badges">Lead Time</div>
              <div className="item-col item-col-actions" />
            </div>
            {keys.map((key) => this.renderItemRow(key, this.state.noinventory[key]))}
          </div>
        )}
      </div>
    );
  }

  renderItemDialog() {
    const item = this.state.dialogItem || {};
    const showValidation = this.state.dialogValidation;
    const remainingProducts = this.getDialogRemainingProducts();
    const quantityMissing =
      showValidation && (!item.quantity || Number(item.quantity) <= 0);
    const productMissing = showValidation && !item.productId;

    return (
      <Dialog
        open={this.state.itemDialogOpen}
        onClose={this.closeItemDialog}
        maxWidth="sm"
        fullWidth
        aria-labelledby="item-dialog-title"
      >
        <DialogTitle id="item-dialog-title">
          {this.state.dialogEditingKey !== null ? "Edit Item" : "Add Item"}
        </DialogTitle>
        <DialogContent>
          <div className="item-dialog-row">
            {this.renderAutoComplete({
              fieldname: "dialogCategory",
              placeholder: "Category Name",
              options: this.state.categories,
              required: true,
              value: item.selectedCategory || null,
              getOption: (option) => option.name,
              onChange: (e, value) => this.handleDialogCategoryChange(value),
            })}
          </div>
          <div className="item-dialog-row two-col">
            {this.renderAutoComplete({
              fieldname: "dialogProductName",
              placeholder: "Inventory Name",
              options: remainingProducts,
              disableClearable: true,
              required: true,
              value: item.selectedProduct || null,
              getOption: (option) => option["name"],
              onChange: (e, value) => this.handleDialogProductChange(value),
            })}
            {this.renderAutoComplete({
              fieldname: "dialogProductCode",
              placeholder: "Inventory Code",
              options: remainingProducts,
              disableClearable: true,
              required: true,
              value: item.selectedProduct || null,
              getOption: (option) => option["productCode"],
              onChange: (e, value) => this.handleDialogProductChange(value),
            })}
          </div>
          {productMissing && (
            <div className="quantity-error-msg">Please select a product</div>
          )}
          <div className="item-dialog-row two-col">
            <div className={`quantity-field-wrapper${quantityMissing ? " quantity-error" : ""}`}>
              {this.renderTextField({
                fieldname: "dialogQuantity",
                placeholder: "Enter Quantity",
                type: "number",
                required: true,
                skipAdd: true,
                validation: "nonegative",
                value: item.quantity || "",
                onChange: (value) => {
                  this.setState((prev) => ({ dialogItem: { ...prev.dialogItem, quantity: value } }));
                },
              })}
              {quantityMissing && <span className="quantity-error-msg">Quantity is required</span>}
            </div>
            {this.renderTextField({
              fieldname: "dialogUnit",
              placeholder: "Measurement Unit",
              disabled: true,
              skipAdd: true,
              value: item.unit || "",
            })}
          </div>
          {this.renderDialogStockChips(item)}
          <div className="item-dialog-row">
            {this.renderTextField({
              fieldname: "dialogSpecification",
              placeholder: "Enter Specification",
              skipAdd: true,
              validation: "maxlength",
              lengthConstraint: 100,
              errorMessage: "only 100 characters allowed",
              value: item.specification ?? "",
              onChange: (value) =>
                this.setState((prev) => ({ dialogItem: { ...prev.dialogItem, specification: value } })),
            })}
          </div>
          <div className="item-dialog-row">
            {this.renderTextField({
              fieldname: "dialogRemarks",
              placeholder: "Enter Remarks",
              skipAdd: true,
              validation: "maxlength",
              lengthConstraint: 100,
              errorMessage: "only 100 characters allowed",
              value: item.remarks ?? "",
              onChange: (value) =>
                this.setState((prev) => ({ dialogItem: { ...prev.dialogItem, remarks: value } })),
            })}
          </div>
          {item.leadTimeDays != null && (
            <div className="lead-time-chip">
              <span className="lead-time-chip-icon">⏱</span>
              Lead Time: <strong>{item.leadTimeDays} days</strong>
            </div>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={this.closeItemDialog} buttonClass="grey" label="Cancel" />
          <Button
            onClick={this.saveDialogItem}
            buttonClass="blue"
            label={this.state.dialogEditingKey !== null ? "Save Changes" : "Add Item"}
          />
        </DialogActions>
      </Dialog>
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
        {this.renderItemDialog()}
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
              requiredBy={this.formData.requiredBy}
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
                      {this.renderFileArea()}
                    </div>
                  </div>
                  {this.renderItemsSection()}
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