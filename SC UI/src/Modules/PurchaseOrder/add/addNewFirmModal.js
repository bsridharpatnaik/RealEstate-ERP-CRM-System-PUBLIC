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

class AddNewFirmModal extends React.Component {
state = {
    // mode
    isEditMode: false,
    editFirmId: null,
    // fields
    firmName: "",
    firmDescription: "",
    firmGstNumber: "",
    firmPanNumber: "",
    firmEmail: "",
    contactPerson: "",
    contactPersonMobileNo: "",
    firmContactNumber: "",       // comma-separated e.g. "9876543210,9123456789"
    addr_line1: "",
    addr_line2: "",
    city: "",
    state: "",
    zip: "",
    errors: {},
    isSaving: false,
  };

componentDidMount() {
  if (this.props.innerRef) {
    this.props.innerRef(this);
  }
}

componentDidUpdate(prevProps) {
  // When modal opens with an editFirmId, load the firm
  if (this.props.open && !prevProps.open && this.props.editFirmId) {
    this.loadFirmForEdit(this.props.editFirmId);
  }

  // When modal closes, reset state
  if (!this.props.open && prevProps.open) {
    this.resetState();
  }
}
resetState = () => {
  this.setState({
    isEditMode: false,
    editFirmId: null,
    firmName: "",
    firmDescription: "",
    firmGstNumber: "",
    firmPanNumber: "",
    firmEmail: "",
    contactPerson: "",
    contactPersonMobileNo: "",
    firmContactNumber: "",
    addr_line1: "",
    addr_line2: "",
    city: "",
    state: "",
    zip: "",
    errors: {},
    isSaving: false,
  });
};

handleInputChange = (field) => (event) => {
    let value = event.target.value;

    // GST: uppercase alphanumeric, max 15
    if (field === "firmGstNumber") {
      value = value.replace(/[^A-Za-z0-9]/g, "").toUpperCase();
      if (value.length > 15) value = value.slice(0, 15);
    }

    // PAN: uppercase alphanumeric, max 10
    if (field === "firmPanNumber") {
      value = value.replace(/[^A-Za-z0-9]/g, "").toUpperCase();
      if (value.length > 10) value = value.slice(0, 10);
    }

    // Contact Person Mobile: digits only, max 10
    if (field === "contactPersonMobileNo") {
      value = value.replace(/\D/g, "");
      if (value.length > 10) value = value.slice(0, 10);
    }

    // Firm Contact Number: digits and comma only, max 21 (10 + comma + 10)
    if (field === "firmContactNumber") {
      value = value.replace(/[^0-9,]/g, "");
      if (value.length > 21) value = value.slice(0, 21);
    }

    // ZIP: digits only, max 6
    if (field === "zip") {
      value = value.replace(/\D/g, "");
      if (value.length > 6) value = value.slice(0, 6);
    }

    this.setState({
      [field]: value,
      errors: {
        ...this.state.errors,
        [field]: this.getFieldError(field, value),
      },
    });
  };

  getFieldError = (field, value) => {
      const trimmedValue = value ? value.trim() : "";

      const requiredFields = {
        firmName: "Firm Name is required",
        addr_line1: "Address Line 1 is required",
        city: "City is required",
        state: "State is required",
        zip: "Zip Code is required",
        firmContactNumber: "Contact Number is required",
        firmEmail: "Email is required",
      };

      if (requiredFields[field] && !trimmedValue) {
        return requiredFields[field];
      }

      if (trimmedValue) {
        if (field === "zip" && trimmedValue.length === 6 && !/^\d{6}$/.test(trimmedValue)) {
          return "Zip Code must be exactly 6 digits";
        }
        if (field === "contactPersonMobileNo" && trimmedValue.length === 10 && !/^\d{10}$/.test(trimmedValue)) {
          return "Mobile No. must be exactly 10 digits";
        }
        if (field === "firmGstNumber" && trimmedValue.length === 15 && !/^[A-Z0-9]{15}$/.test(trimmedValue)) {
          return "GST Number must be exactly 15 alphanumeric characters";
        }
        if (field === "firmPanNumber" && trimmedValue.length === 10) {
          if (!/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(trimmedValue)) {
            return "PAN must be in format: ABCDE1234F";
          }
        }
        if (field === "firmContactNumber") {
          const parts = trimmedValue.split(",").map(p => p.trim());
          if (parts.length > 2) return "Maximum 2 contact numbers allowed";
          for (const p of parts) {
            if (!/^\d{10}$/.test(p)) return "Each number must be exactly 10 digits";
          }
          if (parts.length === 2 && parts[0] === parts[1]) return "Duplicate numbers not allowed";
        }
        if (field === "firmEmail") {
          if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedValue)) return "Invalid email address";
        }
      }

      return undefined;
    };

  validate = () => {
      const errors = {};

      if (!this.state.firmName.trim()) errors.firmName = "Firm Name is required";
      if (!this.state.addr_line1.trim()) errors.addr_line1 = "Address Line 1 is required";
      if (!this.state.city.trim()) errors.city = "City is required";
      if (!this.state.state.trim()) errors.state = "State is required";
      if (!this.state.zip.trim()) errors.zip = "Zip Code is required";
      if (!this.state.firmContactNumber.trim()) errors.firmContactNumber = "Contact Number is required";
      if (!this.state.firmEmail.trim()) errors.firmEmail = "Email is required";
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.state.firmEmail.trim())) errors.firmEmail = "Invalid email address";

      if (this.state.firmGstNumber && !/^[A-Z0-9]{15}$/.test(this.state.firmGstNumber.trim())) {
        errors.firmGstNumber = "GST Number must be exactly 15 alphanumeric characters";
      }
      if (this.state.firmPanNumber) {
        const pan = this.state.firmPanNumber.trim();
        if (pan.length !== 10 || !/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan)) {
          errors.firmPanNumber = "PAN must be in format: ABCDE1234F";
        }
      }
      if (this.state.firmContactNumber) {
        const parts = this.state.firmContactNumber.trim().split(",").map(p => p.trim());
        if (parts.length > 2) errors.firmContactNumber = "Maximum 2 contact numbers allowed";
        else {
          for (const p of parts) {
            if (!/^\d{10}$/.test(p)) { errors.firmContactNumber = "Each number must be exactly 10 digits"; break; }
          }
          if (parts.length === 2 && parts[0] === parts[1]) errors.firmContactNumber = "Duplicate numbers not allowed";
        }
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
      firmName: this.state.firmName.trim(),
      firmDescription: this.state.firmDescription ? this.state.firmDescription.trim() : "",
      firmGstNumber: this.state.firmGstNumber ? this.state.firmGstNumber.trim() : "",
      firmPanNumber: this.state.firmPanNumber ? this.state.firmPanNumber.trim() : "",
      firmEmail: this.state.firmEmail ? this.state.firmEmail.trim() : "",
      contactPerson: this.state.contactPerson ? this.state.contactPerson.trim() : "",
      contactPersonMobileNo: this.state.contactPersonMobileNo ? this.state.contactPersonMobileNo.trim() : "",
      firmContactNumber: this.state.firmContactNumber ? this.state.firmContactNumber.trim() : "",
      addr_line1: this.state.addr_line1.trim(),
      addr_line2: this.state.addr_line2 ? this.state.addr_line2.trim() : "",
      city: this.state.city.trim(),
      state: this.state.state.trim(),
      zip: this.state.zip.trim(),
    };

try {
      const isEdit = this.state.isEditMode && this.state.editFirmId;
      const response = isEdit
        ? await API.PUT(apiEndpoints.updateFirm + this.state.editFirmId, payload)
        : await API.POST(apiEndpoints.createFirm, payload);

      if (response.success) {
        // Extract the created firm data from response
        const createdFirm = response.data;
        
        // Transform to match the format expected by parent component
        const firm = {
          id: createdFirm.firmId || createdFirm.id,
          name: createdFirm.firmName || this.state.firmName,
          address: createdFirm.firmAddress || this.state.firmAddress,
        };

        this.props.enqueueSnackbar(this.state.isEditMode ? "Firm updated successfully" : "Firm created successfully", {
                  variant: "success",
                });

                // Reset isSaving BEFORE calling handleClose so the guard doesn't block it
                this.setState({ isSaving: false }, () => {
                  this.handleClose();
                  this.props.onSave(firm);
                });
      } else {
        this.props.enqueueSnackbar(
          response.errorMessage || "Failed to create firm",
          {
            variant: "error",
          }
        );
        this.setState({ isSaving: false });
      }
    } catch (error) {
      this.props.enqueueSnackbar("An error occurred while creating firm", {
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
      isEditMode: false,
      editFirmId: null,
      firmName: "",
      firmDescription: "",
      firmGstNumber: "",
      firmPanNumber: "",
      firmEmail: "",
      contactPerson: "",
      contactPersonMobileNo: "",
      firmContactNumber: "",
      addr_line1: "",
      addr_line2: "",
      city: "",
      state: "",
      zip: "",
      errors: {},
      isSaving: false,
    });
    this.props.onClose();
  };

loadFirmForEdit = async (firmId) => {
    try {
      const response = await API.GET(apiEndpoints.getFirmDetail + firmId);
      if (response.success && response.data) {
        const f = response.data;
        this.setState({
          isEditMode: true,
          editFirmId: firmId,
          firmName: f.firmName || "",
          firmDescription: f.firmDescription || "",
          firmGstNumber: f.firmGstNumber || "",
          firmPanNumber: f.firmPanNumber || "",
          firmEmail: f.firmEmail || "",
          contactPerson: f.contactPerson || "",
          contactPersonMobileNo: f.contactPersonMobileNo || "",
          firmContactNumber: f.firmContactNumber || "",
          addr_line1: f.addr_line1 || "",
          addr_line2: f.addr_line2 || "",
          city: f.city || "",
          state: f.state || "",
          zip: f.zip || "",
          errors: {},
        });
      }
    } catch (e) {
      this.props.enqueueSnackbar("Failed to load firm details", { variant: "error" });
    }
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
            {this.state.isEditMode ? "Edit Firm" : "Add New Firm"}
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
                          <label className="supplier-field-label">Firm Name*</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.firmName}
                            onChange={this.handleInputChange("firmName")}
                            error={!!this.state.errors.firmName}
                            helperText={this.state.errors.firmName}
                            inputProps={{ tabIndex: 1 }}
                          />
                        </div>
                        <div className="supplier-field">
                          <label className="supplier-field-label">Contact Person</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.contactPerson}
                            onChange={this.handleInputChange("contactPerson")}
                            inputProps={{ tabIndex: 3 }}
                          />
                        </div>
                        <div className="supplier-field">
                          <label className="supplier-field-label">GST Number</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.firmGstNumber}
                            onChange={this.handleInputChange("firmGstNumber")}
                            error={!!this.state.errors.firmGstNumber}
                            helperText={this.state.errors.firmGstNumber}
                            inputProps={{ maxLength: 15, tabIndex: 5 }}
                          />
                        </div>
                        <div className="supplier-field">
                          <label className="supplier-field-label">Email*</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.firmEmail}
                            onChange={this.handleInputChange("firmEmail")}
                            error={!!this.state.errors.firmEmail}
                            helperText={this.state.errors.firmEmail}
                            inputProps={{ tabIndex: 7 }}
                          />
                        </div>
                      </div>
                      <div className="form-column">
                        <div className="supplier-field">
                          <label className="supplier-field-label">Contact Number* (comma-separate two numbers)</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.firmContactNumber}
                            onChange={this.handleInputChange("firmContactNumber")}
                            placeholder="e.g. 9876543210,9123456789"
                            helperText={this.state.errors.firmContactNumber}
                            inputProps={{ maxLength: 21, tabIndex: 2 }}
                          />
                        </div>
                        <div className="supplier-field">
                          <label className="supplier-field-label">Contact Person Mobile No.</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.contactPersonMobileNo}
                            onChange={this.handleInputChange("contactPersonMobileNo")}
                            error={!!this.state.errors.contactPersonMobileNo}
                            helperText={this.state.errors.contactPersonMobileNo}
                            inputProps={{ maxLength: 10, pattern: "[0-9]*", inputMode: "numeric", tabIndex: 4 }}
                          />
                        </div>
                        <div className="supplier-field">
                          <label className="supplier-field-label">PAN Number</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.firmPanNumber}
                            onChange={this.handleInputChange("firmPanNumber")}
                            error={!!this.state.errors.firmPanNumber}
                            helperText={this.state.errors.firmPanNumber}
                            inputProps={{ maxLength: 10, tabIndex: 6 }}
                          />
                        </div>
                        <div className="supplier-field">
                          <label className="supplier-field-label">Firm Description</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.firmDescription}
                            onChange={this.handleInputChange("firmDescription")}
                            inputProps={{ tabIndex: 8 }}
                          />
                        </div>
                      </div>
                    </div>
                    {/* Address */}
                    <div className="form-row">
                      <div className="form-column">
                        <div className="supplier-field">
                          <label className="supplier-field-label">Address Line 1*</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.addr_line1}
                            onChange={this.handleInputChange("addr_line1")}
                            error={!!this.state.errors.addr_line1}
                            helperText={this.state.errors.addr_line1}
                            inputProps={{ tabIndex: 9 }}
                          />
                        </div>
                      </div>
                      <div className="form-column">
                        <div className="supplier-field">
                          <label className="supplier-field-label">Address Line 2</label>
                          <TextField
                            variant="outlined" fullWidth size="small"
                            value={this.state.addr_line2}
                            onChange={this.handleInputChange("addr_line2")}
                            inputProps={{ tabIndex: 10 }}
                          />
                        </div>
                      </div>
                    </div>
                    <div className="form-row form-row-city-state-zip">
                      <div className="supplier-field">
                        <label className="supplier-field-label">City*</label>
                        <TextField
                          variant="outlined" fullWidth size="small"
                          value={this.state.city}
                          onChange={this.handleInputChange("city")}
                          error={!!this.state.errors.city}
                          helperText={this.state.errors.city}
                          inputProps={{ tabIndex: 11 }}
                        />
                      </div>
                      <div className="supplier-field">
                        <label className="supplier-field-label">State*</label>
                        <TextField
                          variant="outlined" fullWidth size="small"
                          value={this.state.state}
                          onChange={this.handleInputChange("state")}
                          error={!!this.state.errors.state}
                          helperText={this.state.errors.state}
                          inputProps={{ tabIndex: 12 }}
                        />
                      </div>
                      <div className="supplier-field">
                        <label className="supplier-field-label">Zip Code*</label>
                        <TextField
                          variant="outlined" fullWidth size="small"
                          value={this.state.zip}
                          onChange={this.handleInputChange("zip")}
                          error={!!this.state.errors.zip}
                          helperText={this.state.errors.zip}
                          inputProps={{ maxLength: 6, pattern: "[0-9]*", inputMode: "numeric", tabIndex: 13 }}
                        />
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
            {this.state.isSaving ? "Saving..." : this.state.isEditMode ? "Update" : "Save"}
          </Button>
        </DialogActions>
      </Dialog>
    );
  }
}

export default withSnackbar(AddNewFirmModal);
