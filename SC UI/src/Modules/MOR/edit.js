//react
import React from "react";
import { connect } from "react-redux";
//third party
import { withSnackbar } from "notistack";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages, constants } from "./../../messages";
//style
import "./style.scss";
import EditForm from "./../../Shared/EditForm";
import moment from "moment";
class Edit extends EditForm {
  updateUrl = apiEndpoints.individualMOR;

  state = { isLoaded: false };
  oldData = {};
  title = messages.common.mor;
  radioOptions = [
    { label: "Daily mode", value: "Daily" },
    { label: "Hourly mode", value: "Hourly" },
    { label: "Meter Reading Mode", value: "MeterReading" },
    { label: "Trip Count Mode", value: "TripCount" },
  ];
  componentDidMount() {
    this.search();
    this.updateUrl = this.updateUrl + this.props.id;
  }
  async search() {
    const response = await API.GET(apiEndpoints.individualMOR + this.props.id);
    if (response.success) {
      const data = response.data;

      this.formData.machineryId = data.machinery.machineryId;
      if (data.supplier) {
        this.formData.supplierId = data.supplier.contactId;
      }
      if (data.contractor) {
        this.formData.contractorId = data.contractor.contactId;
      }
      if (data.usageLocation) {
        this.formData.locationId = data.usageLocation.loationId;
      }
      this.formData.date = data.date;
      this.formData.mode = data.mode;
      this.formData.startDate = data.startDate;
      this.formData.endDate = data.endDate;
      this.formData.startDateTime = data.startDateTime;
      this.formData.endDateTime = data.endDateTime;

      this.formData.initialMeterReading = data.initialMeterReading;
      this.formData.endMeterReading = data.endMeterReading;
      this.formData.noOfTrips = data.noOfTrips;
      this.formData.amountCharged = data.amountCharged;
      this.formData.rate = data.rate;
      this.formData.vehicleNo = data.vehicleNo;
      this.formData.additionalNotes = data.additionalNotes;
      this.formData.fileInformations = data.fileInformations;
      this.formData.mrnGrn = data.mrnGrn;

      this.setState({
        isLoaded: true,
      });
    }
  }
  async update(event) {
    event.preventDefault();

    if (
      !this.formData.fileInformations ||
      this.formData.fileInformations.length === 0
    ) {
      this.props.enqueueSnackbar("Add atleast one file", {
        variant: "error",
      });
      return;
    }
    super.update(event);
  }
  calculateDateTimeDifference() {
    let difference = 0;
    if (this.formData["startDateTime"] && this.formData["endDateTime"]) {
      const sDate = moment(
        this.formData["startDateTime"],
        constants.dateTimeFormat
      );
      const eDate = moment(
        this.formData["endDateTime"],
        constants.dateTimeFormat
      );
      difference = Math.round(eDate.diff(sDate, "hours"));
    }
    this.setState({ dateTimeDiff: difference }, () => this.calculateAmount());
  }
  calculateDateDifference() {
    let difference = 0;
    if (this.formData["startDate"] && this.formData["endDate"]) {
      const sDate = moment(this.formData["startDate"], constants.dateFormat);
      const eDate = moment(this.formData["endDate"], constants.dateFormat);
      difference = Math.round(eDate.diff(sDate, "days"));
      difference = difference + 1;
    }

    this.setState({ dateDiff: difference }, () => this.calculateAmount());
  }
  calculateMeterDifference() {
    let difference = 0;
    if (
      this.formData["initialMeterReading"] &&
      this.formData["endMeterReading"]
    ) {
      difference =
        this.formData["endMeterReading"] - this.formData["initialMeterReading"];
    }
    this.setState({ meterDiff: difference }, () => this.calculateAmount());
  }
  calculateAmount() {
    const rate = this.formData["rate"];
    let diff = 0;
    if (this.formData.mode === "Daily") {
      diff = this.state.dateDiff;
    } else if (this.formData.mode === "Hourly") {
      diff = this.state.dateTimeDiff;
    } else if (this.formData.mode === "MeterReading") {
      diff = this.state.meterDiff;
    } else if (this.formData.mode === "TripCount") {
      diff = this.formData["noOfTrips"];
    }
    this.setState({ amount: rate * diff });
    this.formData["amountCharged"] = rate * diff;
  }
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
            <div className="flex width50">
              {this.renderDate({
                fieldname: "date",
                label: messages.fields.date,
                disabled: true,
                maxDate: moment(),
                //minDate: moment(this.formData.date).add(-3, 'd'),
              })}
              {this.renderAutoComplete({
                fieldname: "machineryId",
                placeholder: "Machinery Name",
                options: this.props.dropdowns?.machinery || [],
                disableClearable: true,
                required: true,
                disabled: true,
                getOption: (option) => {
                  return option["name"];
                },
              })}
            </div>
            <div className="flex width50">
              {this.renderAutoComplete({
                fieldname: "supplierId",
                placeholder: "Supplier Name",
                options: this.props.dropdowns?.supplier || [],
                disabled: true,
                getOption: (option) => {
                  return option["name"];
                },
              })}
              {this.renderAutoComplete({
                fieldname: "contractorId",
                placeholder: "Contractor",
                disabled: true,
                options: this.props.dropdowns?.contractor || [],
                getOption: (option) => {
                  return option["name"];
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData.contractorId = value.id;
                  }
                },
              })}
              {this.renderAutoComplete({
                fieldname: "locationId",
                placeholder: messages.common.location,
                options: this.props.dropdowns?.usagelocation || [],
                getOption: (option) => {
                  return option["name"];
                },
              })}
            </div>
            <div className="flex">
              {this.renderRadio({
                label: "Mode",
                options: this.radioOptions,
                fieldname: "mode",
                defaultValue: this.formData.mode,
                classname: "mode-radio",
                onChange: () => {
                  this.setState({ i: 1 });
                },
              })}
              <div className="custom-mor-options">
                <div class="width30">
                  {this.renderDate({
                    fieldname: "startDate",
                    label: "Start Date",
                    disabled: !(this.formData.mode === "Daily"),
                    onChange: () => this.calculateDateDifference(),
                  })}
                  {this.renderDate({
                    fieldname: "endDate",
                    label: "End Date",
                    disabled: !(this.formData.mode === "Daily"),
                    onChange: () => this.calculateDateDifference(),
                  })}
                  {this.renderTextField({
                    fieldname: "dateDifference",
                    placeholder: "Date Difference",
                    type: "number",
                    disabled: true,
                    value: this.state.dateDiff,
                    skipAdd: true,
                  })}
                </div>
                <div class="width30">
                  {this.renderDateTime({
                    fieldname: "startDateTime",
                    label: "Start Date",
                    disabled: !(this.formData.mode === "Hourly"),
                    onChange: () => this.calculateDateTimeDifference(),
                  })}
                  {this.renderDateTime({
                    fieldname: "endDateTime",
                    label: "End Date",
                    disabled: !(this.formData.mode === "Hourly"),
                    onChange: () => this.calculateDateTimeDifference(),
                  })}
                  {this.renderTextField({
                    fieldname: "dateDifference",
                    placeholder: "Hour Difference",
                    type: "number",
                    disabled: true,
                    value: this.state.dateTimeDiff,
                    skipAdd: true,
                  })}
                </div>
                <div className="width30">
                  {this.renderTextField({
                    fieldname: "initialMeterReading",
                    placeholder: "Initial Meter Reading",
                    type: "number",
                    disabled: !(this.formData.mode === "MeterReading"),
                    required: true,
                    onChange: () => this.calculateMeterDifference(),
                  })}
                  {this.renderTextField({
                    fieldname: "endMeterReading",
                    placeholder: "End Meter Reading",
                    type: "number",
                    disabled: !(this.formData.mode === "MeterReading"),
                    required: true,
                    onChange: () => this.calculateMeterDifference(),
                  })}
                  {this.renderTextField({
                    fieldname: "dateDifference",
                    placeholder: "Meter Difference",
                    type: "number",
                    disabled: true,
                    skipAdd: true,
                    value: this.state.meterDiff,
                  })}
                </div>
                {this.renderTextField({
                  fieldname: "noOfTrips",
                  placeholder: "No Of Trips",
                  type: "number",
                  disabled: !(this.formData.mode === "TripCount"),
                  required: true,
                })}
              </div>
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "rate",
                placeholder: "Rate",
                type: "number",
                onChange: () => {
                  this.calculateAmount();
                },
              })}
              {this.renderTextField({
                fieldname: "amountCharged",
                placeholder: "Amount Charged",
                type: "number",
                disabled: true,
                value: this.state.amount,
              })}

              {this.renderTextField({
                fieldname: "vehicleNo",
                placeholder: "Vehicle No",
              })}
              {this.renderTextField({
              fieldname: "mrnGrn",
              placeholder: "MRN / GRN",
              validation: "maxlength",
              lengthConstraint: 20,
              errorMessage: messages.common.max20,
            })}
            </div>
            <div class="flex">
              {this.renderTextArea({
                fieldname: "additionalNotes",
                placeholder: "Description",
              })}
            </div>
            {this.renderFileArea()}
            
            {this.renderFooter()}
          </form>
        )}
      </div>
    );
  }
}
const mapStateToProps = (state) => {
  return {
    roles: state.roles.roles,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
