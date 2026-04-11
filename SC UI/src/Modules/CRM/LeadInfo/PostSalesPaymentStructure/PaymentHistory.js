
import React from 'react';
import PaymentHistoryCard from './PaymentHistoryCard';
import "./style.scss";
class PaymentHistory extends React.Component {
    constructor(props) {
        super(props); 
        this.state = {
            bankPayments: [
                ...this.props.bankPayments
            ],
            paymentList: [
                ...this.props.paymentList
            ],
        }
    }

   onEditPayment = (id, type) => {
       if(type == 'customer') {
        const payment = this.props.paymentList.find((payment) => payment.paymentId == id);
        this.props.onEditPayment(id, payment);
       } else {
        const payment = this.props.bankPayments.find((payment) => payment.paymentId == id);
        this.props.onEditPayment(id, payment);
       }
   }

   onDeletePayment = (paymentId) => {
       this.props.onDeletePayment(paymentId);
   }

    render() {
        console.log(this.props.bankPayments, "bankflp");
        return (<>
            <h3>Payment History</h3>
            <div className="propertyDetails d-block">
                <div className='w-100 d-flex'>
                    <p className='w-50'>
                        <small>Customer ({this.props.bankPayments.length})</small>
                    </p>
                    <p className='w-50'>
                        <small>Bank ({this.props.bankPayments.length})</small>
                    </p>
                </div> 
            </div>
            <div className="propertyDetails" style={{borderRadius: 'none'}}>
                <div className="leftPane w-100">
                   <div className='customers-panel'>
                        <div className="schedules-main">
                            <div className='schedule-content'>
                                {this.props.paymentList.map((payment) => <PaymentHistoryCard onDeletePayment={this.onDeletePayment} onChangePaymentId={this.onEditPayment} type={'customer'} payment={payment}/> )}
                            </div>
                        </div>
                   </div>
                   <div className='bank-panel'>
                        <div className="schedules-main">
                            <div className='schedule-content'>
                                {this.props.bankPayments.map((payment) => <PaymentHistoryCard onDeletePayment={this.onDeletePayment} onChangePaymentId={this.onEditPayment} type={'bank'} payment={payment}/> )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </>)
    }
}

export default PaymentHistory;