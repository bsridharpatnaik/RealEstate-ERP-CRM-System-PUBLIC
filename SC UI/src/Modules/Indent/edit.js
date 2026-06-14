//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import moment from "moment";
import EditForm from "./../../Shared/EditForm";
import { API } from "./../../axios";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TextField from "@material-ui/core/TextField";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";

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
  rowRefs = {};
  state = {
    value: 0,
    noinventory: {},
    isLoaded: false,
    showValidation: false,
    products: [],
    categories: [],
    productsLoaded: false,
    indentDataLoaded: false,
    fileUploadKey: 0,
    selectedFileName: "",
    selectedFilePreview: null,
    isFileUploading: false,
    currentStep: 1,
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

  clearRowProductSelection(key) {
    const p = { ...this.state.noinventory };
    if (p[key]) {
      p[key] = {
        ...p[key],
        productId: "",
        productCode: "",
        unit: "",
        selectedProduct: null,
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

      if (Object.keys(p).length === 0) {
        p[this.key++] = {};
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

  renderInventory(key) {
    const inventoryItem = this.state.noinventory?.[key];
    if (!inventoryItem) return null;

    const isLineItemLocked = inventoryItem.lineItemStatus &&
      inventoryItem.lineItemStatus.toUpperCase() !== "NEW";

    const currentInventoryId = inventoryItem.productId;
    const selectedInventories = Object.keys(this.state.noinventory)
      .map(index => this?.state?.noinventory?.[index]?.productId)
      .filter(id => id && id !== currentInventoryId);

    const productList = (this.state.noinventory[key]?.products?.length > 0)
      ? this.state.noinventory[key].products
      : (this.state.products.length > 0 ? this.state.products : (this.props.dropdowns?.product || []));
    const remainingInventories = productList.filter(item =>
      !selectedInventories.includes(item.id) || item.id === currentInventoryId
    );

    const inventoryNumber = Number(key);
    const selectedProduct = productList.find(p => p.id === currentInventoryId) || inventoryItem.selectedProduct || null;

    const quantityMissing = this.state.showValidation &&
      (!this.state.noinventory[key]?.quantity || Number(this.state.noinventory[key]?.quantity) <= 0);

    return (
      <div
        className="inventory-item"
        key={`${key}-${this.state.products.length}-${currentInventoryId}`}
        ref={(el) => { this.rowRefs[key] = el; }}
      >
        <div className="inventory-item-header">
          <span>Inventory {inventoryNumber}</span>
          {!isLineItemLocked && (
            <IconButton
              aria-label="delete"
              onClick={() => {
                if (Object.keys(this.state.noinventory).length <= 1) {
                  this.props.enqueueSnackbar("At least one inventory option must be present", {
                    variant: "warning",
                  });
                  return;
                }
                const p = { ...this.state.noinventory };
                delete p[key];
                this.setState({ noinventory: p }, () => {
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              }}
              className="delete-icon"
            >
              <img src={trashRedIcon} alt="Delete" className="trash-red-icon" />
            </IconButton>
          )}
          {isLineItemLocked && (
            <span
              className={`status-badge-table status-${(inventoryItem.lineItemStatus || "").toLowerCase().replace(/\s+/g, "-")}`}
              title={`Line item is ${inventoryItem.lineItemStatus} — cannot be edited`}
              style={{ marginLeft: "auto", alignSelf: "center" }}
            >
              {inventoryItem.lineItemStatus}
            </span>
          )}
        </div>
        <div className="flex">
          <Autocomplete
            disabled={false}
            id={`category-autocomplete-${key}`}
            options={this.state.categories}
            getOptionLabel={(option) => option.name || ""}
            value={this.state.noinventory[key]?.selectedCategory || null}
            onChange={(e, value) => {
              this.fetchProductsByCategoryForRow(key, value);
              if (this.props.onValidationChange) this.props.onValidationChange();
            }}
            disableClearable={false}
            renderInput={(params) => (
              <TextField
                {...params}
                name={`category_${key}`}
                variant="outlined"
                margin="normal"
                label="Category Name"
                required={true}
                InputLabelProps={{ shrink: true }}
              />
            )}
          />
          <Autocomplete
            disabled={false}
            id={`product-autocomplete-${key}`}
            options={remainingInventories}
            getOptionLabel={(option) => option["name"] || ""}
            value={selectedProduct || null}
            onChange={(e, value) => {
              const p = { ...this.state.noinventory };
              if (value) {
                p[key].productId = value.id || "";
                p[key].productCode = value.productCode || "";
                p[key].unit = value.measurementUnit || "";
                p[key].leadTimeDays = value.leadTimeDays ?? null;
                p[key].selectedProduct = value;
                this.setState({ noinventory: p }, () => {
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              } else {
                p[key].productId = "";
                p[key].productCode = "";
                p[key].unit = "";
                p[key].leadTimeDays = null;
                p[key].selectedProduct = null;
                this.setState({ noinventory: p }, () => {
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              }
            }}
            disableClearable={true}
            renderInput={(params) => (
              <TextField
                {...params}
                name="productId"
                variant="outlined"
                margin="normal"
                label="Inventory Name"
                required={true}
                InputLabelProps={{ shrink: true }}
              />
            )}
          />
          <Autocomplete
            disabled={false}
            id={`product-code-autocomplete-${key}`}
            options={remainingInventories}
            getOptionLabel={(option) => option["productCode"] || ""}
            value={selectedProduct || null}
            onChange={(e, value) => {
              const p = { ...this.state.noinventory };
              if (value) {
                p[key].productId = value.id || "";
                p[key].productCode = value.productCode || "";
                p[key].unit = value.measurementUnit || "";
                p[key].leadTimeDays = value.leadTimeDays ?? null;
                p[key].selectedProduct = value;
                this.setState({ noinventory: p }, () => {
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              } else {
                p[key].productId = "";
                p[key].productCode = "";
                p[key].unit = "";
                p[key].leadTimeDays = null;
                p[key].selectedProduct = null;
                this.setState({ noinventory: p }, () => {
                  if (this.props.onValidationChange) {
                    this.props.onValidationChange();
                  }
                });
              }
            }}
            disableClearable={true}
            renderInput={(params) => (
              <TextField
                {...params}
                name="productCode"
                variant="outlined"
                margin="normal"
                label="Inventory Code"
                required={true}
                InputLabelProps={{ shrink: true }}
              />
            )}
          />
        </div>
        <div className="flex">
          <div className={`quantity-field-wrapper${quantityMissing ? " quantity-error" : ""}`}>
            {this.renderTextField({
              fieldname: "quantity",
              placeholder: "Enter Quantity",
              type: "number",
              required: true,
              disabled: isLineItemLocked,
              data: this.state.noinventory[key],
              skipAdd: true,
              validation: "nonegative",
              value: this.state.noinventory[key]?.quantity || "",
              onChange: (value) => {
                if (isLineItemLocked) return;
                const p = this.state.noinventory;
                p[key].quantity = value;
                this.setState({ noinventory: { ...p } }, () => {
                  if (this.props.onValidationChange) this.props.onValidationChange();
                });
              },
            })}
            {quantityMissing && (
              <span className="quantity-error-msg">Quantity is required</span>
            )}
          </div>
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
            data: this.state.noinventory[key],
            skipAdd: true,
            validation: "maxlength",
            lengthConstraint: 100,
            errorMessage: "only 100 characters allowed",
            value: this.state.noinventory[key]?.specification || "",
            onChange: (value) => {
              const p = this.state.noinventory;
              p[key].specification = value;
              this.setState({ noinventory: { ...p } });
            },
          })}
          {this.renderTextField({
            fieldname: "remarks",
            placeholder: "Enter Remarks",
            data: this.state.noinventory[key],
            skipAdd: true,
            validation: "maxlength",
            lengthConstraint: 100,
            errorMessage: "only 100 characters allowed",
            value: this.state.noinventory[key]?.remarks || "",
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
      fileInformations: this.formData.fileInformations || [],
      inventoryList: inventoryList,
    };

    this.setState({ isUpdating: true });
    const response = await API.PUT(this.updateUrl, params);
    this.showToaster(response);
    this.setState({ isUpdating: false });
  }

  submitForm = () => {
    // Find first inventory key with missing quantity
    const firstInvalidKey = Object.keys(this.state.noinventory).find(key => {
      const item = this.state.noinventory[key];
      const qty = item?.quantity;
      return !qty || isNaN(Number(qty)) || Number(qty) <= 0;
    });

    if (firstInvalidKey) {
      this.setState({ showValidation: true }, () => {
        const ref = this.rowRefs[firstInvalidKey];
        if (ref) {
          ref.scrollIntoView({ behavior: "smooth", block: "center" });
        }
      });
      return;
    }

    // Validation passed — advance to review step
    this.setState({ showValidation: false, currentStep: 2 });
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
        <div className="upload-documents-heading">Upload Documents</div>
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

  render() {
    const { currentStep } = this.state;
    return (
      <div className="list-section add create-po-wrapper">
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
              isSaving={this.state.isUpdating}
              onBack={() => this.setState({ currentStep: 1 })}
              onConfirm={this.handleConfirmSave}
            />
          ) : (
            this.state.isLoaded && (
              <form onSubmit={(e) => this.update(e)}>
                <div className="flex">
                  <div className="indent-header-fields">
                    {this.renderDate({
                      fieldname: "indentDate",
                      label: "Indent Date",
                      required: true,
                    })}
                  </div>
                </div>
                {this.renderInventoryAddButton()}
                <div className="inventories-list">
                  {Object.keys(this.state.noinventory).length > 0 ? (
                    Object.keys(this.state.noinventory).sort((a, b) => Number(b) - Number(a)).map((key) =>
                      this.renderInventory(key)
                    )
                  ) : (
                    <div>No inventory items found</div>
                  )}
                </div>
                {this.renderFileArea()}
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