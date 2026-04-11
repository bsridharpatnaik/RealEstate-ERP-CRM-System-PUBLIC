import React from "react";
import "./style.scss";
import {API} from "../../axios";
import {apiEndpoints} from "../../endpoints";
import Common from "../../Shared/CommonIndex";
import {messages} from "../../messages";
import Button from "@material-ui/core/Button";
import TF from "@material-ui/core/TextField";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Skeleton from "@material-ui/lab/Skeleton";
import CommonTable from "../../Shared/Table";
import {getJson2csvCallback} from "../../helper";
import converter from "json-2-csv";
import {withSnackbar} from "notistack";

class PricingReport extends Common {
    title = messages.common.pricingReport;
    tableData = {
        headers: ["Product Name", "Category Name", "Unit Price", "Quantity", "Price"],
        keys: [
            "productName",
            "categoryName",
            "price",
            "totalQuantity",
            "totalPrice"
        ],
    };
    state = {
        locationId: "",
        monthList: [],
        usageLocations: null,
        months: null,
        isLoading: false,
        reportData: null
    };

    async componentDidMount() {
        this.getUsageLocationsDropdown();
    }

    async getUsageLocationsDropdown() {
        const response = await API.GET(apiEndpoints.pricingReportLocationsDropdowns);
        if (response.success) {
            const {usagelocation: usageLocations} = response.data;
            this.setState({usageLocations});
        }
    }

    async getMonthsDropdown() {
        const response = await API.GET(apiEndpoints.pricingReportMonthsDropdowns(this.state.locationId));
        if (response.success) {
            const months = response.data;
            this.setState({months});
        }
    }

    getReportData = async () => {
        this.setState({isLoading: true, reportData: null});
        const {locationId, monthList} = this.state;
        const response = await API.POST(apiEndpoints.pricingReportData, {locationId, monthList});
        if (!response.success) {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
            });
            this.setState({isLoading: false});
            return;
        }
        const reportData = response.data;
        this.setState({reportData, isLoading: false});
        if (reportData.length === 0) {
            this.props.enqueueSnackbar("There are no outward entries added for the selected location", {
                variant: "info",
            });
        }
    }

    exportReport = () => {
        const {locationId, reportData} = this.state;
        let json2csvCallback = getJson2csvCallback(`Pricing Report_${locationId}_${+new Date()}.csv`);
        converter.json2csv(reportData, json2csvCallback);
    }

    render() {
        const {usageLocations, months, locationId, isLoading, reportData} = this.state;

        return (
            <div className="page">
                <div className="header-info">
                    <div>
                        {this.renderHeading()}
                        {this.renderBreadcrums(messages.common.inventory)}
                    </div>
                </div>
                {usageLocations ? <Autocomplete
                    id="tags-standard"
                    options={usageLocations}
                    disabled={isLoading}
                    disableClearable={true}
                    onChange={(event, values) => {
                        this.setState({locationId: values.id, months: null, reportData: null}, this.getMonthsDropdown);
                    }}
                    getOptionLabel={(option) => option["name"]}
                    filterSelectedOptions={true}
                    renderInput={(params) => (
                        <TF
                            {...params}
                            name={"Location"}
                            id={"locationId"}
                            variant="outlined"
                            margin="normal"
                            label={"Location"}
                            InputLabelProps={{shrink: true}}
                        />
                    )}
                /> : this.renderLoader()}
                {months && <Autocomplete
                    id="tags-standard"
                    options={months}
                    disabled={isLoading}
                    multiple={true}
                    disableClearable={true}
                    onChange={(event, values) => {
                        this.setState({monthList: values, reportData: null});
                    }}
                    filterSelectedOptions={true}
                    renderInput={(params) => (
                        <TF
                            {...params}
                            name={"Months"}
                            id={"month"}
                            variant="outlined"
                            margin="normal"
                            label={"Months"}
                            InputLabelProps={{shrink: true}}
                        />
                    )}
                />}
                {(locationId && !months) && this.renderLoader()}
                <div className="flex flex-space-between flex-align-items-center">
                    <div>
                        <Button
                            onClick={this.getReportData}
                            variant="contained"
                            color="primary"
                            disabled={!(locationId && months) || isLoading}
                        >Get Report</Button>
                        <span>&nbsp;</span>
                        {reportData && <Button
                            onClick={this.exportReport}
                            variant="contained"
                            color="primary"
                            disabled={!(locationId && months) || isLoading}
                        >Export</Button>}
                    </div>
                    {reportData && <div>
                        <b>Total Price</b>
                        <span>: </span>
                        <span>{reportData.reduce((total, item) => total + item.totalPrice, 0)}</span>
                    </div>}
                </div>
                {isLoading && this.renderLoader(150)}
                {reportData && <div className="mt-15">
                    <div className="report-table list-section">
                        <CommonTable
                            key={"reportData1"}
                            tableData={this.tableData}
                            rows={reportData}
                            hidedelete={true}
                            hideedit={true}
                        />
                    </div>
                </div>}
            </div>
        );
    }

    renderLoader(height = 50) {
        return (
            <React.Fragment>
                <Skeleton variant="rect" height={height}/>
            </React.Fragment>
        );
    }
}


export default withSnackbar(PricingReport);
