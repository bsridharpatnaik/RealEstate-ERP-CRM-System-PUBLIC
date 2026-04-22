import React, { Component } from "react";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import TF from "./../../../../Shared/TextField";
import Date from "./../../../../Shared/Date/";
import Button from "./../../../../Shared/Button";
import Select from "./../../../../Shared/Select";
import { withSnackbar } from "notistack";
import "./style.scss";

class Property extends Component {

  data = {
    dealId: 0,
    propertytypeId: '',
    propertyNameId: '',
    bookingDate: "",
    dealAmount: "",
    supplementAmount: "",
    loanAmount: "",
    loanStatus: null,
    customerStatus: null,
    loanRequired: true,
    bankName: '',
    details: '',
  };
  state = {
    isLoading: true,
    formValid: false,
    propertyTypes: [],
    propertyNames: [],
    isSaveClicked: false,
    loanList: [{ name: 'Yes', id: true }, { name: 'No', id: false }],
    loanStatuses: ['DocumentCollection', 'ApplicationSubmitted', 'ApplicationProcessing', 'Approved', 'Disbursed'],
    customerStatuses: ['Booked', 'DocumentCollection', 'Agreement', 'Finance', 'Registry', 'Handover'],
    loanRequired: true,
  };
  async componentDidMount() {
    let isloanRequired = true;
    if (this.props.isEdit) {
      this.data = {
        dealId: this.props.currentProperty.dealId,
        propertytypeId: this.props.currentProperty.propertytypeId,
        propertyNameId: this.props.currentProperty.propertyNameId,
        bookingDate: this.props.currentProperty.bookingDate,
        dealAmount: this.props.currentProperty.dealAmount,
        supplementAmount: this.props.currentProperty.supplementAmount,
        loanRequired: this.props.currentProperty.loanRequired,
        loanAmount: this.props.currentProperty.loanAmount,
        loanStatus: this.props.currentProperty.loanStatus,
        customerStatus: this.props.currentProperty.customerStatus,
        bankName: this.props.currentProperty.bankName,
        details: this.props.currentProperty.details,
      };
      isloanRequired = this.props.currentProperty.loanRequired;
    }
    this.setState({ isLoading: true, loanRequired: isloanRequired });
    const response = await API.GET(apiEndpoints.getPropertyTypes);
    if (response.success) {
      this.setState({ propertyTypes: response.data });
      if (this.data.propertytypeId !== 0) {
        this.getPropertyNames(true);
      }
      this.setState({ isLoading: false });
    }
  }

  async getPropertyNames(isconcat = false) {
    const response = await API.GET(apiEndpoints.getPropertyName.replace('{propertyTypeId}', this.data.propertytypeId))
    if (response.success) {
      if (isconcat) {
        const propertyNames = response.data.concat({ name: this.props.currentProperty.propertyName, id: this.props.currentProperty.propertyNameId });
        this.setState({ propertyNames: propertyNames });
      }
      else {
        this.setState({ propertyNames: response.data });
      }
    }
    this.setState({ isLoading: false });
  }

  submit = async () => {
    this.setState({ isSaveClicked: true });
    const params = {
      ...this.data,
      propertyTypeId: this.data.propertytypeId,
      propertyId: this.data.propertyNameId,
      leadId: this.props.leadId,
    };
    let responseSuccess = false;
    let ErrorMessage = '';
    if (this.data.dealId > 0) {
      const response = await API.PUT(apiEndpoints.updateDealStructure.replace('{dealId}', this.data.dealId), params);
      responseSuccess = response.success;
      ErrorMessage = response.errorMessage;
    }
    else {
      const response = await API.POST(apiEndpoints.createDealStructure, params);
      responseSuccess = response.success;
      ErrorMessage = response.errorMessage;
    }
    if (responseSuccess) {
      this.props.refresh();
      this.props.enqueueSnackbar("Property saved Successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(ErrorMessage, {
        variant: "error",
      });
    }
  };

  formValid = () => {
    const formValid1 =
      this.data.propertytypeId > 0 &&
      this.data.propertyNameId > 0 &&
      this.data.bookingDate.length &&
      this.data.details.length &&
      this.data.dealAmount > 0;
    const formValidwithLoan = this.data.loanAmount > 0 && (this.data.bankName && this.data.bankName.length) && (this.data.loanStatus && this.data.loanStatus.length);
    const formValid = this.data.loanRequired ? (formValid1 && formValidwithLoan) : formValid1;
    this.setState({ formValid });
  };

  render() {
    return !this.state.isLoading && (
      <div className="activity-tab">
        <div className="flex width30">
          <Date
            onChange={(e) => {
              this.data.bookingDate = e;
              this.formValid();
            }}
            label="Booking Date"
            fieldname={"bookingDate"}
            dontSetDefault={true}
            defaultValue={this.data.bookingDate}
          />
          <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
            <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
              Property Type
            </label>
            <Select
              label="Property Type"
              name={"propertytypeId"}
              options={this.state.propertyTypes || []}
              labelKey={'name'}
              valueKey={'id'}
              defaultValue={this.data.propertytypeId}
              onChange={(event) => {
                this.data.propertytypeId = event.target.value;
                this.getPropertyNames();
                this.formValid();
              }}
            />
          </div>
          <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
            <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
              Property Name
            </label>
            <Select
              label="Property Name"
              name={"propertyNameId"}
              labelKey={'name'}
              valueKey={'id'}
              options={this.state.propertyNames || []}
              defaultValue={this.data.propertyNameId}
              //disabled={this.props.currentProperty && this.props.currentProperty.propertyNameId > 0}
              onChange={(event) => {
                this.data.propertyNameId = event.target.value;
                this.formValid();
              }}
            />
          </div>
        </div>
        <div className="flex width30">
          <TF
            placeholder="Deal Amount"
            fieldname="dealAmount"
            onChange={(event) => {
              this.data.dealAmount = event;
              this.formValid();
            }}
            type='number'
            validation={"nonnegative"}
            value={this.data.dealAmount}
          />
          <TF
            placeholder="Supplement Amount"
            fieldname="supplementAmount"
            onChange={(event) => {
              this.data.supplementAmount = event;
              this.formValid();
            }}
            type='number'
            validation={"nonnegative"}
            value={this.data.supplementAmount}
          />
          <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
            <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
              Loan Required
            </label>
            <Select
              label="Loan Required"
              name={"loanRequired"}
              options={this.state.loanList || []}
              labelKey={'name'}
              valueKey={'id'}
              defaultValue={this.data.loanRequired}
              onChange={(event) => {
                this.data.loanRequired = event.target.value;
                this.setState({ loanRequired: event.target.value ? true : false });
                this.formValid();

              }}
            />
          </div>
        </div>
        {this.state.loanRequired && (
          <div className="flex width30">
            <TF
              placeholder="Loan Amount"
              fieldname="loanAmount"
              onChange={(event) => {
                this.data.loanAmount = event;
                this.formValid();
              }}
              type='number'
              validation={"nonnegative"}
              value={this.data.loanAmount}
            />
            <TF
              placeholder="Bank Name"
              fieldname="bankName"
              onChange={(event) => {
                this.data.bankName = event;
                this.formValid();
              }}
              type='text'
              value={this.data.bankName}
            />
            <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
              <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
                Loan Status
            </label>
              <Select
                label="Loan Status"
                name={"loanStatus"}
                options={this.state.loanStatuses || []}
                labelKey={'name'}
                valueKey={'name'}
                defaultValue={this.data.loanStatus}
                onChange={(event) => {
                  this.data.loanStatus = event.target.value;
                  this.formValid();
                }}
              />
            </div>
          </div>)
        }
        <div className="flex">
          <div className="left" style={{display: 'flex'}}>
            <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal" style={{ width: '50%' }}>
              <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
                Customer Status
            </label>
              <Select
                label="Customer Status"
                name={"customerStatus"}
                options={this.state.customerStatuses || []}
                labelKey={'name'}
                valueKey={'name'}
                defaultValue={this.data.customerStatus}
                onChange={(event) => {
                  this.data.customerStatus = event.target.value;
                  this.formValid();
                }}
              />
            </div>
            <div  style={{ width: '50%' }}>
              <TF
                placeholder="Details"
                multiline={true}
                fieldname="details"
                onChange={(event) => {
                  this.data.details = event;
                  this.formValid();
                }}
                validation={"maxlength"}
                lengthConstraint={150}
                value={this.data.details}
              />
            </div>
          </div>
          <div className="right">
            <div className="empty-section">
              &nbsp;
                </div>
            <div className="button-section">
              <Button
                type="submit"
                buttonClass="blue"
                label={this.props.isEdit ? "UPDATE" : "ADD"}
                onClick={() => this.submit()}
                disabled={!this.state.formValid || this.state.isSaveClicked}
              />
              <Button
                type="button"
                buttonClass="grey"
                label="CANCEL"
                onClick={() => this.props.cancel()}
              />
            </div>
          </div>
        </div>
      </div>
    )
  }



}

export default withSnackbar(Property)