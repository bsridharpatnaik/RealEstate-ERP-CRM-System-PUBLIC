//react
import React from "react";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";
import CircularProgress from "@material-ui/core/CircularProgress";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import { withSnackbar } from "notistack";

class AddNewSupplierModal extends React.Component {
state = {
    supplierName: "",
    contactPersonName: "",
    officeNo: "",
    gstNo: "",
    contactPersonMobileNo: "",
    emailId: "",
    addressLine1: "",
    addressLine2: "",
    city: "",
    state: "",
    zipCode: "",
    accountName: "",
    accountNumber: "",
    bankName: "",
    branchName: "",
    ifscCode: "",
    errors: {},
    isSaving: false,
  };
  handleInputChange = (field) => (event) => {
    let value = event.target.value;
    
    // Restrict Office No. and Contact Person Mobile No. to 10 digits (numeric only)
    if (field === "officeNo" || field === "contactPersonMobileNo") {
      // Remove any non-numeric characters
      value = value.replace(/\D/g, "");
      // Limit to 10 digits
      if (value.length > 10) {
        value = value.slice(0, 10);
      }
    }
    
    // Restrict Zip Code to 6 digits (numeric only)
    if (field === "zipCode") {
      // Remove any non-numeric characters
      value = value.replace(/\D/g, "");
      // Limit to 6 digits
      if (value.length > 6) {
        value = value.slice(0, 6);
      }
    }
    
    // Restrict GST Number to 15 characters (alphanumeric, uppercase)
    if (field === "gstNo") {
      value = value.replace(/[^A-Za-z0-9]/g, "");
      value = value.toUpperCase();
      if (value.length > 15) {
        value = value.slice(0, 15);
      }
    }

    // Restrict Account Number to 30 digits only
if (field === "accountNumber") {
  value = value.replace(/[^A-Za-z0-9]/g, "");  // allows letters + digits
  if (value.length > 30) {
    value = value.slice(0, 30);
  }
}

    // Restrict Account Holder Name to 100 characters
    if (field === "accountName" && value.length > 100) {
      value = value.slice(0, 100);
    }

    // Restrict Bank Name to 50 characters
    if (field === "bankName" && value.length > 50) {
      value = value.slice(0, 50);
    }

    // Restrict Branch Name to 50 characters
    if (field === "branchName" && value.length > 50) {
      value = value.slice(0, 50);
    }

    // Restrict IFSC Code to 11 characters, uppercase alphanumeric
    if (field === "ifscCode") {
      value = value.replace(/[^A-Za-z0-9]/g, "");
      value = value.toUpperCase();
      if (value.length > 11) {
        value = value.slice(0, 11);
      }
    }
    
    this.setState({ 
      [field]: value,
      errors: {
        ...this.state.errors,
        [field]: this.getFieldError(field, value)
      }
    });
  };

  getFieldError = (field, value) => {
    const trimmedValue = value ? value.trim() : "";
    
    // Required field validation
    const requiredFields = {
      supplierName: "Supplier Name is required",
      contactPersonName: "Contact Person Name is required",
      contactPersonMobileNo: "Contact Person Mobile No. is required",
      addressLine1: "Address line 1 is required",
      city: "City is required",
      state: "State is required",
      zipCode: "Zip Code is required"
    };
    
    if (requiredFields[field] && !trimmedValue) {
      return requiredFields[field];
    }
    
    // Format validation for non-empty fields (only validate when field is at full length or required)
    if (trimmedValue) {
      // Zip Code: validate format only when fully entered (6 digits) - required field
      if (field === "zipCode") {
        if (trimmedValue.length === 6 && !/^\d{6}$/.test(trimmedValue)) {
          return "Zip Code must be exactly 6 digits";
        }
      }
      
      // Contact Person Mobile No.: validate format only when fully entered (10 digits) - required field
      if (field === "contactPersonMobileNo") {
        if (trimmedValue.length === 10 && !/^\d{10}$/.test(trimmedValue)) {
          return "Mobile No. must be exactly 10 digits";
        }
      }
      
      // Office No.: validate format only when fully entered (10 digits) - optional field
      if (field === "officeNo" && trimmedValue.length === 10 && !/^\d{10}$/.test(trimmedValue)) {
        return "Office No. must be exactly 10 digits";
      }
      
      // GST Number: validate format only when fully entered (15 characters) - Required field
      if (field === "gstNo" && trimmedValue.length === 15 && !/^[A-Z0-9]{15}$/.test(trimmedValue.toUpperCase())) {
        return "GST Number must be exactly 15 alphanumeric characters";
      }
      
      // Email validation (validate format whenever there's input)
      if (field === "emailId" && trimmedValue) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(trimmedValue)) {
          return "Please enter a valid email address";
        }
      }
    }
    
    return undefined;
  };

  validate = () => {
    const errors = {};
    
    // Validate required fields with format requirements
    if (!this.state.supplierName.trim()) {
      errors.supplierName = "Supplier Name is required";
    }
    if (!this.state.contactPersonName.trim()) {
      errors.contactPersonName = "Contact Person Name is required";
    }
    if (!this.state.contactPersonMobileNo.trim()) {
      errors.contactPersonMobileNo = "Contact Person Mobile No. is required";
    } else if (!/^\d{10}$/.test(this.state.contactPersonMobileNo.trim())) {
      errors.contactPersonMobileNo = "Mobile No. must be exactly 10 digits";
    }
    if (!this.state.addressLine1.trim()) {
      errors.addressLine1 = "Address line 1 is required";
    }
    if (!this.state.city.trim()) {
      errors.city = "City is required";
    }
    if (!this.state.state.trim()) {
      errors.state = "State is required";
    }
    if (!this.state.zipCode.trim()) {
      errors.zipCode = "Zip Code is required";
    } else if (!/^\d{6}$/.test(this.state.zipCode.trim())) {
      errors.zipCode = "Zip Code must be exactly 6 digits";
    }
    
    // Validate optional fields with format requirements
    if (this.state.officeNo && !/^\d{10}$/.test(this.state.officeNo.trim())) {
      errors.officeNo = "Office No. must be exactly 10 digits";
    }
if (this.state.gstNo && this.state.gstNo.trim() &&
    !/^[A-Z0-9]{15}$/.test(this.state.gstNo.trim().toUpperCase())) {
  errors.gstNo = "GST Number must be exactly 15 alphanumeric characters";
}
    if (this.state.emailId && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.state.emailId.trim())) {
      errors.emailId = "Please enter a valid email address";
    }

    this.setState({ errors });
    return Object.keys(errors).length === 0;
  };

  handleSave = async () => {
    if (!this.validate()) {
      return;
    }

    this.setState({ isSaving: true });

    // Prepare payload according to API structure
    const payload = {
      name: this.state.supplierName.trim(),
      contactType: "SUPPLIER",
      contactPerson: this.state.contactPersonName.trim(),
      contactPersonMobileNo: this.state.contactPersonMobileNo.trim(),
      addr_line1: this.state.addressLine1.trim(),
      addr_line2: this.state.addressLine2 ? this.state.addressLine2.trim() : "",
      city: this.state.city.trim(),
      state: this.state.state.trim(),
zip: this.state.zipCode.trim(),
      emailId: this.state.emailId ? this.state.emailId.trim() : "",
      gstNumber: this.state.gstNo ? this.state.gstNo.trim() : "",
      mobileNo: this.state.officeNo ? this.state.officeNo.trim() : "",
      accountName: this.state.accountName ? this.state.accountName.trim() : "",
      accountNumber: this.state.accountNumber ? this.state.accountNumber.trim() : "",
      bankName: this.state.bankName ? this.state.bankName.trim() : "",
      branchName: this.state.branchName ? this.state.branchName.trim() : "",
      ifscCode: this.state.ifscCode ? this.state.ifscCode.trim() : ""
    };

    try {
      const response = await API.POST(apiEndpoints.createContact, payload);

      if (response.success) {
        // Extract the created supplier data from response
        const createdSupplier = response.data;
        
        // Build address from available fields
        const addressParts = [];
        if (createdSupplier.addr_line1) addressParts.push(createdSupplier.addr_line1);
        if (createdSupplier.addr_line2) addressParts.push(createdSupplier.addr_line2);
        if (createdSupplier.city) addressParts.push(createdSupplier.city);
        if (createdSupplier.state) addressParts.push(createdSupplier.state);
        if (createdSupplier.zip) addressParts.push(createdSupplier.zip);
        
        // Fallback to form values if response doesn't have address
        if (addressParts.length === 0) {
          if (this.state.addressLine1) addressParts.push(this.state.addressLine1);
          if (this.state.addressLine2) addressParts.push(this.state.addressLine2);
          if (this.state.city) addressParts.push(this.state.city);
          if (this.state.state) addressParts.push(this.state.state);
          if (this.state.zipCode) addressParts.push(this.state.zipCode);
        }
        
        // Transform to match the format expected by parent component
        const supplier = {
          id: createdSupplier.contactId || createdSupplier.id,
          name: createdSupplier.name || this.state.supplierName,
          contactPerson: createdSupplier.contactPerson || this.state.contactPersonName,
          mobileNumber: createdSupplier.contactPersonMobileNo || this.state.contactPersonMobileNo,
          address: addressParts.length > 0 ? addressParts.join(", ") : null,
        };

// AFTER
this.props.enqueueSnackbar("Supplier created successfully", {
  variant: "success",
});

// Reset isSaving BEFORE calling handleClose so the guard doesn't block it
this.setState({ isSaving: false }, () => {
  this.handleClose();
  this.props.onSave(supplier);
});
      } else {
        this.props.enqueueSnackbar(
          response.errorMessage || "Failed to create supplier",
          {
            variant: "error",
          }
        );
        this.setState({ isSaving: false });
      }
    } catch (error) {
      this.props.enqueueSnackbar("An error occurred while creating supplier", {
        variant: "error",
      });
      this.setState({ isSaving: false });
    }
  };

  handleClose = () => {
    if (this.state.isSaving) {
      return; // Prevent closing while saving
    }
this.setState({
      supplierName: "",
      contactPersonName: "",
      officeNo: "",
      gstNo: "",
      contactPersonMobileNo: "",
      emailId: "",
      addressLine1: "",
      addressLine2: "",
      city: "",
      state: "",
      zipCode: "",
      accountName: "",
      accountNumber: "",
      bankName: "",
      branchName: "",
      ifscCode: "",
      errors: {},
      isSaving: false,
    });
    this.props.onClose();
  };

  render() {
    return (
      <Dialog
        open={this.props.open}
        onClose={this.handleClose}
        maxWidth="md"
        fullWidth
        classes={{ paper: "supplier-modal", root: "supplier-modal-backdrop" }}
      >
        <DialogTitle className="supplier-modal-title">
          <div className="modal-header">
            Add New Supplier
            <IconButton
              aria-label="close"
              onClick={this.handleClose}
              className="close-button"
            >
              <CloseIcon />
            </IconButton>
          </div>
        </DialogTitle>
        <DialogContent className="supplier-modal-content">
          <div className="supplier-form">
            <div className="form-row">
              <div className="form-column">
                <div className="supplier-field">
                  <label className="supplier-field-label">Supplier Name*</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.supplierName}
                    onChange={this.handleInputChange("supplierName")}
                    error={!!this.state.errors.supplierName}
                    helperText={this.state.errors.supplierName}
                    size="small"
                    inputProps={{ tabIndex: 1 }}
                  />
                </div>
                <div className="supplier-field">
                  <label className="supplier-field-label">Contact Person Name*</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.contactPersonName}
                    onChange={this.handleInputChange("contactPersonName")}
                    error={!!this.state.errors.contactPersonName}
                    helperText={this.state.errors.contactPersonName}
                    size="small"
                    inputProps={{ tabIndex: 3 }}
                  />
                </div>
                <div className="supplier-field">
                  <label className="supplier-field-label">Office No.</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.officeNo}
                    onChange={this.handleInputChange("officeNo")}
                    inputProps={{ maxLength: 10, pattern: "[0-9]*", inputMode: "numeric", tabIndex: 5 }}
                    error={!!this.state.errors.officeNo}
                    helperText={this.state.errors.officeNo}
                    size="small"
                  />
                </div>
                <div className="supplier-field">
                  <label className="supplier-field-label">Address line 1*</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.addressLine1}
                    onChange={this.handleInputChange("addressLine1")}
                    error={!!this.state.errors.addressLine1}
                    helperText={this.state.errors.addressLine1}
                    size="small"
                    inputProps={{ tabIndex: 7 }}
                  />
                </div>
              </div>
              <div className="form-column">
                <div className="supplier-field">
                  <label className="supplier-field-label">GST No.</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.gstNo}
                    onChange={this.handleInputChange("gstNo")}
                    inputProps={{ maxLength: 15, pattern: "[A-Z0-9]*", tabIndex: 2 }}
                    error={!!this.state.errors.gstNo}
                    helperText={this.state.errors.gstNo}
                    size="small"
                  />
                </div>
                <div className="supplier-field">
                  <label className="supplier-field-label">Contact Person Mobile No.*</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.contactPersonMobileNo}
                    onChange={this.handleInputChange("contactPersonMobileNo")}
                    inputProps={{ maxLength: 10, pattern: "[0-9]*", inputMode: "numeric", tabIndex: 4 }}
                    error={!!this.state.errors.contactPersonMobileNo}
                    helperText={this.state.errors.contactPersonMobileNo}
                    size="small"
                  />
                </div>
                <div className="supplier-field">
                  <label className="supplier-field-label">Email id</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    type="email"
                    value={this.state.emailId}
                    onChange={this.handleInputChange("emailId")}
                    error={!!this.state.errors.emailId}
                    helperText={this.state.errors.emailId}
                    size="small"
                    inputProps={{ tabIndex: 6 }}
                  />
                </div>
                <div className="supplier-field">
                  <label className="supplier-field-label">Address line 2</label>
                  <TextField
                    variant="outlined"
                    fullWidth
                    value={this.state.addressLine2}
                    onChange={this.handleInputChange("addressLine2")}
                    placeholder="Enter address line 2"
                    size="small"
                    inputProps={{ tabIndex: 8 }}
                  />
                </div>
              </div>
            </div>
            <div className="form-row form-row-city-state-zip">
              <div className="supplier-field">
                <label className="supplier-field-label">City*</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.city}
                  onChange={this.handleInputChange("city")}
                  placeholder="Enter City"
                  error={!!this.state.errors.city}
                  helperText={this.state.errors.city}
                  size="small"
                  inputProps={{ tabIndex: 9 }}
                />
              </div>
              <div className="supplier-field">
                <label className="supplier-field-label">State*</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.state}
                  onChange={this.handleInputChange("state")}
                  placeholder="Enter State"
                  error={!!this.state.errors.state}
                  helperText={this.state.errors.state}
                  size="small"
                  inputProps={{ tabIndex: 10 }}
                />
              </div>
              <div className="supplier-field">
                <label className="supplier-field-label">Zip Code*</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.zipCode}
                  onChange={this.handleInputChange("zipCode")}
                  placeholder="Enter Zip Code"
                  inputProps={{ maxLength: 6, pattern: "[0-9]*", inputMode: "numeric", tabIndex: 11 }}
                  error={!!this.state.errors.zipCode}
                  helperText={this.state.errors.zipCode}
                  size="small"
                />
              </div>
              
            </div>
            {/* Bank Details */}
          <div className="form-row">
            <div className="form-column">
              <div className="supplier-field">
                <label className="supplier-field-label">Account Holder Name</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.accountName}
                  onChange={this.handleInputChange("accountName")}
                  error={!!this.state.errors.accountName}
                  helperText={this.state.errors.accountName}
                  size="small"
                  inputProps={{ maxLength: 100, tabIndex: 12 }}
                />
              </div>
              <div className="supplier-field">
                <label className="supplier-field-label">Bank Name</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.bankName}
                  onChange={this.handleInputChange("bankName")}
                  size="small"
                  inputProps={{ maxLength: 50, tabIndex: 14 }}
                />
              </div>
              <div className="supplier-field">
                <label className="supplier-field-label">IFSC Code</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.ifscCode}
                  onChange={this.handleInputChange("ifscCode")}
                  error={!!this.state.errors.ifscCode}
                  helperText={this.state.errors.ifscCode || "e.g. KKBK0000561"}
                  size="small"
                  inputProps={{ maxLength: 11, tabIndex: 16 }}
                />
              </div>
            </div>
            <div className="form-column">
              <div className="supplier-field">
                <label className="supplier-field-label">Account Number</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.accountNumber}
                  onChange={this.handleInputChange("accountNumber")}
                  error={!!this.state.errors.accountNumber}
                  helperText={this.state.errors.accountNumber}
                  size="small"
                  inputProps={{ maxLength: 18, pattern: "[0-9]*", inputMode: "numeric", tabIndex: 13 }}
                />
              </div>
              <div className="supplier-field">
                <label className="supplier-field-label">Branch Name</label>
                <TextField
                  variant="outlined"
                  fullWidth
                  value={this.state.branchName}
                  onChange={this.handleInputChange("branchName")}
                  size="small"
                  inputProps={{ maxLength: 50, tabIndex: 15 }}
                />
              </div>
            </div>
          </div>
        </div>
          
        </DialogContent>
        <DialogActions className="supplier-modal-actions">
          <Button 
            onClick={this.handleClose} 
            color="default"
            disabled={this.state.isSaving}
          >
            Cancel
          </Button>
          <Button 
            onClick={this.handleSave} 
            color="primary" 
            variant="contained"
            disabled={this.state.isSaving}
            startIcon={this.state.isSaving ? <CircularProgress size={16} /> : null}
          >
            {this.state.isSaving ? "Saving..." : "Save"}
          </Button>
        </DialogActions>
      </Dialog>
    );
  }
}

export default withSnackbar(AddNewSupplierModal);
