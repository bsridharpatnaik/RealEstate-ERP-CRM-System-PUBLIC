import React, { Component } from "react";
import { API } from "./../../../../axios";
import { SvgIcon } from "@material-ui/core";
import { apiEndpoints } from "./../../../../endpoints";
import { withSnackbar } from "notistack";
import { ReactComponent as CloseSVG } from "./../Progress/close.svg";
import { ReactComponent as EditSVG } from "./../Note/edit (1).svg";
import { getSession, clearSession } from '../../../../helper';
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import PaymentForm from "./PaymentForm";
import PaymentHistory from './PaymentHistory';
import Typography from "@material-ui/core/Typography";
import "./style.scss";

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

class PaymentStructure extends Component {
    state = {
        dealstructure: [],
        currentPropertyId: 0,
        isLoading: true,
        isAddSchedule: false,
        paymentId: null,
        payment: {},
        bankPayments: [],
        paymentList: [],
        totalAmount: "N/A",
        totalPaymentPending: "N/A",
        totalPaymentReceived: "N/A",

    }
    url = apiEndpoints.getDealStructure;
    async componentDidMount() {
        const currentPropertyId = getSession('currentPropertyId');
        currentPropertyId && this.setState({ currentPropertyId: currentPropertyId });
        this.getDeals();        
    }

    fetchPayments = (currentPropertyId) => {
        API.GET(apiEndpoints.getPaymentStatus.replace('{leadid}', currentPropertyId)).then((response) => {
            this.setState({
                totalAmount: response.data?.totalAmount,
                totalPaymentPending: response.data?.totalPaymentPending,
                totalPaymentReceived: response.data?.totalPaymentReceived,
            })
        })
        API.GET(apiEndpoints.getPaymentListByDealId.replace('{dealId}', currentPropertyId))
        .then((response) => {
            if(response.success) {
                console.log(response.data);
                this.setState({
                    bankPayments: response.data.bankPayments.paymentList,
                    paymentList: response.data.customerPayments.paymentList,
                })
            }
        })
    }

    async getDeals() {
        this.setState({ isLoading: true })
        const response = await API.GET(this.url.replace('{leadid}', this.props.leadId))
        if (response.success) {
            const currentPropertyId = this.state.currentPropertyId === 0 ? ((response.data && response.data.length > 0) ? response.data[0].dealId : 0) : this.state.currentPropertyId;
            this.setState({
                dealstructure: response.data,
                currentPropertyId: currentPropertyId,
                currentProperty: this.state.currentPropertyId === 0 ? ((response.data && response.data.length > 0) ? response.data[0] : null)
                    : response.data.filter(
                        (val) => val.dealId === this.state.currentPropertyId
                    )[0],
            });
            this.fetchPayments(currentPropertyId);
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

    afterUpdatePaymentForm = () => {
        this.setState({payment: {}, paymentId: null});
        this.fetchPayments(this.state.currentPropertyId);
    }

    onDeletePayment = (paymentId) => {
        API.DELETE(apiEndpoints.onDeletePayment.replace("{paymentId}", paymentId), {
            paymentId: paymentId,
            dealStructureId: this.props.dealId,
        }).then((response) => {
            this.props.enqueueSnackbar("Payment Deleted Successfully", {
                variant: "success",
            });
            this.fetchPayments(this.state.currentPropertyId);
        })
    }
    onEditPayment = (paymentId, payment) => {
        this.setState({paymentId: paymentId});
        this.setState({payment: payment});
    }

    DealDetails() {
        return this.state.currentProperty && (
            <div className="propertyDetails">
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
        );
    }

    render() {
        console.log("this.state.payment", this.state.payment);
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
                        </div>
                        <React.Fragment>
                            {this.state.currentProperty && !this.state.isAddProperty && this.DealDetails()}
                        </React.Fragment>
                        <React.Fragment>
                            <div className="paymentForm">
                                <PaymentForm afterUpdatePaymentForm={this.afterUpdatePaymentForm} payment={this.state.payment} dealId={this.state.currentPropertyId}/>
                            </div>
                        </React.Fragment>
                        <React.Fragment>
                                <PaymentHistory onDeletePayment={this.onDeletePayment} bankPayments={this.state.bankPayments} paymentList={this.state.paymentList} payment={this.state.payment} onEditPayment={this.onEditPayment} dealId={this.state.currentPropertyId}/>
                        </React.Fragment>
                    </React.Fragment>
                )}
            </div>
        )
    }

}


export default withSnackbar(PaymentStructure)