import React from 'react';
import { API } from "./../../../../axios";
import TF from "./../../../../Shared/TextField";
import Select from "./../../../../Shared/Select/withValue";
import Button from "./../../../../Shared/Button";
import Date from "./../../../../Shared/Date/";
import { apiEndpoints } from "./../../../../endpoints";
import { withSnackbar } from "notistack";

class PaymentForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            options: [],
            validPaymentFrom: [],
            validMPaymentMode: [],
            formValid: false,
            data: {
                ...this.data
            }
        }
    }

    data = {
        paymentBy: "",
        date: "",
        amount: "",
        bankName: "",
        paymentMode: "",
        referenceNumber: "",
    }

    submit = async () => {
        if (!(this.props.payment && this.props.payment.paymentId)) {
            this.setState({ formValid: false });
            try {
                const response = await API.POST(apiEndpoints.savePaymentRecieved, {
                    ...this.state.data,
                    paymentReceivedDate: this.state.data.date,
                    dealStructureId: this.props.dealId,
                });
                console.log("Response:", response);
                if (response.success === true) {
                    this.props.afterUpdatePaymentForm();
                    this.setState({
                        data: {
                            ...this.data,
                            date: this.state.data.date,
                        }
                    })
                    this.props.enqueueSnackbar("Payment Added Successfully", {
                        variant: "success",
                    });
                } else {
                    if (response.errorMessage) {
                        throw new Error(response.errorMessage);
                    } else {
                        throw new Error("Something went wrong.");
                    }
                }
            } catch (error) {
                console.error("Catch error:", error);
                this.props.enqueueSnackbar(error.message || "Something went wrong.", {
                    variant: "error",
                });
                this.setState({ formValid: true });
            }
        } else {
            // Handle PUT request similarly
        }
    }
    
    
    
    
    
    

    formValid = () => {
        const data = this.state.data;
        const isValid = data.paymentBy.length > 0 && data.date.length > 0 &&
            data.amount > 0 && data.paymentMode.length > 0 &&
            data.referenceNumber.length > 0 && data.bankName.length > 0;
        this.setState({ formValid: isValid });
    };

    componentDidMount() {
        API.GET(apiEndpoints.getPaymentDropdownValues).then((response) => {
            if (response.success) {
                this.setState({
                    validMPaymentMode: response.data.validMPaymentMode,
                    validPaymentFrom: response.data.validPaymentFrom
                })
            }
        });
    }
    componentWillReceiveProps(nextProps) {
        if (nextProps.payment && nextProps.payment.paymentId) {
            this.setState({
                data: {
                    ...nextProps.payment,
                    date: nextProps.payment.paymentReceivedDate,
                }
            });
        }
    }
    render() {
        return (
            <div className="activity-tab">
                <div className="flex width30">
                    <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
                        <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
                            Payment By
                        </label>
                        <Select
                            label="Payment By"
                            name={"paymentBy"}
                            options={this.state.validPaymentFrom || []}
                            onChange={(event) => {
                                this.setState({
                                    data: {
                                        ...this.state.data,
                                        paymentBy: event.target.value,
                                    }
                                });
                                this.formValid();
                            }}
                            value={this.state.data.paymentBy}
                        />
                    </div>
                    <Date
                        onChange={(e) => {
                            this.setState({
                                data: {
                                    ...this.state.data,
                                    date: e,
                                }
                            });
                            this.formValid();
                        }}
                        label="Date"
                        fieldname={"date"}
                        dontSetDefault={true}
                        value={this.state.data.date}
                    />
                    <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
                        <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
                            Types of Payment
                        </label>
                        <Select
                            label="Types of Payment"
                            name={"paymentMode"}
                            options={this.state.validMPaymentMode || []}
                            onChange={(event) => {
                                this.setState({
                                    data: {
                                        ...this.state.data,
                                        paymentMode: event.target.value,
                                    }
                                });
                                this.formValid();
                            }}
                            value={this.state.data.paymentMode}
                        />
                    </div>
                </div>
                <div className="flex width30">
                    <TF
                        placeholder="Amount"
                        fieldname="amount"
                        onChange={(event) => {
                            this.setState({
                                data: {
                                    ...this.state.data,
                                    amount: event,
                                }
                            }, () => this.formValid());
                        }}
                        validation={"maxlength"}
                        lengthConstraint={25}
                        value={this.state.data.amount}
                    />
                    <TF
                        placeholder="Bank Name"
                        fieldname="bankName"
                        onChange={(event) => {
                            this.setState({
                                data: {
                                    ...this.state.data,
                                    bankName: event,
                                }
                            }, () => this.formValid());
                        }}
                        validation={"maxlength"}
                        lengthConstraint={25}
                        value={this.state.data.bankName}
                    />
                    <TF
                        placeholder="Reference Number"
                        fieldname="referenceNumber"
                        onChange={(event) => {
                            this.setState({
                                data: {
                                    ...this.state.data,
                                    referenceNumber: event,
                                }
                            }, () => this.formValid());
                        }}
                        validation={"maxlength"}
                        lengthConstraint={25}
                        value={this.state.data.referenceNumber}
                    />
                </div>
                <div className="button-section">
                    <Button
                        type="submit"
                        buttonClass="blue"
                        label={this.props.payment && this.props.payment.paymentId ? "UPDATE" : "SAVE"}
                        onClick={() => this.submit()}
                        disabled={!this.state.formValid}
                    />
                </div>
            </div>
        )
    }
}

export default withSnackbar(PaymentForm);