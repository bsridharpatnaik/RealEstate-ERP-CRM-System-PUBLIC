//react
import React from "react";
import { connect } from "react-redux";
//third party
import AddForm from "./../../Shared/AddForm";
import { withSnackbar } from "notistack";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages, constants } from "./../../messages";
//style
import "./style.scss";
import moment from "moment";
import { getRoleEditConstraintDays } from "./../../helper";

class Add extends AddForm {
  title = messages.common.mor;
  addurl = apiEndpoints.createMOR;
  constructor(props) {
    super(props);
    this.formData.mode = "Daily";
  }
  radioOptions = [
    { label: "Daily mode", value: "Daily" },
    { label: "Hourly mode", value: "Hourly" },
    { label: "Meter Reading Mode", value: "MeterReading" },
    { label: "Trip Count Mode", value: "TripCount" },
  ];
  async add(event) {
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

    super.add(event);
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
    const days = getRoleEditConstraintDays();

    return (
      <div className="list-section add">
        {this.renderHeading()}
        {
          <form onSubmit={(e) => this.add(e)}>
            <div className="flex width50">
              {this.renderDate({
                fieldname: "date",
                label: messages.fields.date,
                maxDate: moment(),
                minDate: moment().add(-days, "d"),
              })}
              {this.renderAutoComplete({
                fieldname: "machineryId",
                placeholder: "Machinery Name",
                options: this.props.dropdowns?.machinery || [],
                disableClearable: true,
                required: true,
                getOption: (option) => {
                  return option["name"];
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData.machineryId = value.id;
                  }
                },
              })}
            </div>
            <div className="flex width50">
              {this.renderAutoComplete({
                fieldname: "supplierId",
                placeholder: "Supplier Name",
                options: this.props.dropdowns?.supplier || [],
                getOption: (option) => {
                  return option["name"];
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData.supplierId = value.id;
                  }
                },
              })}
              {this.renderAutoComplete({
                fieldname: "contractorId",
                placeholder: "Contractor",
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
                onChange: (e, value) => {
                  if (value) {
                    this.formData.locationId = value.id;
                  }
                },
              })}
            </div>
            <div className="flex">
              {this.renderRadio({
                label: "Mode",
                options: this.radioOptions,
                fieldname: "mode",
                defaultValue: "Daily",
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
                name: "Description",
              })}
            </div>
            {this.renderFileArea()}
            
            {this.renderFooter()}
          </form>
        }
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
  withSnackbar(Add)
);
