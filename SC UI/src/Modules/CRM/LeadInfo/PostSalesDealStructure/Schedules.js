import React, { Component } from "react";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import Button from "./../../../../Shared/Button";
import Schedule from './Schedule';
import { SvgIcon } from "@material-ui/core";
import Grid from '@material-ui/core/Grid';
import { ReactComponent as CloseSVG } from "./../Progress/close.svg";
import { ReactComponent as EditSVG } from "./../Note/edit (1).svg";
import { withSnackbar } from "notistack";
import "./style.scss";
import "../PostSalesPaymentStructure/style.scss";

function EditIcon(props) {
    return (
      <SvgIcon {...props}>
        <EditSVG />
      </SvgIcon>
    );
  }
  function CrossIcon(props) {
    return (
      <SvgIcon {...props}>
        <CloseSVG />
      </SvgIcon>
    );
  }

class Schedules extends Component{
    state = {
        isLoading: true,
        schedules: [],
        isAddSchedule: false,
        isEdit: false,
    }

    async componentDidMount() {
        this.setState({isLoading: true});
        const response = await API.GET(apiEndpoints.getSchedulesByDeal.replace('{dealId}', this.props.dealId));
        if (response.success) {
          this.setState({ schedules: response.data });
          this.setState({isLoading: false});
        }
        else {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
              });
        }
    }

    cancel(){
        this.setState({isEdit: false, isAddSchedule: false})
    }

    async deleteSchedule(scheduleId)
      {
          const response = await API.DELETE(apiEndpoints.deleteSchedule.replace('{scheduleId}',scheduleId));
          if(response.success)
          {
            this.props.refresh();
            this.props.enqueueSnackbar("Schedule deleted Successfully", {
                variant: "success",
              });
          }
          else {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
              });
          }
      }

    render()
    {
        const totalCustomer = this.state.schedules.customer && Array.isArray(this.state.schedules.customer.scheduleList) && this.state.schedules.customer.scheduleList.length;
        const totalBank = this.state.schedules.bank && Array.isArray(this.state.schedules.bank.scheduleList) && this.state.schedules.bank.scheduleList.length;
        return(
            <div className="schedules-main">
                <div className="schedule-header">
                    <span className="schedule-heading">Schedule: {this.state.schedules && !this.state.schedules.isMatchingDealAmount ? <small className="text-danger">Note: Schedule does not match with deal amount</small> : null}</span> 
                    <Button
                        type="button"
                        buttonClass="blue"
                        label="Add Schedule"
                        onClick={() => this.setState({isAddSchedule: true, isEdit: false})}
                        disabled={this.state.isAddSchedule}
                    />
                </div>
                {this.state.isAddSchedule && (
                    <Schedule cancel={() => this.cancel()} currentSchedule={this.state.currentSchedule} dealId={this.props.dealId} loanRequired={this.props.loanRequired} isEdit={this.state.isEdit} refresh={() => this.props.refresh()} />
                )} 
                <div className="propertyDetails" style={{borderRadius: '0'}}>
                    <div className="leftPane w-100">
                        <Grid container>
                            <Grid item md={6} xs={12} className="customers-panel">
                                <small>Customer ({totalCustomer})</small>
                                <div className="schedules-main">
                                    <div className='schedule-content' style={{marginLeft:'10px'}}>
                                        {!this.state.isAddSchedule && this.state.schedules && 
                                        this.state.schedules.customer && Array.isArray(this.state.schedules.customer.scheduleList)
                                        ? this.state.schedules.customer.scheduleList.map((item, index) => {
                                                return this.schedule(item);
                                            }) : null
                                        }
                                    </div>
                                </div>
                            </Grid>
                            <Grid item md={6} xs={12} className="bank-panel" style={{borderRight: 'none'}}>
                                <small>Bank ({totalBank})</small>
                                <div className="schedules-main">
                                    <div className='schedule-content'>
                                        {!this.state.isAddSchedule && this.state.schedules && 
                                        this.state.schedules.bank && Array.isArray(this.state.schedules.bank.scheduleList)
                                        ? this.state.schedules.bank.scheduleList.map((item, index) => {
                                                return this.schedule(item);
                                            }) : null
                                        }
                                    </div>
                                </div>
                            </Grid>
                        </Grid>
                    </div>
                </div>
            </div>
        )
    }
    schedule(item){
        const className = item.isReceived ? "received" : "pending";
        return (
            <div className={`schedule ${className}`}>
                <div className="schedule-buttons">
                    <CrossIcon onClick={() => this.deleteSchedule(item.scheduleId)} />
                </div>
                <div className='clearfix'></div>
                <div className='w-100 d-flex' style={{marginTop: '12px'}}>
                    <div className="schedule-left">
                        <div className="schedule-top">
                            <div className="schedule-date">
                                <b>{item.paymentDate}</b>
                            </div>
                            <div className={`schedule-status ${className}`}>
                                {` | ${item.isReceived ? 'Received' : 'Pending'}`}
                            </div>
                        </div>
                            <div className="schedule-bottom">
                                {item.details}
                            </div>
                    </div>
                    <div className="schedule-right d-flex">
                        <div className="schedule-mode">
                            {item.mode}
                        </div>
                        <div className="schedule-amount">
                            <span className='text-amount' style={{top: '10px', left: '10px'}}>
                                {`₹ ${item.amount}`}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

export default withSnackbar(Schedules)