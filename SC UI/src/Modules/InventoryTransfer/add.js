//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
import { API } from "./../../axios";
import InventoryTransferConfirm from "./../../Shared/InventoryTransferConfirm";
import InventoryTransferResultModal from "./../../Shared/InventoryTransferResultModal";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import moment from "moment";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import Button from "./../../Shared/Button";
import trashRedIcon from "./../../Shared/Icons/trash-red.png";
//style
import "./style.scss";
import "../PurchaseOrder/add/stepStyles.scss";
import { fetchUnit } from "./../../actions/measurementUnit";
import { instance } from "./../../axios";
import { getToken } from "./../../helper";
import axios from "axios";

class Add extends AddForm {
  title = messages.common.inventoryTransfer;
  addurl = apiEndpoints.createInventoryTransfer;
  state = {
    isAdding: false,
    dropdowns: {},
    products: {},
    selectedFromWarehouse: null,
    selectedToWarehouse: null,
    showConfirmDialog: false,
    transferData: null,
    showResultModal: false,
    resultModalData: null,
    stockValidationErrors: {}, // Store stock validation errors by product key
    currentStocks: {}, // Store current stock values by product key
  };
  _isMounted = false;
  key = 1;

  componentDidMount() {
    this._isMounted = true;
    const { dispatch } = this.props;
    dispatch(fetchUnit());
    this.fetchDropdowns();

    // Inventory transfer should always use today's date (hide date selection)
    this.formData.transferDate = moment().format(this.dateFormat);

    // Initialize with one empty product
    this.setState({
      products: {
        1: {}
      }
    });
  }

  componentWillUnmount() {
    this._isMounted = false;
  }

  async fetchDropdowns() {
    // Fetch dropdown options for projects and warehouses
    const warehouseResponse = await API.GET(apiEndpoints.getWarehouse);
    if (warehouseResponse.success && this._isMounted) {
      this.setState({
        dropdowns: {
          ...this.state.dropdowns,
          warehouse: warehouseResponse.data || [],
        },
      });
    }

    // Fetch projects/tenants - only show those with inventory: true
    const projectsResponse = await API.GET(apiEndpoints.getTenants);
    if (projectsResponse.success && this._isMounted) {
      // Filter to only show tenants with inventory enabled
      const inventoryTenants = (projectsResponse.data || []).filter(
        (tenant) => tenant.inventory === true
      );
      this.setState({
        dropdowns: {
          ...this.state.dropdowns,
          projects: inventoryTenants,
        },
      });
    }

    // Don't fetch products here - wait for "From Project" selection
    // Products will be fetched when "From Project" is selected
  }

  async fetchProductsForTenant(tenantCode) {
    if (!tenantCode) {
      return;
    }

    try {
      // Create a temporary axios instance without interceptor to use custom tenant-id
      const token = getToken();
      const customAxios = axios.create({
        baseURL: process.env.REACT_APP_BASE_URL,
      });

      // For inventory transfer, we need all products (both managed and unmanaged inventory)
      // Don't send isManagedInventory parameter to get both true and false results
      const response = await customAxios.get(apiEndpoints.getProductForIndentWithManagedInventory(), {
        headers: {
          Authorization: `Bearer ${token}`,
          "tenant-id": tenantCode,
        },
      });

      if (response.data && Array.isArray(response.data) && this._isMounted) {
        // Transform the API response to match component expectations (same as indent)
        const transformedProducts = response.data.map((product) => ({
          id: product.productId,
          name: product.productName,
          measurementUnit: product.measurementUnit,
          productCode: product.productCode,
          isManagedInventory: product.isManagedInventory,
        }));
        // productCodes for Product Code dropdown: { id, name } where name = productCode
        const productCodes = transformedProducts
          .filter(p => p.productCode)
          .map(p => ({ id: p.id, name: p.productCode }));
        this.setState({
          dropdowns: {
            ...this.state.dropdowns,
            product: transformedProducts,
            productCodes,
          },
        });
      }
    } catch (error) {
      if (this._isMounted) {
        this.props.enqueueSnackbar(
          error?.response?.data?.message || "Failed to fetch products",
          { variant: "error" }
        );
      }
    }
  }

  async validateStockForProduct(key, productId, enteredQuantity = null) {
    if (!productId || !this.formData.fromWarehouseId) {
      return;
    }

    try {
      const token = getToken();
      const customAxios = axios.create({
        baseURL: process.env.REACT_APP_BASE_URL,
      });
      const store = require("./../../index").store;
      const state = store.getState();

      const response = await customAxios.get(
        apiEndpoints.getInventoryTransferCurrentStock + `tenant=${state.tennant.tennant_id}&productId=${productId}&warehouseId=${this.formData.fromWarehouseId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 200 && response.data !== undefined && response.data !== null) {
        const currentStock = parseFloat(response.data.currentStock) || 0;
        const product = this.state.products[key];
        const productName = product?.productName || "Product";

        // Store current stock for this product
        const currentStocks = { ...(this.state.currentStocks || {}) };
        currentStocks[key] = currentStock;
        this.setState({ currentStocks });

        // If quantity is provided, validate it
        if (enteredQuantity !== null && enteredQuantity > 0) {
          if (enteredQuantity > currentStock) {
            // Block submission - show inline validation error
            const errors = { ...this.state.stockValidationErrors };
            errors[key] = `Current stock for inventory ${productName} is ${currentStock}. Please enter a quantity less than the current stock.`;
            this.setState({ stockValidationErrors: errors });
          } else {
            // Clear validation error
            const errors = { ...this.state.stockValidationErrors };
            delete errors[key];
            this.setState({ stockValidationErrors: errors });
          }
        }
      }
    } catch (error) {
      // Silently fail stock validation - don't block user if API call fails
      console.error("Failed to validate stock:", error);
    }
  }

  async fetchWarehousesForTenant(tenantCode, type) {
    if (!tenantCode) {
      return;
    }

    try {
      // Create a temporary axios instance without interceptor to use custom tenant-id
      const token = getToken();
      const customAxios = axios.create({
        baseURL: process.env.REACT_APP_BASE_URL,
      });

      const response = await customAxios.get(apiEndpoints.getWarehouse, {
        headers: {
          Authorization: `Bearer ${token}`,
          "tenant-id": tenantCode,
        },
      });

      if (response.data && Array.isArray(response.data) && this._isMounted) {
        // Update warehouse dropdowns based on type (fromWarehouse or toWarehouse)
        const warehouseKey = type === 'from' ? 'fromWarehouse' : 'toWarehouse';
        this.setState({
          dropdowns: {
            ...this.state.dropdowns,
            [warehouseKey]: response.data,
          },
        });
      }
    } catch (error) {
      if (this._isMounted) {
        this.props.enqueueSnackbar(
          error?.response?.data?.message || "Failed to fetch warehouses",
          { variant: "error" }
        );
      }
    }
  }

  renderProduct(key) {
    const currentProductId = this.state.products?.[key]?.productId;
    const selectedProducts = Object.keys(this.state.products).map(index => this?.state?.products?.[index]?.productId);
    // Use products from state dropdowns, fallback to props.dropdowns if available
    const productList = this.state.dropdowns.product || this.props.dropdowns?.product || [];
    const productCodeList = this.state.dropdowns.productCodes || this.props.dropdowns?.productCodes || [];
    const remainingProducts = productList.filter(item => (!selectedProducts.includes(item.id) || currentProductId === item.id));
    const remainingProductCodes = productCodeList.filter(item => (!selectedProducts.includes(item.id) || currentProductId === item.id));
    const productKeys = Object.keys(this.state.products).sort((a, b) => Number(a) - Number(b));
    const productNumber = productKeys.indexOf(key.toString()) + 1;

    return (
      <div className="inventory-item" key={key}>
        <div className="inventory-item-header">
          <span>Product {productNumber}</span>
          <IconButton
            aria-label="delete"
            onClick={() => {
              // Prevent deletion if there's only one product
              if (Object.keys(this.state.products).length <= 1) {
                this.props.enqueueSnackbar("At least one product must be present", {
                  variant: "warning",
                });
                return;
              }
              const p = this.state.products;
              delete p[key];
              this.setState({ products: { ...p } });
            }}
            className="delete-icon"
          >
            <img src={trashRedIcon} alt="Delete" className="trash-red-icon" />
          </IconButton>
        </div>
        <div className="flex">
          {this.renderAutoComplete({
            fieldname: "productId",
            placeholder: "Product Name",
            options: remainingProducts,
            disableClearable: true,
            required: true,
            value: this.state.products[key]?.selectedProduct || null,
            getOption: (option) => {
              return option["name"] || "";
            },
            onChange: async (e, value) => {
              const p = this.state.products;
              p[key].productId = value?.id || "";
              p[key].productName = value?.name || "";
              p[key].productCode = value?.productCode || "";
              p[key].measurementUnit = value?.measurementUnit || "";
              p[key].selectedProduct = value; // Store the full product object
              
              // Clear validation error for this product if product changes
              const errors = { ...this.state.stockValidationErrors };
              delete errors[key];
              
              if (value) {
                this.setState({ products: { ...p }, stockValidationErrors: errors });
                // Fetch current stock immediately when product is selected (if warehouse is also selected)
                if (this.formData.fromWarehouseId) {
                  const quantity = p[key].quantity ? parseFloat(p[key].quantity) : null;
                  await this.validateStockForProduct(key, value.id, quantity);
                }
              }
            },
          })}
          {this.renderAutoComplete({
            fieldname: "productCode",
            placeholder: "Product Code",
            options: remainingProductCodes,
            disableClearable: true,
            required: true,
            value: productCodeList.find(pc => pc.id === currentProductId) || null,
            getOption: (option) => {
              return option["name"] || "";
            },
            onChange: async (e, value) => {
              const p = this.state.products;
              // Resolve full product from productList by id (productCodes have id = productId)
              const fullProduct = value ? productList.find(pr => pr.id === value.id) : null;
              if (value) {
                p[key].productId = value.id || "";
                p[key].productName = fullProduct?.name || "";
                p[key].productCode = value.name || "";
                p[key].measurementUnit = fullProduct?.measurementUnit || "";
                p[key].selectedProduct = fullProduct || { id: value.id, name: value.name, productCode: value.name, measurementUnit: "" };
              } else {
                p[key].productId = "";
                p[key].productName = "";
                p[key].productCode = "";
                p[key].measurementUnit = "";
                p[key].selectedProduct = null;
              }
              
              // Clear validation error for this product if product changes
              const errors = { ...this.state.stockValidationErrors };
              delete errors[key];
              
              if (value) {
                this.setState({ products: { ...p }, stockValidationErrors: errors });
                // Fetch current stock immediately when product is selected (if warehouse is also selected)
                if (this.formData.fromWarehouseId) {
                  const quantity = p[key].quantity ? parseFloat(p[key].quantity) : null;
                  await this.validateStockForProduct(key, value.id, quantity);
                }
              }
            },
          })}
          {this.renderTextField({
            fieldname: "quantity",
            placeholder: "Qty",
            type: "number",
            required: true,
            skipAdd: true,
            validation: "nonegative",
            disabled: (this.state.currentStocks[key] === 0),
            error: !!this.state.stockValidationErrors[key],
            helperText: this.state.stockValidationErrors[key] || (this.state.currentStocks[key] === 0 ? `${this.state.products[key]?.productName || "This product"} has zero stock in source warehouse. Cannot be added for transfer` : ""),
            onChange: async (value) => {
              const p = this.state.products;
              p[key].quantity = value;
              this.setState({ products: { ...p } });

              // Validate stock when quantity is entered
              if (value && p[key].productId && this.formData.fromWarehouseId) {
                await this.validateStockForProduct(key, p[key].productId, parseFloat(value) || 0);
              } else {
                // Clear validation error if quantity is empty or product/warehouse not selected
                const errors = { ...this.state.stockValidationErrors };
                delete errors[key];
                this.setState({ stockValidationErrors: errors });
              }
            },
          })}
          {this.renderTextField({
            fieldname: "measurementUnit",
            placeholder: "Unit",
            disabled: true,
            value: this.state.products[key]?.measurementUnit || "",
          })}
        </div>
      </div>
    );
  }

  async add(event) {
    event.preventDefault();

    // Always send current date in payload (ignore any previously set value)
    this.formData.transferDate = moment().format(this.dateFormat);

    if (!this.formData.fromProjectId) {
      this.props.enqueueSnackbar("Select From Project", {
        variant: "error",
      });
      return;
    }

    if (!this.formData.fromWarehouseId) {
      this.props.enqueueSnackbar("Select From Warehouse", {
        variant: "error",
      });
      return;
    }

    if (!this.formData.toProjectId) {
      this.props.enqueueSnackbar("Select To Project", {
        variant: "error",
      });
      return;
    }

    if (!this.formData.toWarehouseId) {
      this.props.enqueueSnackbar("Select To Warehouse", {
        variant: "error",
      });
      return;
    }

    // Check for stock validation errors
    const validationErrors = Object.keys(this.state.stockValidationErrors);
    if (validationErrors.length > 0) {
      this.props.enqueueSnackbar("Please fix stock validation errors before submitting", {
        variant: "error",
      });
      return;
    }

    // Validate all products
    const products = Object.values(this.state.products);
    if (products.length === 0) {
      this.props.enqueueSnackbar("Add at least one product", {
        variant: "error",
      });
      return;
    }

    for (let i = 0; i < products.length; i++) {
      const product = products[i];
      if (!product.productId) {
        this.props.enqueueSnackbar(`Select inventory for Product ${i + 1}`, {
          variant: "error",
        });
        return;
      }
      if (!product.quantity || product.quantity.trim() === "") {
        this.props.enqueueSnackbar(`Enter quantity for Product ${i + 1}`, {
          variant: "error",
        });
        return;
      }
    }

    // Transform products data to match API payload structure
    const items = Object.values(this.state.products).map((product) => ({
      productId: product.productId,
      quantity: parseFloat(product.quantity) || 0,
    }));

    // Create product lookup for confirmation dialog
    const productLookup = {};
    Object.values(this.state.products).forEach((product) => {
      productLookup[product.productId] = product;
    });

    // Prepare transfer data for confirmation dialog
    const transferData = {
      transferDate: moment().format(this.dateFormat),
      sourceTenant: this.formData.fromProjectId,
      targetTenant: this.formData.toProjectId,
      sourceWarehouseId: this.formData.fromWarehouseId,
      targetWarehouseId: this.formData.toWarehouseId,
      remarks: this.formData.remarks || "",
      items: items,
      fromProjectName: this.formData.fromProjectName,
      fromWarehouseName: this.formData.fromWarehouseName,
      toProjectName: this.formData.toProjectName,
      toWarehouseName: this.formData.toWarehouseName,
      products: productLookup,
    };

    // Show confirmation dialog instead of directly submitting
    this.setState({
      showConfirmDialog: true,
      transferData: transferData,
    });
  }

  async submitTransfer() {
    this.setState({ isAdding: true, showConfirmDialog: false });

    const { transferData } = this.state;
    const params = {
      transferDate: moment().format(this.dateFormat),
      sourceTenant: transferData.sourceTenant,
      targetTenant: transferData.targetTenant,
      sourceWarehouseId: transferData.sourceWarehouseId,
      targetWarehouseId: transferData.targetWarehouseId,
      remarks: transferData.remarks,
      items: transferData.items,
    };

    try {
      // Use raw axios to get HTTP status codes
      const token = getToken();
      const customAxios = axios.create({
        baseURL: process.env.REACT_APP_BASE_URL,
      });
      const store = require("./../../index").store;
      const state = store.getState();
      
      const response = await customAxios.post(this.addurl, params, {
        headers: {
          Authorization: `Bearer ${token}`,
          ...(state.tennant.tennant_id ? { "tenant-id": state.tennant.tennant_id } : {}),
        },
      });

      // Handle status 201
      if (response.status === 201) {
        this.handleResponseStatus201(response.data);
      } else {
        // Other success statuses (200, etc.)
        this.handleResponseStatus201(response.data);
      }
    } catch (error) {
      // Handle error responses
      if (error.response && error.response.status === 500) {
        // Status 500 - complete failure
        const errorMessage = error.response?.data?.message || "Internal server error occurred";
        this.props.enqueueSnackbar(errorMessage, {
          variant: "error",
        });
      } else {
        // Other error statuses
        const errorMessage = error.response?.data?.message || "Transfer failed";
        this.props.enqueueSnackbar(errorMessage, {
          variant: "error",
        });
      }
    } finally {
      this.setState({ isAdding: false, transferData: null });
    }
  }

  handleResponseStatus201(responseData) {
    // Parse the response body
    if (!responseData) {
      this.props.enqueueSnackbar("Invalid response received", {
        variant: "error",
      });
      return;
    }

    if (responseData.fullySuccessful === true) {
      // All items transferred successfully
      this.props.enqueueSnackbar("Inventory transfer completed successfully", {
        variant: "success",
      });
      this.props.back();
    } else if (responseData.fullySuccessful === false) {
      // Partial success - show modal with item results
      if (responseData.itemResults && Array.isArray(responseData.itemResults)) {
        this.setState({
          showResultModal: true,
          resultModalData: {
            itemResults: responseData.itemResults,
            productLookup: this.state.transferData?.products || {},
          },
        });
      } else {
        this.props.enqueueSnackbar("Transfer completed with some issues", {
          variant: "warning",
        });
      }
    } else {
      // Fallback for responses without fullySuccessful flag
      this.props.enqueueSnackbar(this.title + " added successfully", {
        variant: "success",
      });
      this.props.back();
    }
  }

  handleResultModalClose = () => {
    this.setState({ showResultModal: false, resultModalData: null });
    // Don't navigate back - let user review results and close manually
  }

  handleConfirmCancel = () => {
    this.setState({ showConfirmDialog: false, transferData: null });
  }

  handleConfirmSubmit = () => {
    this.submitTransfer();
  }

  renderBreadcrumbs() {
    const steps = [messages.common.inventoryTransfer, messages.common.add + " " + messages.common.inventoryTransfer];
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
    return (
      <div className="po-action-buttons">
        <Button
          onClick={this.props.back}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          onClick={(e) => this.add(e)}
          buttonClass="blue"
          label={messages.common.save}
          disabled={this.state.isAdding}
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
          {messages.common.add} {messages.common.inventoryTransfer}
        </span>
      </div>
    );
  }

  renderProductAddButton() {
    return (
      <div className="add-inventory-button-wrapper">
        <Button
          onClick={() => {
            const newKey = ++this.key;
            const p = this.state.products;
            p[newKey] = {};
            this.setState({ products: { ...p } });
          }}
          buttonClass="grey"
          label="+ Add Product"
          disabled={Object.keys(this.state.products).length >= 5}
        />
      </div>
    );
  }

  render() {
    return (
      <div className="list-section add create-po-wrapper">
        <div className="create-po-header">
          <div className="create-po-header-row">
            {this.renderAddHeading()}
            {this.renderHeaderActions()}
          </div>
          {this.renderBreadcrumbs()}
        </div>
        <div className="create-po-content">
        <form onSubmit={(e) => this.add(e)}>
          <div className="form-section">
            <h3 className="section-title">From</h3>
            <div className="flex">
              {this.renderAutoComplete({
                fieldname: "fromProjectId",
                placeholder: "Project",
                options: this.state.dropdowns.projects || this.props.dropdowns?.projects || [],
                disableClearable: true,
                required: true,
                getOption: (option) => {
                  return option["name"] || option["tenantName"] || "";
                },
                onChange: (e, value) => {
                  if (value) {
                    const tenantCode = value.tenantCode || value.id || "";
                    this.formData.fromProjectId = tenantCode;
                    this.formData.fromProjectName = value.name || value.tenantName || "";
                    // Fetch products for the selected tenant
                    this.fetchProductsForTenant(tenantCode);
                    // Fetch warehouses for the selected tenant
                    this.fetchWarehousesForTenant(tenantCode, 'from');
                    // Clear selected warehouse when project changes
                    this.formData.fromWarehouseId = "";
                    this.formData.fromWarehouseName = "";
                    // Clear products, stock data, and validation errors when tenant changes
                    this.setState({
                      selectedFromWarehouse: null,
                      selectedProduct: null,
                      products: { 1: {} }, // Reset to single empty product
                      currentStocks: {}, // Clear all stock data
                      stockValidationErrors: {} // Clear all validation errors
                    });
                  }
                },
              })}
              {this.renderAutoComplete({
                fieldname: "fromWarehouseId",
                placeholder: "Ware House",
                options: this.state.dropdowns.fromWarehouse || this.props.dropdowns?.warehouse || [],
                value: this.state.selectedFromWarehouse,
                disableClearable: true,
                required: true,
                getOption: (option) => {
                  return option["name"] || "";
                },
                onChange: async (e, value) => {
                  if (value) {
                    this.formData.fromWarehouseId = value.id || "";
                    this.formData.fromWarehouseName = value.name || "";
                    this.setState({ 
                      selectedFromWarehouse: value,
                      stockValidationErrors: {} // Clear stock validation errors when warehouse changes
                    });
                    // Fetch stock for all products when warehouse changes
                    const products = this.state.products;
                    for (const [key, product] of Object.entries(products)) {
                      if (product.productId) {
                        const quantity = product.quantity ? parseFloat(product.quantity) : null;
                        await this.validateStockForProduct(key, product.productId, quantity);
                      }
                    }
                  }
                },
              })}
            </div>
          </div>

          <div className="form-section">
            <h3 className="section-title">To</h3>
            <div className="flex">
              {this.renderAutoComplete({
                fieldname: "toProjectId",
                placeholder: "Project",
                options: this.state.dropdowns.projects || this.props.dropdowns?.projects || [],
                disableClearable: true,
                required: true,
                getOption: (option) => {
                  return option["name"] || option["tenantName"] || "";
                },
                onChange: (e, value) => {
                  if (value) {
                    const tenantCode = value.id || value.tenantCode || "";
                    this.formData.toProjectId = tenantCode;
                    this.formData.toProjectName = value.name || value.tenantName || "";
                    // Fetch warehouses for the selected tenant
                    this.fetchWarehousesForTenant(tenantCode, 'to');
                    // Clear selected warehouse when project changes
                    this.formData.toWarehouseId = "";
                    this.formData.toWarehouseName = "";
                    // Reset selected warehouse object in state
                    this.setState({
                      selectedToWarehouse: null
                    });
                  }
                },
              })}
              {this.renderAutoComplete({
                fieldname: "toWarehouseId",
                placeholder: "Ware House",
                options: this.state.dropdowns.toWarehouse || this.props.dropdowns?.warehouse || [],
                value: this.state.selectedToWarehouse,
                disableClearable: true,
                required: true,
                getOption: (option) => {
                  return option["name"] || "";
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData.toWarehouseId = value.id || "";
                    this.formData.toWarehouseName = value.name || "";
                    this.setState({ selectedToWarehouse: value });
                  }
                },
              })}
            </div>
          </div>

          <div className="form-section">
            <h3 className="section-title">Products</h3>
            <div className="inventories-list">
              {Object.keys(this.state.products).sort((a, b) => Number(a) - Number(b)).map((key) => this.renderProduct(key))}
            </div>
            {this.renderProductAddButton()}
            <div className="flex">
              {this.renderTextField({
                fieldname: "remarks",
                placeholder: "Transfer Remarks",
                multiline: true,
                rows: 4,
                skipAdd: true,
                onChange: (value) => {
                  this.formData.remarks = value;
                  this.setState({});
                },
              })}
            </div>
          </div>
        </form>
        </div>
        <InventoryTransferConfirm
          open={this.state.showConfirmDialog}
          transferData={this.state.transferData}
          onCancel={this.handleConfirmCancel}
          onConfirm={this.handleConfirmSubmit}
        />
        <InventoryTransferResultModal
          open={this.state.showResultModal}
          itemResults={this.state.resultModalData?.itemResults || []}
          productLookup={this.state.resultModalData?.productLookup || {}}
          onClose={this.handleResultModalClose}
        />
      </div>
    );
  }
}

export default connect()(withSnackbar(Add));
