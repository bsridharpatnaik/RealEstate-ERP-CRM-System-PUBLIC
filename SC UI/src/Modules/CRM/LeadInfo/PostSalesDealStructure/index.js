import React, { Component } from "react";
import { API } from "./../../../../axios";
import { SvgIcon } from "@material-ui/core";
import { apiEndpoints } from "./../../../../endpoints";
import Button from "./../../../../Shared/Button";
import Property from './Property';
import Schedules from './Schedules';
import { withSnackbar } from "notistack";
import { ReactComponent as CloseSVG } from "./../Progress/close.svg";
import { ReactComponent as EditSVG } from "./../Note/edit (1).svg";
import { getSession, clearSession } from '../../../../helper';
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";
import Grid from '@material-ui/core/Grid';
import Card from '@material-ui/core/Card';
import {Pie, Doughnut} from 'react-chartjs-2';
import {Chart, ArcElement} from 'chart.js'
Chart.register(ArcElement);

const HtmlTooltip = withStyles((theme) => ({
    tooltip: {
        backgroundColor: "#f5f5f9",
        color: "rgba(0, 0, 0, 0.87)",
        maxWidth: 220,
        fontSize: theme.typography.pxToRem(12),
        border: "1px solid #dadde9",
    },
}))(Tooltip);

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

class DealStructure extends Component {
    state = {
        dealstructure: [],
        currentPropertyId: 0,
        isLoading: true,
        isAddProperty: false,
        isAddSchedule: false,
        totalAmount: "N/A",
        totalPaymentPending: "N/A",
        totalPaymentReceived: "N/A",
        chartData: {
            bankAmount: [],
            customerAmount: [],
            tenPercent: [],
        },
    }
    url = apiEndpoints.getDealStructure;
    async componentDidMount() {
        const currentPropertyId = getSession('currentPropertyId');
        currentPropertyId && this.setState({ currentPropertyId: currentPropertyId });
        this.getDeals();
    }

    async getDeals() {
        this.setState({ isLoading: true })
        const response = await API.GET(this.url.replace('{leadid}', this.props.leadId))
        if (response.success) {
            const currentPropertyId = this.state.currentPropertyId === 0 ? ((response.data && response.data.length > 0) ? response.data[0].dealId : 0) : this.state.currentPropertyId;
            const currentProperty = this.state.currentPropertyId === 0 ? ((response.data && response.data.length > 0) ? response.data[0] : null)
                                        : response.data.filter(
                                            (val) => val.dealId === this.state.currentPropertyId
                                        )[0];
            this.setState({
                dealstructure: response.data,
                currentPropertyId: currentPropertyId,
                currentProperty: currentProperty,
            });
            if(Array.isArray(response.data) && response.data.length > 0) {
                this.props.togglePayments(true);
            } else if(Array.isArray(response.data) && response.data.length == 0) {
                this.props.togglePayments(false);
            }
            API.GET(apiEndpoints.getPaymentStatus.replace('{leadid}', currentPropertyId)).then((response) => {
                this.setState({
                    totalAmount: response.data?.totalAmount,
                    totalPaymentPending: response.data?.totalPaymentPending,
                    totalPaymentReceived: response.data?.totalPaymentReceived,
                })
            });

            // Prepare chart data
            const chartData = currentProperty?.chartsInformation;
            this.setState({
                chartData: {
                    bankAmount: {
                        datasets: [
                            {
                                backgroundColor: [
                                   '#ff808b',
                                   '#44c4a1'
                                ],
                                hoverBackgroundColor: [
                                    '#ff808b',
                                    '#44c4a1'
                                ],
                                data: [chartData?.bankAmount.remaining, chartData?.bankAmount.received]
                            }
                        ]
                    },
                    customerAmount: {
                        datasets: [
                            {
                                backgroundColor: [
                                   '#ff808b',
                                   '#44c4a1'
                                ],
                                hoverBackgroundColor: [
                                    '#ff808b',
                                    '#44c4a1'
                                ],
                                data: [chartData?.customerAmount.remaining, chartData?.customerAmount.received]
                            }
                        ]
                    },
                    tenPercent: {
                        datasets: [
                            {
                                backgroundColor: [
                                   '#ff808b',
                                   '#44c4a1'
                                ],
                                hoverBackgroundColor: [
                                    '#ff808b',
                                    '#44c4a1'
                                ],
                                data: [chartData?.tenPercent.remaining, chartData?.tenPercent.received]
                            }
                        ]
                    },
                }
            })
        }
        else {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
              });
        }
        clearSession('currentPropertyId');
        this.setState({ isLoading: false })
    }

    async deleteProperty() {
        const response = await API.DELETE(apiEndpoints.deleteDealStructure.replace('{dealId}', this.state.currentPropertyId));
        if (response.success) {
            this.setState({ currentPropertyId: 0 });
            this.getDeals();
            this.props.enqueueSnackbar("Property deleted Successfully", {
                variant: "success",
            });
        }
        else {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
              });
        }
    }

    changeDeal(dealId) {
        if (dealId !== this.state.currentPropertyId) {
            this.setState({
                currentPropertyId: dealId,
                currentProperty: this.state.dealstructure.filter(
                    (item) => item.dealId === dealId
                )[0],
                isAddProperty: false,
                isEdit: false,
            });
            this.refresh();
        }
    }

    async refresh() {
        this.cancel();
        this.getDeals();
    }

    cancel() {
        this.setState({ isEdit: false, isAddProperty: false })
    }

    DealDetails() {
        return this.state.currentProperty && (
            <React.Fragment>
                <div className="propertyDetails">
                    <div className="actionButtons">
                        <EditIcon onClick={() => { this.setState({ isAddProperty: true, isEdit: true }) }} />
                        <CrossIcon onClick={() => this.deleteProperty()} />
                    </div>
                    <div>
                        <div>
                            <p style={{fontWeight:'600', color: '#8989aa'}}> Booking Date: <small>{this.state.currentProperty.bookingDate}</small></p>
                            <p><span><b className="text-success">Deal Mode :</b></span> {this.state.currentProperty.loanRequired ? `Loan (${this.state.currentProperty.loanStatus})` : 'Self'}</p>
                        </div>
                        <Grid container spacing={4} xs={12} direction="row">
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Property Type</p>
                                <p className="value-deal">{`${this.state.currentProperty.propertyType}`}</p>
                            </Grid>
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Property Name</p>
                                <p className="value-deal">{`${this.state.currentProperty.propertyName}`}</p>
                            </Grid>
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Loan Required</p>
                                <p className="value-deal">{this.state.currentProperty.loanRequired ? 'Yes' : 'Self'}</p>
                            </Grid>
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Bank Name</p>
                                <p className="value-deal">{this.state.currentProperty.bankName}</p>
                            </Grid>
                        </Grid>
                    </div>
                    <div>
                        <Grid container spacing={4} xs={12} direction="row">
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Deal Amount</p>
                                <p className="value-deal text-success">{this.state.currentProperty.dealAmount}</p>
                            </Grid>
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Loan Amount</p>
                                <p className="value-deal text-warning">{this.state.currentProperty.loanAmount}</p>
                            </Grid>
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Supplement Amount</p>
                                <p className="value-deal text-danger">{this.state.currentProperty.supplementAmount}</p>
                            </Grid>
                            <Grid item xs={6} md={3}>
                                <p className="title-deal">Customer Status</p>
                                <p className="value-deal">{this.state.currentProperty.customerStatus}</p>
                            </Grid>
                        </Grid>
                    </div>
                </div>
                <div className="propertyDetails mt-15">
                    <div className="leftPane w-100">  
                        <div className="totalAmount">
                            <div className="amount"> {`₹ ${this.state.totalAmount}`} </div>
                            <div className="header"> {`Total Deal`}</div>
                        </div>
                        <div className="recievedAmount">
                            <div className="amount"> {`₹ ${this.state.totalPaymentReceived}`} </div>
                            <div className="header"> {`Total Received`}</div>
                        </div>
                        <div className="pendingAmount">
                            <div className="amount"> {`₹ ${this.state.totalPaymentPending}`} </div>
                            <div className="header"> {`Total Pending`}</div>
                        </div>
                    </div>
                </div>
            </React.Fragment>
        );
    }

    ChartDetails() {
        const COLORS = ['#ff808b', '#44c4a1'];
        return (
            <div className="mt-15">
                <Grid container spacing={4} xs={12} direction="row">
                    <Grid item xs={12} md={4}>
                        <b className="cart-header">10% Deal Amount</b>
                        <Card style={{marginTop: '20px'}}>
                            <div className="chart-left" style={{width: '50%' , display: 'inline-block'}}>
                                <Pie
                                    data={this.state.chartData.tenPercent}
                                    options={{
                                        tooltips: {
                                            mode: 'label'
                                        },
                                        responsive: true,
                                        maintainAspectRatio: false,
                                        title:{
                                            display:true,
                                            fontSize:20
                                        },
                                        legend:{
                                            display:true,
                                            position:'right'
                                        }
                                    }}
                                />     
                            </div>
                            <div className="chart-right" style={{width: '50%', display: 'inline-block'}}>
                                <p>
                                    <span className="chart-deal-type chart-deal-type-green">
                                        <ul><li>Total Amount :</li></ul>
                                    </span>
                                    <p className="text-success m-0">
                                        {this.state.currentProperty.chartsInformation.tenPercent.total}
                                    </p>
                                </p>
                                <p>
                                    <span className="chart-deal-type chart-deal-type-red">
                                        <ul><li>Remaining Amount :</li></ul>
                                    </span>
                                    <p className="text-danger m-0">
                                        {this.state.currentProperty.chartsInformation.tenPercent.remaining}
                                    </p>
                                </p>
                            </div>
                        </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                        <b className="cart-header">Remaining Customer Amount</b>
                        <Card style={{marginTop: '20px'}}>
                            <div className="chart-left" style={{width: '50%' , display: 'inline-block'}}>
                                <Pie
                                    data={this.state.chartData.customerAmount}
                                    options={{
                                        responsive: true,
                                        maintainAspectRatio: false,
                                        title:{
                                        display:true,
                                        fontSize:20
                                        },
                                        legend:{
                                        display:true,
                                        position:'right'
                                        }
                                    }}
                                />  
                            </div>
                            <div className="chart-right" style={{width: '50%', display: 'inline-block'}}>
                                <p>
                                    <span className="chart-deal-type chart-deal-type-green">
                                        <ul><li>Total Amount :</li></ul>
                                    </span>
                                    <p className="text-success m-0">
                                        {this.state.currentProperty.chartsInformation.customerAmount.total}
                                    </p>
                                </p>
                                <p>
                                    <span className="chart-deal-type chart-deal-type-red">
                                        <ul><li>Remaining Amount :</li></ul>
                                    </span>
                                    <p className="text-danger m-0">
                                        {this.state.currentProperty.chartsInformation.customerAmount.remaining}
                                    </p>
                                </p>
                            </div>
                        </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                        <b className="cart-header">Bank Amount</b>
                        <Card style={{marginTop: '20px'}}>
                            <div className="chart-left" style={{width: '50%' , display: 'inline-block'}}>
                                <Pie
                                    data={this.state.chartData.bankAmount}
                                    options={{
                                        responsive: true,
                                        maintainAspectRatio: false,
                                        title:{
                                        display:true,                                    fontSize:20
                                        },
                                        legend:{
                                        display:true,
                                        position:'right'
                                        }
                                    }}
                                />  
                            </div>
                            <div className="chart-right" style={{width: '50%', display: 'inline-block'}}>
                                <p>
                                    <span className="chart-deal-type chart-deal-type-green">
                                        <ul><li>Total Amount :</li></ul>
                                    </span>
                                    <p className="text-success m-0">
                                        {this.state.currentProperty.chartsInformation.bankAmount.total}
                                    </p>
                                </p>
                                <p>
                                    <span className="chart-deal-type chart-deal-type-red">
                                        <ul><li>Remaining Amount :</li></ul>
                                    </span>
                                    <p className="text-danger m-0">
                                        {this.state.currentProperty.chartsInformation.bankAmount.remaining}
                                    </p>
                                </p>
                            </div>
                        </Card>
                    </Grid>
                </Grid>
            </div>
        );
    }
    render() {
        return (
            <div className="main-content">
                {!this.state.isLoading && (
                    <React.Fragment>
                        <div className="main-properties">
                            <div className="properties">
                                {this.state.dealstructure &&
                                    this.state.dealstructure.map((item, index) => {
                                        const className = item.dealId === this.state.currentPropertyId ? "property selected" : "property";
                                        return (
                                            <div className={className} onClick={() => this.changeDeal(item.dealId)}>
                                                {`${item.propertyName}`}
                                            </div>
                                        )
                                    })
                                }
                            </div>
                            <div className="addProperty">
                                <Button
                                    type="button"
                                    buttonClass="blue"
                                    label="Add Property"
                                    onClick={() => this.setState({ isAddProperty: true, isEdit: false })}
                                    disabled={this.state.isAddProperty}
                                />
                            </div>
                        </div>
                        <React.Fragment>
                            {this.state.currentProperty && !this.state.isAddProperty && this.DealDetails()}
                        </React.Fragment>
                        <React.Fragment>
                            {this.state.currentProperty && !this.state.isAddProperty && this.ChartDetails()}
                        </React.Fragment>
                        <React.Fragment>
                            {!this.state.isAddProperty && this.state.currentPropertyId ? (
                                <Schedules dealId={this.state.currentPropertyId} leadId={this.props.leadId} loanRequired={this.state.currentProperty.loanRequired} refresh={() => this.refresh()} />
                            ) : (<React.Fragment> </React.Fragment>)}
                        </React.Fragment>
                    </React.Fragment>
                )}
                {this.state.isAddProperty && (
                    <Property cancel={() => this.cancel()} currentProperty={this.state.currentProperty} leadId={this.props.leadId} isEdit={this.state.isEdit} refresh={() => this.refresh()} />
                )}
            </div>
        )
    }

}


export default withSnackbar(DealStructure)