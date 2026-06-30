//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
import { API } from "./../../axios";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TextField from "@material-ui/core/TextField";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import EditIcon from "@material-ui/icons/Edit";
import AddIcon from "@material-ui/icons/Add";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import trashRedIcon from "./../../Shared/Icons/trash-red.png";
//style
import "./style.scss";
import "../PurchaseOrder/add/stepStyles.scss";
import Step2ReviewIndent from "./step2ReviewIndent";
import { fetchUnit } from "./../../actions/measurementUnit";
//misc
import Button from "./../../Shared/Button";
import CircularProgress from "@material-ui/core/CircularProgress";

class Edit extends EditForm {
  title = messages.common.indent;
  updateUrl = apiEndpoints.updateIndent;
  state = {
    value: 0,
    noinventory: {},
    isLoaded: false,
    products: [],
    categories: [],
    productsLoaded: false,
    indentDataLoaded: false,
    fileUploadKey: 0,
    selectedFileName: "",
    selectedFilePreview: null,
    isFileUploading: false,
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

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    const { dispatch } = this.props;
    dispatch(fetchUnit());
    this.fetchCategories();
    Promise.all([this.fetchProducts(), this.search()]);
  }

  componentWillUnmount() {
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

  async fetchProducts() {
    const response = await API.GET(apiEndpoints.getProductForIndentWithManagedInventory());
    if (response.success && Array.isArray(response.data)) {
      const transformedProducts = response.data.map((product) => ({
        id: product.productId,
        name: product.productName,
        measurementUnit: product.measurementUnit,
        productCode: product.productCode,
        isManagedInventory: product.isManagedInventory,
        leadTimeDays: product.leadTimeDays ?? null,
      }));
      this.setState({
        products: transformedProducts,
        productsLoaded: true
      });
      this.checkIfReadyToRender();
      return transformedProducts;
    }
    this.setState({ productsLoaded: true });
    this.checkIfReadyToRender();
    return [];
  }

  async search() {
    const response = await API.GET(apiEndpoints.getIndentDetail + this.props.id);

    if (response.success) {
      const data = response.data;

      this.formData.indentDate = data.indentDate || data.dateCreation || "";
      this.formData.requiredBy = data.requiredBy;
      this.formData.fileInformations = data.fileInformations || [];

      const inventoryItems = data.inventoryItems || data.inventoryList || [];
      const p = {};
      this.key = 1;

      for (let i = 0; i < inventoryItems.length; i++) {
        const item = inventoryItems[i];
        const pid = item.product?.productId || item.productId || item.product?.id;
        const unit = item.product?.measurementUnit || item.measurementUnit || item.unit || "";
        const productCode = item.product?.productCode || item.productCode || "";

        if (pid) {
          const category = item.product?.category;
          const selectedCategory = category
            ? { id: category.categoryId, name: category.categoryName }
            : null;
          p[this.key++] = {
            quantity: item.quantity || 0,
            productId: pid,
            productCode: productCode,
            unit: unit,
            leadTimeDays: item.leadTimeDays ?? null,
            specification: item.specification || "",
            remarks: item.remarks || "",
            lineItemStatus: item.lineItemStatus || "NEW",
            lineItemCode: item.lineItemCode || null,
            selectedCategory,
            selectedProduct: {
              id: pid,
              name: item.product?.productName || "",
              measurementUnit: unit,
              productCode: productCode,
              leadTimeDays: item.leadTimeDays ?? null,
            },
          };
        }
      }

      this.setState({
        indentDataLoaded: true,
        noinventory: p,
      });
      this.checkIfReadyToRender();
    } else {
      this.setState({ indentDataLoaded: true });
      this.checkIfReadyToRender();
    }
  }

  checkIfReadyToRender() {
    if (this.state.productsLoaded && this.state.indentDataLoaded) {
      this.setState({ isLoaded: true });
    }
  }

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
    const productList = (item.products && item.products.length > 0)
      ? item.products
      : (this.state.products.length > 0 ? this.state.products : (this.props.dropdowns?.product || []));
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

  handleDialogProductChange = (value) => {
    this.setState((prev) => ({
      dialogItem: {
        ...prev.dialogItem,
        productId: value?.id || "",
        productCode: value?.productCode || "",
        unit: value?.measurementUnit || "",
        leadTimeDays: value?.leadTimeDays ?? null,
        selectedProduct: value || null,
      },
    }));
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
    const item = this.state.noinventory[key];
    const isLineItemLocked = item?.lineItemStatus && item.lineItemStatus.toUpperCase() !== "NEW";
    if (isLineItemLocked) {
      this.props.enqueueSnackbar(
        `Line item is ${item.lineItemStatus} — cannot be deleted`,
        { variant: "warning" }
      );
      return;
    }
    const p = { ...this.state.noinventory };
    delete p[key];
    this.setState({ noinventory: p }, () => {
      if (this.props.onValidationChange) {
        this.props.onValidationChange();
      }
    });
  };

  /** Small inline flags shown on each list row — lead time only (no BOQ/stock data in edit). */
  renderItemRowBadges(item) {
    const badges = [];
    if (item.leadTimeDays != null) {
      badges.push(
        <span key="lead" className="item-badge item-badge-neutral">
          ⏱ {item.leadTimeDays}d
        </span>
      );
    }
    return badges;
  }

  renderItemRow(key, item) {
    const isLineItemLocked = item?.lineItemStatus && item.lineItemStatus.toUpperCase() !== "NEW";
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
          {isLineItemLocked ? (
            <span
              className={`status-badge-table status-${(item.lineItemStatus || "").toLowerCase().replace(/\s+/g, "-")}`}
              title={`Line item is ${item.lineItemStatus} — cannot be edited`}
            >
              {item.lineItemStatus}
            </span>
          ) : badges.length > 0 ? (
            badges
          ) : (
            <span className="item-col-empty">—</span>
          )}
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
          {!isLineItemLocked && (
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
          )}
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
              <div className="item-col item-col-badges">Status / Lead Time</div>
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
    const isLineItemLocked = item.lineItemStatus && item.lineItemStatus.toUpperCase() !== "NEW";
    const quantityMissing = showValidation && (!item.quantity || Number(item.quantity) <= 0);
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
          {isLineItemLocked && (
            <span
              className={`status-badge-table status-${(item.lineItemStatus || "").toLowerCase().replace(/\s+/g, "-")}`}
              style={{ marginLeft: 12 }}
            >
              {item.lineItemStatus}
            </span>
          )}
        </DialogTitle>
        <DialogContent>
          <div className="item-dialog-row">
            <Autocomplete
              id="dialog-category-autocomplete"
              options={this.state.categories}
              getOptionLabel={(option) => option.name || ""}
              value={item.selectedCategory || null}
              onChange={(e, value) => this.handleDialogCategoryChange(value)}
              disableClearable={false}
              renderInput={(params) => (
                <TextField
                  {...params}
                  name="dialogCategory"
                  variant="outlined"
                  margin="normal"
                  label="Category Name"
                  required
                  InputLabelProps={{ shrink: true }}
                />
              )}
            />
          </div>
          <div className="item-dialog-row two-col">
            <Autocomplete
              id="dialog-product-autocomplete"
              options={remainingProducts}
              getOptionLabel={(option) => option["name"] || ""}
              value={item.selectedProduct || null}
              onChange={(e, value) => this.handleDialogProductChange(value)}
              disableClearable
              renderInput={(params) => (
                <TextField
                  {...params}
                  name="dialogProductName"
                  variant="outlined"
                  margin="normal"
                  label="Inventory Name"
                  required
                  InputLabelProps={{ shrink: true }}
                />
              )}
            />
            <Autocomplete
              id="dialog-product-code-autocomplete"
              options={remainingProducts}
              getOptionLabel={(option) => option["productCode"] || ""}
              value={item.selectedProduct || null}
              onChange={(e, value) => this.handleDialogProductChange(value)}
              disableClearable
              renderInput={(params) => (
                <TextField
                  {...params}
                  name="dialogProductCode"
                  variant="outlined"
                  margin="normal"
                  label="Inventory Code"
                  required
                  InputLabelProps={{ shrink: true }}
                />
              )}
            />
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
                disabled: isLineItemLocked,
                skipAdd: true,
                validation: "nonegative",
                value: item.quantity || "",
                onChange: (value) => {
                  if (isLineItemLocked) return;
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

  async update(event) {
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
      indentDate: this.formData.indentDate || "",
      requiredBy: this.formData.requiredBy,
      fileInformations: this.formData.fileInformations || [],
      inventoryList: inventoryList,
    };

    this.setState({ isUpdating: true });
    const response = await API.PUT(this.updateUrl, params);
    this.showToaster(response);
    this.setState({ isUpdating: false });
  }

  submitForm = () => {
    const keys = Object.keys(this.state.noinventory);
    if (keys.length === 0) {
      this.props.enqueueSnackbar("Add at least one item.", { variant: "error" });
      return;
    }

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
    this.update(syntheticEvent);
  }

  getSaveButtonDisabled = () => {
    return (
      this.state.isUpdating ||
      this.state.isFileUploading ||
      Object.keys(this.formValidation).length > 0
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

    const validTypes = ["image/jpeg", "image/jpg", "image/png"];
    if (!validTypes.includes(file.type)) {
      this.props.enqueueSnackbar("Only JPG and PNG files are allowed", {
        variant: "error",
      });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) {
        this.fileInputRef.current.value = "";
      }
      return;
    }

    const previewUrl = URL.createObjectURL(file);
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
                id="indent-file-upload-edit"
                accept="image/jpeg,image/jpg,image/png"
                onChange={(e) => this.handleFileSelect(e)}
                style={{ display: "none" }}
              />
              <label
                htmlFor="indent-file-upload-edit"
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
                  </div>
                ))}
              </div>
            </div>
            <div className="upload-hint">
              Upload a file here, Max 2 MB allow (JPG, PNG only)
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderBreadcrumbs() {
    const { currentStep } = this.state;
    const steps = [
      messages.common.indent,
      messages.common.update + " " + messages.common.indent,
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
            disabled={this.state.isUpdating}
          />
          <Button
            onClick={this.handleConfirmSave}
            buttonClass="blue"
            label={this.state.isUpdating ? "Saving..." : "Confirm & Save"}
            disabled={this.state.isUpdating}
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
          onClick={() => this.submitForm()}
          buttonClass="blue"
          label="Review"
          disabled={this.getSaveButtonDisabled()}
        />
      </div>
    );
  }

  renderEditHeading() {
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
          {messages.common.update} {messages.common.indent}
        </span>
      </div>
    );
  }

  render() {
    const { currentStep } = this.state;
    return (
      <div className="list-section add create-po-wrapper">
        {this.renderItemDialog()}
        <div className="create-po-header">
          <div className="create-po-header-row">
            {this.renderEditHeading()}
            {this.renderHeaderActions()}
          </div>
          {this.renderBreadcrumbs()}
        </div>
        <div className="create-po-content">
          {currentStep === 2 ? (
            <Step2ReviewIndent
              noinventory={this.state.noinventory}
              fileInformations={this.formData.fileInformations || []}
              indentDate={this.formData.indentDate || ""}
              requiredBy={this.formData.requiredBy}
              isSaving={this.state.isUpdating}
              onBack={() => this.setState({ currentStep: 1 })}
              onConfirm={this.handleConfirmSave}
            />
          ) : (
            this.state.isLoaded && (
              <form onSubmit={(e) => this.update(e)}>
                <div className="indent-header-card">
                  <div className="header-required-by header-required-by-group">
                    {this.renderDate({
                      fieldname: "indentDate",
                      label: "Indent Date",
                      required: true,
                    })}
                    {this.renderAutoComplete({
                      fieldname: "requiredBy",
                      placeholder: "Required By",
                      options: this.props.dropdowns?.requiredByOptions || [],
                      freeSolo: true,
                      helperText: "Type to search existing, or enter a new name",
                      getOption: (option) =>
                        typeof option === "string" ? option : option["name"] || "",
                    })}
                  </div>
                  <div className="header-divider" />
                  <div className="header-upload-documents">
                    {this.renderFileArea()}
                  </div>
                </div>
                {this.renderItemsSection()}
              </form>
            )
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
  withSnackbar(Edit)
);
