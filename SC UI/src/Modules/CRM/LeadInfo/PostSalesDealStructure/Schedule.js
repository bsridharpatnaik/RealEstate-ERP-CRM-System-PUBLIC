import React, { Component } from "react";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import TF from "./../../../../Shared/TextField";
import Date from "./../../../../Shared/Date/";
import Button from "./../../../../Shared/Button";
import { CheckedIcon, UncheckIcon } from './../../../../Shared/Icons/Index';
import IconButton from "@material-ui/core/IconButton";
import { withSnackbar } from "notistack";
import "./style.scss";
import Select from "./../../../../Shared/Select";

class Schedule extends Component{
    data = {
        dealStructureId: 0,
        paymentDate: "",
        isCustomerPayment: false,
        mode: "",
        amount: "",
        details: "",
        isReceived: false,
        scheduleId: 0,
      };

      state = {
        isLoading: true,
        formValid: false,
        isSaveClicked: false,
        isReceived: false,
        isLoanRequired: false,
        paymentFrom: [{name:'Bank', id:false}, {name:'Customer', id:true}],
      };

      async componentDidMount() {
        if(this.props.isEdit){
            this.data = { dealStructureId: this.props.currentSchedule.dealStructureId,
                          paymentDate: this.props.currentSchedule.paymentDate,
                          isCustomerPayment: this.props.currentSchedule.isCustomerPayment,
                          amount: this.props.currentSchedule.amount,
                          details: this.props.currentSchedule.details,
                          isReceived: this.props.currentSchedule.isReceived,
                          scheduleId: this.props.currentSchedule.scheduleId,
                          mode: this.props.currentSchedule.mode,
                      };
            this.setState({isReceived: this.data.isReceived});
        }
        else{
            this.data.dealStructureId = this.props.dealId;
            this.data.isCustomerPayment = this.props.loanRequired ? false : true;
        }
        this.setState({isLoading: false, isLoanRequired: this.props.loanRequired});
    }

    submit = async () => {  
        this.setState({isSaveClicked: true});      
        const params = {
          ...this.data,
          isReceived: this.state.isReceived,
        };
        let responseSuccess = false;
        let ErrorMessage = '';
        if(this.data.scheduleId > 0)
        {
          const response = await API.PUT(apiEndpoints.updateSchedule.replace('{scheduleId}',this.data.scheduleId), params);
          responseSuccess = response.success;
          ErrorMessage = response.errorMessage;
        }
        else{
        const response = await API.POST(apiEndpoints.createSchedule, params);
        responseSuccess = response.success;
        ErrorMessage = response.errorMessage;
        }
        if (responseSuccess) {
          this.props.refresh();
          this.props.enqueueSnackbar("Schedule saved Successfully", {
            variant: "success",
          });
        } else {
          this.props.enqueueSnackbar(ErrorMessage, {
            variant: "error",
          });
        }
      };

      formValid = () => {
        const formValid =
          this.data.dealStructureId > 0 &&
          this.data.paymentDate.length &&          
          this.data.details.length &&
          this.data.mode.length &&
          this.data.amount > 0;          
        this.setState({ formValid });
      };

      render()
      {
          return !this.state.isLoading && (
    <div className="activity-tab">
        <div className="flex width30">
          <Date
            onChange={(e) => {
              this.data.paymentDate = e;
              this.formValid();
            }}
            label="Payment Date"
            fieldname={"paymentDate"}
            dontSetDefault={false}
            defaultValue={this.data.paymentDate}
          />
          <TF
              placeholder="Mode"
              fieldname="mode"
              onChange={(event) => {
                this.data.mode = event;
                this.formValid();
              }}
              validation={"maxlength"}
              lengthConstraint={15}
              value={this.data.mode}
            />
            <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
            <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
              Payment From
            </label>
            <Select
              label="Payment From"
              name={"isCustomerPayment"}
              labelKey={'name'}
              valueKey={'id'}
              options={this.state.paymentFrom || []}
              defaultValue={this.data.isCustomerPayment}
              disabled={!this.state.isLoanRequired}
              onChange={(event) => {
                this.data.isCustomerPayment = event.target.value;
                this.formValid();
              }}
            />
          </div>
            <TF
              placeholder="Amount"
              fieldname="amount"
              onChange={(event) => {
                this.data.amount = event;
                this.formValid();
              }}
              type='number'
              validation={"nonnegative"}
              value={this.data.amount}
            />
            </div>

        <div className="flex">
            <div className="left">
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
            <div className="right">
                <div className="empty-section">
                {/* <IconButton
                    label={'Received'}
                    aria-label={'Received'}
                    className={'back-icon'}
                    onClick={() => {
                        this.setState({isReceived:!this.state.isReceived});
                    }}
                > 
                { this.state.isReceived ? CheckedIcon({ fontSize: 'medium' }) : UncheckIcon({fontSize: 'medium'})}
                </IconButton> */}
                {/* <span>Received</span> */}
                {/* <IconButton
                    label={'Pending'}
                    aria-label={'Pending'}
                    className={'back-icon'}
                    onClick={() => {
                        this.setState({isReceived:!this.state.isReceived});
                    }}
                > 
                { !this.state.isReceived ? CheckedIcon({ fontSize: 'medium' }) : UncheckIcon({fontSize: 'medium'})}
                </IconButton>
                <span>Pending</span> */}
                
                </div>
                <div className="button-section" style={{marginTop: '22px'}}>
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

export default withSnackbar(Schedule)