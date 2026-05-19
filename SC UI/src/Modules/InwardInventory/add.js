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
import Button from "./../../Shared/Button";
import CloseIcon from "@material-ui/icons/Close";
//style
import "./style.scss";
import { fetchUnit } from "./../../actions/measurementUnit";
//misc
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import moment from "moment";
import { getRoleEditConstraintDays } from "./../../helper";

class InwardInventoryForm extends AddForm {
  title = 'Add ' + messages.common.inventory;
  state = {
    value: 0,
    noproduct: {},
    currentStock: {},
    localProducts: [],
    hasPurchaseOrder: false,
    // PO-related state
    poDetails: null,
    poOptions: [],
    isLoadingPO: false,
    selectedPO: null,
    selectedSupplier: null,
    isDirectInward: true,
    isSampleInward: false,     // NEW: tracks sample inward mode
    isEditMode: false,
    isLoaded: false,
    isProductsLoaded: false,
    createdFromPO: false,
    originalSupplierId: null,
    formKey: 0,
    // Store form field values in state for React to track changes
    formValues: {
      ourSlipNo: '',
      vehicleNo: '',
      supplierSlipNo: '',
      challanNo: '',
      billNo: '',
      additionalInfo: '',
      challanDate: null,
      billDate: null,
      date: null,
    },
  };
  key = 1;

  async loadExistingData() {
    const response = await API.GET(apiEndpoints.getInwardInventoryDetail + this.props.id);
    if (response.success) {
      const data = response.data;
      let p = {};
      const currentStock = {};
      const oldStock = {};
      let keyCounter = 1;

      // Determine mode based on createdFromPO flag
      const createdFromPO = data.createdFromPO || false;
      const isDirectInward = !createdFromPO;
      const originalSupplierId = data.supplier?.contactId || null;

      // For edit mode, restore isSampleInward from saved data
      const isSampleInward = data.isSampleInward || false;

      this.setState({
        isDirectInward,
        isSampleInward,
        isProductsLoaded: isDirectInward,
        createdFromPO,
        originalSupplierId,
      });

      if (isDirectInward) {
        this.formData.date = data.date || null;
        this.formData.supplierId = originalSupplierId;
        this.formData.supplierSlipNo = data.supplierSlipNo || data.vendorSlipNo || null;

        if (originalSupplierId && data.supplier) {
          this.setState({
            selectedSupplier: {
              id: originalSupplierId,
              name: data.supplier.name || ''
            }
          });
        }

        // Pass isSampleInward so fetchProducts loads the right product set
        await this.fetchProducts(isSampleInward);
      } else {
        this.fetchPOOptions();
        this.formData.poNumber = data.purchaseOrderNo;
        this.formData.poDate = data.purchaseOrderDate;
        this.formData.supplierId = originalSupplierId;

        this.setState({
          selectedPO: {
            id: data.purchaseOrderNo,
            name: [
              data.purchaseOrderNo,
              data.purchaseOrderDate ? new Date(data.purchaseOrderDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '',
              data.supplier?.name || ''
            ].filter(Boolean).join(' | '),
            poDate: data.purchaseOrderDate,
            supplierName: data.supplier?.name,
            ...data
          }
        });

        const lineItems = {};
        if (data.inwardOutwardList && data.inwardOutwardList.length > 0) {
          data.inwardOutwardList.forEach((item, index) => {
            const tolPct = item.tolerancePercent || 0;
            const poQty  = item.poQuantity || item.quantity || 0;
            // Option B: backend returns maxAllowedQuantity = indentQty × (1 + tolerance%)
            // Use it directly if provided; edit-mode fallback uses poQty (approximate)
            const maxAllowed = item.maxAllowedQuantity != null
              ? item.maxAllowedQuantity
              : poQty * (1 + tolPct / 100);
            lineItems[index] = {
              productId: item.product.productId,
              productCode: item.product.productCode,
              productName: item.product.productName,
              measurementUnit: item.product.measurementUnit,
              poQuantity: poQty,
              tolerancePercent: tolPct,
              maxAllowedQuantity: maxAllowed,
              quantity: "",
              warehouseId: item.warehouse?.warehouseId || null,
              lineItemCode: item.lineItemCode
            };
          });

          this.setState({
            poDetails: {
              purchaseOrderNumber: data.purchaseOrderNo,
              poDate: data.purchaseOrderDate,
              supplierName: data.supplier?.name,
              lineItems: data.inwardOutwardList.map(item => ({
                productId: item.product.productId,
                productCode: item.product.productCode,
                productName: item.product.productName,
                measurementUnit: item.product.measurementUnit,
                orderedQuantity: item.poQuantity || item.quantity,
                pendingQuantity: item.quantity,
                lineItemCode: item.lineItemCode
              }))
            },
            isLoadingPO: false
          });
        } else {
          this.setState({
            poDetails: {
              purchaseOrderNumber: data.purchaseOrderNo,
              poDate: data.purchaseOrderDate,
              supplierName: data.supplier?.name,
              lineItems: []
            },
            isLoadingPO: false
          });
        }

        p = lineItems;
        this.formData.inwardDate = data.date || null;
        this.formData.supplierSlipNo = data.supplierSlipNo || data.vendorSlipNo;
      }

      // Common fields for both modes
      this.formData.invoiceReceived = data.invoiceReceived || false;
      this.formData.vehicleNo = data.vehicleNo || '';
      this.formData.ourSlipNo = data.ourSlipNo || '';
      this.formData.challanNo = data.challanNo || '';
      this.formData.challanDate = data.challanDate || null;
      this.formData.billNo = data.billNo || '';
      this.formData.billDate = data.billDate || null;
      this.formData.additionalInfo = data.additionalInfo || '';
      this.formData.fileInformations = data.fileInformations || [];

      // Process existing line items
      if (isDirectInward) {
        for (let i = 0; i < data.inwardOutwardList.length; i++) {
          const item = data.inwardOutwardList[i];
          const pid = item.product.productId;
          p[keyCounter] = {
            quantity: item.quantity,
            productId: pid,
            warehouseId: item.warehouse?.warehouseId,
            productCode: item.product.productCode,
            productName: item.product.productName,
            unit: item.product.measurementUnit,
            measurementUnit: item.product.measurementUnit,
            poQuantity: item.poQuantity,
            lineItemCode: item.lineItemCode,
            selectedProduct: {
              id: pid,
              name: item.product.productName,
              productCode: item.product.productCode,
              measurementUnit: item.product.measurementUnit,
              isManagedInventory: item.product.isManagedInventory,
            }
          };
          currentStock[pid] = item.closingStock;
          oldStock[pid] = item.quantity;
          keyCounter++;
        }
      } else {
        for (let i = 0; i < data.inwardOutwardList.length; i++) {
          const item = data.inwardOutwardList[i];
          const pid = item.product.productId;
          currentStock[pid] = item.closingStock;
          oldStock[pid] = item.quantity;
        }
      }

      this.oldStock = oldStock;

      const formValues = {
        ourSlipNo: this.formData.ourSlipNo || '',
        vehicleNo: this.formData.vehicleNo || '',
        supplierSlipNo: this.formData.supplierSlipNo || '',
        challanNo: this.formData.challanNo || '',
        billNo: this.formData.billNo || '',
        additionalInfo: this.formData.additionalInfo || '',
        challanDate: this.formData.challanDate,
        billDate: this.formData.billDate,
        date: this.formData.date,
        inwardDate: this.formData.inwardDate,
      };

      const newFormKey = Date.now();

      this.setState({
        isLoaded: true,
        noproduct: { ...p },
        currentStock: currentStock,
        formKey: newFormKey,
        formValues: formValues,
      });
    }
  }

  componentDidMount() {
    const { dispatch, mode, id } = this.props;
    dispatch(fetchUnit());

    const isEditMode = !!id;
    this.setState({ isEditMode });

    if (isEditMode) {
      this.loadExistingData();
    } else {
      // CHANGE 1: detect sample mode from prop
      const isSampleInward = mode === 'sample';
      const isDirectInward = mode === 'direct' || isSampleInward;

      this.setState({ isDirectInward, isSampleInward });

      if (isDirectInward) {
        // CHANGE 2: pass isSampleInward to fetchProducts
        this.fetchProducts(isSampleInward);
      } else {
        this.fetchPOOptions();
        this.setState({ isProductsLoaded: true });
      }

      this.setState({ noproduct: {} });
    }
  }

  renderHeading() {
    const actionText = this.state.isEditMode ? messages.common.update : messages.common.add;
    return (
      <div className="add-heading-wrapper">
        <IconButton
          aria-label="back"
          onClick={this.props.back}
          className="back-icon"
        >
          <KeyboardBackspaceIcon />
        </IconButton>
        <span className="add-heading">{actionText + messages.common.inventory}</span>
      </div>
    );
  }

  // CHANGE 3: fetchProducts now accepts isSampleInward flag
  // - isSampleInward = true  → fetch ALL products (managed + unmanaged)
  // - isSampleInward = false → fetch only unmanaged products (original behaviour)
  async fetchProducts(isSampleInward = false) {
    const url = isSampleInward
      ? apiEndpoints.getProductWithManagedInventory()       // no filter → all products
      : apiEndpoints.getProductWithManagedInventory(false); // unmanaged only

    const response = await API.GET(url);
    if (response.success && Array.isArray(response.data)) {
      const transformedProducts = response.data
        .filter(product =>
          product &&
          // Sample inward: allow all products. Direct inward: unmanaged only.
          (isSampleInward || product.isManagedInventory === false) &&
          product.productId &&
          product.productName &&
          typeof product.productName === 'string' &&
          product.productCode &&
          typeof product.productCode === 'string'
        )
        .map((product) => ({
          id: product.productId,
          name: product.productName,
          measurementUnit: product.measurementUnit,
          productCode: product.productCode,
          isManagedInventory: product.isManagedInventory,
          isExpirable: product.isExpirable || false,
        }));

      // In edit mode, also include existing products from the inward data
      // (they may not appear in the filtered list otherwise)
      let finalProducts = [...transformedProducts];
      if (this.state.isEditMode && this.state.noproduct) {
        const existingProductIds = new Set(transformedProducts.map(p => p.id));
        Object.values(this.state.noproduct).forEach(product => {
          if (product.productId && !existingProductIds.has(product.productId)) {
            finalProducts.push({
              id: product.productId,
              name: product.productName,
              measurementUnit: product.measurementUnit,
              productCode: product.productCode,
              isManagedInventory: product.isManagedInventory,
            });
          }
        });
      }

      this.setState({
        localProducts: finalProducts,
        ...(this.state.isEditMode ? {} : { noproduct: {} }),
        isProductsLoaded: true,
      });
    } else {
      this.setState({
        localProducts: [],
        noproduct: {},
      });
    }
  }

  async fetchPOOptions() {
    try {
      const response = await API.GET(apiEndpoints.getInwardPODropdown);
      if (response.success && Array.isArray(response.data)) {
        const validPOs = response.data.filter(po => po && po.purchaseOrderNumber && typeof po.purchaseOrderNumber === 'string');
        this.setState({
          poOptions: validPOs
            .map(po => ({
              id: po.purchaseOrderNumber,
              name: [
                po.purchaseOrderNumber,
                po.poDate ? new Date(po.poDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '',
                po.supplierName || ''
              ].filter(Boolean).join(' | '),
              ...po
            }))
            .sort((a, b) => {
              const numA = parseInt(a.name.replace(/^[A-Za-z-]+/, ''), 10);
              const numB = parseInt(b.name.replace(/^[A-Za-z-]+/, ''), 10);
              if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
              return a.name.localeCompare(b.name);
            })
        });
      }
    } catch (error) {
      console.error('Error fetching PO options:', error);
      this.props.enqueueSnackbar("Failed to load PO options", { variant: "error" });
    }
  }

  async fetchPODetails(poNumber) {
    if (!poNumber) return;

    this.setState({ isLoadingPO: true });
    try {
      const response = await API.GET(apiEndpoints.getInwardPODetails(poNumber));

      if (response.success && response.data) {
        const poData = response.data;
        this.setState({
          poDetails: poData,
          isLoadingPO: false
        });

        if (!this.state.isEditMode) {
          this.formData.poNumber = poData.purchaseOrderNumber || poData.poNumber;
          this.formData.poDate = poData.poDate;
          this.formData.supplierId = poData.supplierId || poData.supplierName;
        }

        const lineItems = {};
        poData.lineItems?.forEach((item, index) => {
          const tolPct    = item.tolerancePercent || 0;
          const poQty     = item.orderedQuantity || 0;
          const pendingQty = item.pendingQuantity != null ? item.pendingQuantity : poQty;
          lineItems[index] = {
            productId: item.productId,
            productCode: item.productCode,
            productName: item.productName,
            measurementUnit: item.measurementUnit,
            poQuantity: poQty,
            tolerancePercent: tolPct,
            pendingQuantity: pendingQty,
            maxAllowedQuantity: item.maxAllowedQuantity != null ? item.maxAllowedQuantity : pendingQty + (poQty * tolPct / 100),
            quantity: "",
            warehouseId: null,
            lineItemCode: item.lineItemCode
          };
        });

        this.setState({ noproduct: lineItems });
      } else {
        this.setState({ isLoadingPO: false });
        this.props.enqueueSnackbar("Failed to load PO details", { variant: "error" });
      }
    } catch (error) {
      console.error('Error fetching PO details:', error);
      this.setState({ isLoadingPO: false });
      this.props.enqueueSnackbar("Failed to load PO details", { variant: "error" });
    }
  }

  renderProduct(key) {
    const product = this.state.noproduct[key];
    if (!product) return null;

    const isEditMode = this.state.isEditMode;

    if (this.state.isDirectInward) {
      // Direct Inward / Sample Inward Layout
      const currentProductId = product.productId;
      const selectedProducts = Object.keys(this.state.noproduct).map(index => this.state.noproduct[index]?.productId);
      const productList = this.state.localProducts || [];

      let dropdownProducts = [...productList];
      if (product.selectedProduct && !dropdownProducts.find(p => p.id === product.selectedProduct.id)) {
        dropdownProducts.push(product.selectedProduct);
      }

      const remainingProducts = dropdownProducts.filter(item => (!selectedProducts.includes(item.id) || currentProductId === item.id));

      return (
        <div key={key} className="product-row-container" style={{ marginBottom: '16px', position: 'relative' }}>
          {!isEditMode && (
            <IconButton
              aria-label="close"
              onClick={() => {
                const p = this.state.noproduct;
                delete p[key];
                this.setState({ noproduct: { ...p } });
              }}
              className="close-icon"
              disableRipple
              disableFocusRipple
              size="small"
              style={{
                position: 'absolute',
                top: '8px',
                right: '8px',
                backgroundColor: 'transparent',
                padding: '4px',
                width: '24px',
                height: '24px',
                zIndex: 10
              }}
            >
              <CloseIcon style={{ fontSize: '18px' }} />
            </IconButton>
          )}

          <div className="product-row-scroll" style={{ overflowX: 'auto', overflowY: 'hidden', paddingRight: '40px', paddingBottom: '20px' }}>
            <div className="flex product-row" style={{ alignItems: 'center', paddingTop: '16px', display: 'flex', flexWrap: 'nowrap', width: 'fit-content' }}>
              {/* Warehouse */}
              <div style={{ width: '200px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderAutoComplete({
                  fieldname: `warehouse_${key}`,
                  placeholder: "Warehouse",
                  options: this.props.dropdowns?.warehouse || [],
                  value: this.props.dropdowns?.warehouse?.find(w => w.id === product.warehouseId) || null,
                  disableClearable: true,
                  required: true,
                  disabled: isEditMode,
                  getOption: (option) => option?.name || '',
                  onChange: (e, value) => {
                    const p = this.state.noproduct;
                    p[key].warehouseId = value?.id || null;
                    if (value?.id) {
                      this.setState({ noproduct: { ...p } }, () => {
                        this.getCurrentStock(key);
                      });
                    }
                  },
                })}
              </div>

              {/* Product Name */}
              <div style={{ width: '220px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderAutoComplete({
                  fieldname: `productName_${key}`,
                  placeholder: "Product Name",
                  options: remainingProducts,
                  disableClearable: true,
                  required: true,
                  disabled: isEditMode,
                  value: product.selectedProduct || null,
                  getOption: (option) => option?.name || '',
                  onChange: (e, value) => {
                    const p = this.state.noproduct;
                    p[key].productId = value?.id || "";
                    p[key].productCode = value?.productCode || "";
                    p[key].unit = value?.measurementUnit || "";
                    p[key].isExpirable = value?.isExpirable || false;
                    p[key].selectedProduct = value;
                    if (value) {
                      this.setState({ noproduct: { ...p } }, () => {
                        this.getCurrentStock(key);
                      });
                    }
                  },
                })}
              </div>

              {/* Product Code */}
              <div style={{ width: '180px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderAutoComplete({
                  fieldname: `productCode_${key}`,
                  placeholder: "Product Code",
                  options: remainingProducts,
                  disableClearable: false,
                  required: true,
                  disabled: isEditMode,
                  value: product.selectedProduct || null,
                  getOption: (option) => option?.productCode || '',
                  onChange: (e, value) => {
                    const p = this.state.noproduct;
                    p[key].productId = value?.id || "";
                    p[key].productCode = value?.productCode || "";
                    p[key].unit = value?.measurementUnit || "";
                    p[key].isExpirable = value?.isExpirable || false;
                    p[key].selectedProduct = value;
                    if (value) {
                      this.setState({ noproduct: { ...p } }, () => {
                        this.getCurrentStock(key);
                      });
                    }
                  },
                })}
              </div>

              {/* Measurement Unit */}
              <div style={{ width: '50px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `unit_${key}`,
                  placeholder: "Unit",
                  value: product.unit || '',
                  disabled: true,
                  skipAdd: true,
                })}
              </div>

              {/* Quantity */}
              <div style={{ width: '90px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `quantity_${key}`,
                  placeholder: "Receive Qty",
                  type: "number",
                  required: true,
                  skipAdd: true,
                  validation: "nonegative",
                  value: product.quantity,
                  onChange: (value) => {
                    const p = this.state.noproduct;
                    p[key].quantity = parseFloat(value) || 0;
                    this.setState({ noproduct: { ...p } }, () => {
                      this.getCurrentStock(key);
                    });
                  },
                })}
              </div>

              {/* Closing Stock */}
              <div style={{ width: '110px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `closingStock_${key}`,
                  placeholder: "Closing Stock",
                  type: "number",
                  value: this.state.currentStock[product.productId] || '',
                  disabled: true,
                  skipAdd: true,
                })}
              </div>

              {/* Brand (optional) */}
              <div style={{ width: '150px', flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `brand_${key}`,
                  placeholder: "Brand (optional)",
                  skipAdd: true,
                  value: product.brand || '',
                  onChange: (value) => {
                    const p = this.state.noproduct;
                    p[key].brand = value;
                    this.setState({ noproduct: { ...p } });
                  },
                })}
              </div>

              {/* Expiry Date (required if expirable) */}
              <div style={{ width: '170px', flexShrink: 0, display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                {this.renderDate({
                  fieldname: `expiryDate_${key}`,
                  label: product.isExpirable ? "Expiry Date *" : "Expiry Date",
                  value: product.expiryDate || null,
                  onChange: () => {
                    const p = this.state.noproduct;
                    p[key].expiryDate = this.formData[`expiryDate_${key}`];
                    this.setState({ noproduct: { ...p } });
                  },
                })}
                {product.isExpirable && (
                  <span style={{ fontSize: '10px', color: '#888', marginTop: '2px' }}>Required for this product</span>
                )}
              </div>
            </div>
          </div>

          {/* Warning: all products used up */}
          {productList.length > 0 && remainingProducts.length === 0 && (
            <div style={{
              color: '#ff9800',
              fontSize: '12px',
              marginTop: '8px',
              marginBottom: '12px',
              paddingLeft: '4px'
            }}>
              {this.state.isSampleInward
                ? "All available products have been added."
                : "All available unmanaged products have been added. Please remove a product from another row to add a different one."}
            </div>
          )}
        </div>
      );
    } else {
      // PO Inward Layout
      return (
        <div key={key} className="product-row-container" style={{ marginBottom: '16px', position: 'relative' }}>
          <IconButton
            aria-label="close"
            onClick={() => {
              const p = this.state.noproduct;
              delete p[key];
              this.setState({ noproduct: { ...p } });
            }}
            className="close-icon"
            disableRipple
            disableFocusRipple
            size="small"
            style={{
              position: 'absolute',
              top: '8px',
              right: '8px',
              backgroundColor: 'transparent',
              padding: '4px',
              width: '24px',
              height: '24px',
              zIndex: 10
            }}
          >
            <CloseIcon style={{ fontSize: '18px' }} />
          </IconButton>

          <div className="product-row-scroll" style={{ overflowX: 'auto', overflowY: 'hidden', paddingRight: '40px', paddingBottom: '20px' }}>
            <div className="flex product-row" style={{ alignItems: 'center', paddingTop: '16px', flexWrap: 'nowrap', display: 'flex', width: 'fit-content' }}>
              {/* Warehouse */}
              <div style={{ width: '200px', flexShrink: 0, marginRight: '4px', display: 'flex', alignItems: 'center' }}>
                {this.renderAutoComplete({
                  fieldname: `warehouse_${key}`,
                  placeholder: "Warehouse",
                  options: this.props.dropdowns?.warehouse || [],
                  value: this.props.dropdowns?.warehouse?.find(w => w.id === product.warehouseId) || null,
                  disableClearable: true,
                  required: true,
                  getOption: (option) => option?.name || '',
                  onChange: (e, value) => {
                    const p = this.state.noproduct;
                    p[key].warehouseId = value?.id || null;
                    if (value?.id) {
                      this.setState({ noproduct: { ...p } }, () => {
                        this.getCurrentStock(key);
                      });
                    }
                  },
                })}
              </div>

              {/* Product Name - Read Only */}
              <div style={{ width: '220px', flexShrink: 0, marginRight: '4px', display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `productName_${key}`,
                  placeholder: "Product Name",
                  value: product.productName || '',
                  disabled: true,
                  skipAdd: true,
                })}
              </div>

              {/* PO Quantity - Read Only */}
              <div style={{ width: '50px', flexShrink: 0, marginRight: '4px', display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `poQuantity_${key}`,
                  placeholder: "PO Qty",
                  type: "number",
                  value: product.poQuantity || '',
                  disabled: true,
                  skipAdd: true,
                })}
              </div>

              {/* Max Allowed - Read Only, always shown for PO-linked inwards */}
              {product.maxAllowedQuantity != null && (
                <div style={{ width: '80px', flexShrink: 0, marginRight: '4px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                  {this.renderTextField({
                    fieldname: `maxAllowed_${key}`,
                    placeholder: "Max Allowed",
                    type: "number",
                    value: Math.floor(product.maxAllowedQuantity * 100) / 100,
                    disabled: true,
                    skipAdd: true,
                  })}
                  <span style={{ fontSize: '10px', color: '#888', marginTop: '2px' }}>
                    {product.tolerancePercent > 0
                      ? `Max (incl. ${product.tolerancePercent}% tol.)`
                      : 'Max Allowed (pending)'}
                  </span>
                </div>
              )}

              {/* Measurement Unit - Read Only */}
              <div style={{ width: '70px', flexShrink: 0, marginRight: '4px', display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `unit_${key}`,
                  placeholder: "Unit",
                  value: product.measurementUnit || '',
                  disabled: true,
                  skipAdd: true,
                })}
              </div>

              {/* Quantity - Editable */}
              <div style={{ width: '90px', flexShrink: 0, marginRight: '4px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                {this.renderTextField({
                  fieldname: `quantity_${key}`,
                  placeholder: "Receive Qty",
                  type: "number",
                  required: true,
                  skipAdd: true,
                  validation: "nonegative",
                  value: product.quantity,
                  onChange: (value) => {
                    const p = this.state.noproduct;
                    p[key].quantity = parseFloat(value) || 0;
                    this.setState({ noproduct: { ...p } }, () => {
                      this.getCurrentStock(key);
                    });
                  },
                })}
                {product.quantity > product.maxAllowedQuantity && (
                  <span style={{ fontSize: '10px', color: '#c62828', marginTop: '2px' }}>
                    {product.tolerancePercent > 0
                      ? `Exceeds max allowed (${Math.floor(product.maxAllowedQuantity * 100) / 100}). Pending: ${Math.round(product.pendingQuantity * 100) / 100}, Tolerance: ${product.tolerancePercent}%`
                      : `Exceeds pending qty (${Math.round(product.pendingQuantity * 100) / 100}). No tolerance set.`}
                  </span>
                )}
                {product.tolerancePercent > 0 && product.quantity > product.pendingQuantity && product.quantity <= product.maxAllowedQuantity && (
                  <span style={{ fontSize: '10px', color: '#2e7d32', marginTop: '2px' }}>
                    Within {product.tolerancePercent}% tolerance. Max: {Math.floor(product.maxAllowedQuantity * 100) / 100}
                  </span>
                )}
              </div>

              {/* Closing Stock - Read Only */}
              <div style={{ width: '50px', flexShrink: 0, marginRight: '4px', display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `closingStock_${key}`,
                  placeholder: "Closing Stock",
                  type: "number",
                  value: this.state.currentStock[product.productId] != null
                    ? Math.round(this.state.currentStock[product.productId] * 100) / 100
                    : '',
                  disabled: true,
                  skipAdd: true,
                })}
              </div>

              {/* Brand (optional) */}
              <div style={{ width: '150px', flexShrink: 0, marginRight: '4px', display: 'flex', alignItems: 'center' }}>
                {this.renderTextField({
                  fieldname: `brand_${key}`,
                  placeholder: "Brand (optional)",
                  skipAdd: true,
                  value: product.brand || '',
                  onChange: (value) => {
                    const p = this.state.noproduct;
                    p[key].brand = value;
                    this.setState({ noproduct: { ...p } });
                  },
                })}
              </div>

              {/* Expiry Date */}
              <div style={{ width: '170px', flexShrink: 0, marginRight: '4px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                {this.renderDate({
                  fieldname: `expiryDate_${key}`,
                  label: "Expiry Date",
                  value: product.expiryDate || null,
                  onChange: () => {
                    const p = this.state.noproduct;
                    p[key].expiryDate = this.formData[`expiryDate_${key}`];
                    this.setState({ noproduct: { ...p } });
                  },
                })}
              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  async getCurrentStock(index) {
    const productItem = this.state.noproduct[index];
    const warehouseId = productItem?.warehouseId;
    const productId = productItem?.productId;
    if (!productId || !productItem || !warehouseId) return;

    const response = await API.GET(
      apiEndpoints.getCurrentStock +
        "productId=" + productId +
        "&warehouseId=" + warehouseId
    );
    if (response.success) {
      const currentStock = this.state.currentStock;
      const newQuantity = Number(productItem.quantity) || 0;
      const apiStock = Number(response.data) || 0;

      if (this.state.isEditMode && this.oldStock && this.oldStock[productId] !== undefined) {
        const oldQuantity = Number(this.oldStock[productId]) || 0;
        currentStock[productId] = apiStock + (newQuantity - oldQuantity);
      } else {
        currentStock[productId] = apiStock + newQuantity;
      }

      this.setState({ currentStock: { ...currentStock } });
    }
  }

  async add(event) {
    event.preventDefault();

    if (Object.keys(this.state.noproduct).length === 0) {
      this.props.enqueueSnackbar("Add atleast one Product", { variant: "error" });
      return;
    }

    if (!this.formData.fileInformations || this.formData.fileInformations.length === 0) {
      this.props.enqueueSnackbar("Please upload at least one file", { variant: "error" });
      return;
    }

    // Validate expiry date for expirable products (direct inward only — PO validates server-side too)
    if (this.state.isDirectInward) {
      for (const product of Object.values(this.state.noproduct)) {
        if (product.isExpirable && !product.expiryDate) {
          this.props.enqueueSnackbar(
            `Expiry date is required for "${product.productName || 'product'}".`,
            { variant: "error" }
          );
          return;
        }
      }
    }

    // Block submission if any PO-linked line item exceeds its max allowed quantity
    if (!this.state.isDirectInward) {
      for (const product of Object.values(this.state.noproduct)) {
        const max = product.maxAllowedQuantity != null ? product.maxAllowedQuantity : product.pendingQuantity || product.poQuantity;
        if (product.quantity > max) {
          const tolerance = product.tolerancePercent > 0
            ? ` (Pending: ${product.pendingQuantity}, Tolerance: ${product.tolerancePercent}%)`
            : ` No tolerance set.`;
          this.props.enqueueSnackbar(
            `${product.productName}: Quantity ${product.quantity} exceeds max allowed ${Math.floor(max * 100) / 100}.${tolerance}`,
            { variant: "error" }
          );
          return;
        }
      }
    }

    let params;
    let apiUrl;
    let apiMethod;

    if (this.state.isEditMode) {
      apiUrl = apiEndpoints.updateInwardInventory + this.props.id;
      apiMethod = 'PUT';

      const supplierIdToSend = this.state.createdFromPO
        ? this.state.originalSupplierId
        : this.formData.supplierId;

      params = {
        inwardDate: (this.state.isDirectInward ? this.formData.date : this.formData.inwardDate) || null,
        supplierId: supplierIdToSend,
        productWithQuantities: Object.values(this.state.noproduct).map(product => ({
          productId: product.productId,
          quantity: product.quantity
        })),
        vehicleNo: this.formData.vehicleNo,
        supplierSlipNo: this.formData.supplierSlipNo,
        ourSlipNo: this.formData.ourSlipNo,
        additionalInfo: this.formData.additionalInfo,
        invoiceReceived: this.formData.invoiceReceived || false,
        challanNo: this.formData.challanNo,
        billNo: this.formData.billNo,
        challanDate: this.formData.challanDate || null,
        billDate: this.formData.billDate || null,
        fileInformations: this.formData.fileInformations || []
      };
    } else if (this.state.isDirectInward) {
      // Handles both Direct Inward and Sample Inward (same endpoint)
      apiUrl = apiEndpoints.createInwardInventory;
      apiMethod = 'POST';
      params = {
        inwardDate: this.formData.date,
        supplierId: this.formData.supplierId,
        isSampleInward: this.state.isSampleInward,   // CHANGE 4: send flag to backend
        productWithQuantities: Object.values(this.state.noproduct).map(product => ({
          warehouseId: product.warehouseId,
          productId: product.productId,
          quantity: product.quantity,
          brand: product.brand || null,
          expiryDate: product.expiryDate || null,
        })),
        fileInformations: this.formData.fileInformations || [],
        invoiceReceived: this.formData.invoiceReceived || false,
        vehicleNo: this.formData.vehicleNo,
        supplierSlipNo: this.formData.supplierSlipNo,
        ourSlipNo: this.formData.ourSlipNo,
        additionalComments: this.formData.additionalInfo,
        challanNo: this.formData.challanNo,
        challanDate: this.formData.challanDate || null,
        billNo: this.formData.billNo,
        billDate: this.formData.billDate || null
      };
    } else {
      // PO Inward
      apiUrl = apiEndpoints.createInwardInventoryFromPO;
      apiMethod = 'POST';
      params = {
        poNumber: this.formData.poNumber,
        inwardDate: this.formData.inwardDate,
        invoiceReceived: this.formData.invoiceReceived || false,
        vehicleNo: this.formData.vehicleNo,
        supplierSlipNo: this.formData.supplierSlipNo,
        ourSlipNo: this.formData.ourSlipNo,
        billNo: this.formData.billNo,
        challanNo: this.formData.challanNo,
        challanDate: this.formData.challanDate || null,
        billDate: this.formData.billDate || null,
        additionalInfo: this.formData.additionalInfo,
        fileInformations: this.formData.fileInformations || [],
        lineItems: Object.values(this.state.noproduct).map(item => ({
          lineItemCode: item.lineItemCode,
          quantityReceived: item.quantity,
          warehouseId: item.warehouseId,
          brand: item.brand || null,
          expiryDate: item.expiryDate || null,
        }))
      };
    }

    this.setState({ isAdding: true });

    const response = apiMethod === 'PUT'
      ? await API.PUT(apiUrl, params)
      : await API.POST(apiUrl, params);

    this.showToaster(response);
    this.setState({ isAdding: false });
  }

  async updateStockInfo(warehouseId, productIds) {
    if (!warehouseId || !productIds || productIds.length === 0) return;

    const params = { warehouseId, productIds };
    const response = await API.POST(apiEndpoints.getMultiStock, params);
    if (response.success) {
      const data = response.data;
      const currentStock = { ...this.state.currentStock };
      const products = Object.values(this.state.noproduct);

      data.forEach((element) => {
        const productId = element.productId;
        let product = products.filter((p) => p.productId === productId);
        product = product[0];
        if (product) {
          currentStock[productId] = element.stock + Number(product.quantity);
        }
      });
      this.setState({ currentStock });
    }
  }

  renderFooter() {
    return (
      <div className="form-footer">
        {this.renderToggle("Invoice Received", "invoiceReceived")}
        <Button
          onClick={this.props.back}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          type="submit"
          buttonClass="blue"
          label={messages.common.save}
          disabled={this.state.isAdding || Object.keys(this.formValidation).length}
        />
      </div>
    );
  }

  render() {
    const days = getRoleEditConstraintDays();
    const { poDetails, isLoadingPO } = this.state;

    // CHANGE 5: derive section title and empty-state message from mode
    const productsSectionTitle = this.state.isSampleInward
      ? "Products (Sample Inward)"
      : this.state.isDirectInward
      ? "Products (Direct Inward - Unmanaged Inventory)"
      : "Line Items from Purchase Order";

    const noProductsMessage = this.state.isSampleInward
      ? "No products available. Please add products first."
      : "No unmanaged products available for direct inward. Please add unmanaged products first.";

    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isEditMode && !this.state.isLoaded && (
          <div className="loading-section" style={{ textAlign: 'center', padding: '20px' }}>
            Loading inward inventory details...
          </div>
        )}
        {(this.state.isEditMode ? this.state.isLoaded : true) && (() => {
          return (
            <form key={`form_${this.state.formKey || 0}`} onSubmit={(e) => this.add(e)}>

              {/* Purchase Order Section - Only show when in PO mode */}
              {!this.state.isDirectInward && (
                <div className="form-section">
                  <div className="section-header">
                    <h3 className="section-title">Purchase Order</h3>
                  </div>
                  {this.state.isLoadingPO ? (
                    <div className="loading-po">Loading purchase order details...</div>
                  ) : (
                    <div className="flex" style={{ alignItems: 'flex-end' }}>
                      <div style={{ maxWidth: '400px', minWidth: '300px' }}>
                        {this.renderAutoComplete({
                          fieldname: "poNumber",
                          placeholder: "Select Purchase Order Number",
                          options: this.state.poOptions,
                          value: this.state.selectedPO,
                          disableClearable: true,
                          required: true,
                          disabled: this.state.isLoadingPO || this.state.isEditMode,
                          getOption: (option) => option?.name || '',
                          onOpen: () => {},
                          onChange: (e, value) => {
                            this.formData.poNumber = value?.id || '';
                            if (value?.id) {
                              this.setState({ selectedPO: value }, () => {
                                this.fetchPODetails(value.id);
                              });
                            } else {
                              this.setState({ selectedPO: null, poDetails: null, noproduct: {} });
                            }
                          },
                        })}
                      </div>
                      {poDetails && (
                        <>
                          {this.renderTextField({
                            fieldname: "poDate_display",
                            placeholder: "PO Date",
                            value: poDetails.poDate || '',
                            disabled: true,
                            skipAdd: true,
                          })}
                          {this.renderTextField({
                            fieldname: "supplier_display",
                            placeholder: "Supplier",
                            value: poDetails.supplierName || '',
                            disabled: true,
                            skipAdd: true,
                          })}
                          {this.renderTextField({
                            fieldname: "lineItems_display",
                            placeholder: "Line Items",
                            value: `${poDetails.lineItems?.length || 0} items`,
                            disabled: true,
                            skipAdd: true,
                          })}
                        </>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* Inward Details Section */}
              {this.state.isDirectInward || this.state.poDetails ? (
                <div className="form-section">
                  <div className="section-header">
                    <h3 className="section-title">Inward Details</h3>
                  </div>

                  <div className="flex width50">
                    {/* Supplier selection for Direct / Sample Inward */}
                    {this.state.isDirectInward && this.renderAutoComplete({
                      fieldname: "supplierId",
                      placeholder: "Supplier",
                      options: this.props.dropdowns?.supplier,
                      value: this.state.selectedSupplier || this.props.dropdowns?.supplier?.find(s => s.id === this.formData.supplierId) || null,
                      disableClearable: true,
                      required: true,
                      disabled: this.state.createdFromPO,
                      getOption: (option) => option["name"],
                      onChange: (e, value) => {
                        if (value) {
                          this.formData.supplierId = value.id;
                          this.setState({ selectedSupplier: value });
                        } else {
                          this.formData.supplierId = null;
                          this.setState({ selectedSupplier: null });
                        }
                      },
                    })}
                    {this.renderDate({
                      fieldname: this.state.isDirectInward ? "date" : "inwardDate",
                      label: messages.common.receivingDate,
                      required: true,
                      maxDate: moment(),
                      minDate: moment().add(-days, "d"),
                      value: this.state.isDirectInward ? this.formData.date : this.formData.inwardDate,
                    })}
                    {(() => {
                      const value = this.formData.ourSlipNo || '';
                      const key = `ourSlipNo_${this.state.formKey || 0}`;
                      return this.renderTextField({
                        fieldname: "ourSlipNo",
                        placeholder: "MRN / GRN",
                        value,
                        validation: "maxlength",
                        lengthConstraint: 100,
                        errorMessage: messages.common.max100,
                        disabled: true,
                        key,
                      });
                    })()}
                    {(() => {
                      const value = this.formData.vehicleNo || '';
                      const key = `vehicleNo_${this.state.formKey || 0}`;
                      return this.renderTextField({
                        fieldname: "vehicleNo",
                        placeholder: "Vehicle Number",
                        value,
                        validation: "maxlength",
                        lengthConstraint: 100,
                        errorMessage: messages.common.max100,
                        key,
                      });
                    })()}
                    {(() => {
                      const value = this.formData.supplierSlipNo || '';
                      const key = `supplierSlipNo_${this.state.formKey || 0}`;
                      return this.renderTextField({
                        fieldname: "supplierSlipNo",
                        placeholder: "Supplier Slip No",
                        value,
                        validation: "maxlength",
                        lengthConstraint: 100,
                        errorMessage: messages.common.max100,
                        key,
                      });
                    })()}
                  </div>
                </div>
              ) : null}

              {/* Documentation Section */}
              {this.state.isDirectInward || this.state.poDetails ? (
                <div className="form-section">
                  <div className="section-header">
                    <h3 className="section-title">Documentation</h3>
                  </div>

                  <div className="flex width50">
                    {this.renderDate({
                      fieldname: "challanDate",
                      label: "Challan Date",
                      emptyDate: true,
                      type: "date",
                      value: this.formData.challanDate,
                    })}
                    {(() => {
                      const value = this.formData.challanNo || '';
                      const key = `challanNo_${this.state.formKey || 0}`;
                      return this.renderTextField({
                        fieldname: "challanNo",
                        placeholder: "Challan Number",
                        value,
                        key,
                      });
                    })()}
                  </div>

                  <div className="flex width50">
                    {this.renderDate({
                      fieldname: "billDate",
                      label: "Bill Date",
                      emptyDate: true,
                      type: "date",
                      value: this.formData.billDate,
                    })}
                    {(() => {
                      const value = this.formData.billNo || '';
                      const key = `billNo_${this.state.formKey || 0}`;
                      return this.renderTextField({
                        fieldname: "billNo",
                        placeholder: "Bill Number",
                        value,
                        key,
                      });
                    })()}
                  </div>

                  <div className="flex">
                    {this.renderTextArea({
                      fieldname: "additionalInfo",
                      placeholder: this.state.isDirectInward ? "Additional Comments" : "Additional Information",
                      value: this.formData.additionalInfo || '',
                      key: `additionalInfo_${this.state.formKey || 0}`,
                    })}
                  </div>

                  {this.renderFileArea()}
                </div>
              ) : null}

              {/* Products / Line Items Section */}
              {this.state.isDirectInward || this.state.poDetails ? (
                <div className="form-section">
                  <div className="section-header">
                    {/* CHANGE 5: dynamic section title */}
                    <h3 className="section-title">{productsSectionTitle}</h3>
                  </div>

                  <div className="products-list">
                    {/* CHANGE 5: dynamic empty-state message */}
                    {this.state.isDirectInward && this.state.localProducts && this.state.localProducts.length === 0 && (
                      <div style={{
                        color: '#ff9800',
                        fontSize: '14px',
                        margin: '16px 0',
                        padding: '12px 16px',
                        backgroundColor: '#fff3e0',
                        border: '1px solid #ffb74d',
                        borderRadius: '4px'
                      }}>
                        {noProductsMessage}
                      </div>
                    )}
                    {Object.keys(this.state.noproduct).map((key) =>
                      this.renderProduct(key)
                    )}
                    {this.state.isDirectInward && !this.state.isEditMode && this.renderProductAddButton()}
                  </div>
                </div>
              ) : null}

              {/* Footer */}
              {(this.state.isDirectInward || this.state.poDetails) && this.renderFooter()}
            </form>
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
  withSnackbar(InwardInventoryForm)
);