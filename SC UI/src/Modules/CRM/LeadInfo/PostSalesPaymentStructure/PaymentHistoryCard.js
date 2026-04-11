import React from 'react';
import "./style.scss";

import { SvgIcon } from "@material-ui/core";
import { ReactComponent as CloseSVG } from "./../Progress/close.svg";
import { ReactComponent as EditSVG } from "./../Note/edit (1).svg";

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
class PaymentHistoryCard extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            date: "",
            typeOfPayment: "",
            paymentBy: "",
            amount: "",

        }
    }

    onDeletePayment = () => {
        this.props.onDeletePayment(this.props.payment.paymentId);
    } 
    onChangePaymentId = (paymentId) => {
        this.props.onChangePaymentId(this.props.payment.paymentId, this.props.type);
    }

    render() {
        const className = this.props.type == 'customer' ? "received" : "pending";
        return (
            <div className={`schedule ${className}`}>
                <div>
                    <div className="schedule-buttons">
                        <CrossIcon onClick={() => this.onDeletePayment()} />
                    </div>
                </div>
                <div className='clearfix'></div>
                <div className='w-100 d-flex'>
                    <div className="schedule-left">
                        <div className="schedule-top">
                            <div className="schedule-date">
                                <b>{this.props.payment.paymentReceivedDate}</b>
                            </div>
                        </div>
                        <div className="schedule-bottom">
                            {/* {item.details} */}
                        </div>
                    </div>
                
                    <div className="schedule-right d-flex">
                        <div className="schedule-mode">
                            {this.props.payment.bankName}
                        </div>
                        <div className="schedule-amount">
                            <span className='text-amount'>
                                {` ${this.props.payment.amount}`}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

export default PaymentHistoryCard;