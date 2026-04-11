//react
import React from "react";
//Third Party
import {withSnackbar} from "notistack";
import Popper from "@material-ui/core/Popper";

//component
import ListCommon from "./../../Shared/List";
import IconButtons from "./../../Shared/Button/IconButtons.js";
//misc
import {apiEndpoints, exportURL} from "./../../endpoints";
import {messages} from "./../../messages";
import Filter from "./filter";
import {API} from '../../axios';
import CommonTable from "../../Shared/Table";

class List extends ListCommon {
    title = messages.common.pricingHeader;
    filterData = {};
    state = {
        pageno: 0,
        data: [],
        key: 1,
        totals: []
    };
    tableData = {
        headers: ["Category Name", "Product Name", "Measurement Unit", "Month"],
        keys: [
            "categoryName",
            "productName",
            "measurementUnit",
            "date"
        ],
    };
    url;
    ignoreQueryParamsExport = true;

    componentDidMount() {
        this.url = this.props.isExisting ? apiEndpoints.getExistingPrices : apiEndpoints.missingPrices;
        this.exportUrl = this.props.isExisting ? apiEndpoints.exportExistingPrices : apiEndpoints.exportMissingPrices;
        this.exportFile = this.props.isExisting ? messages.exportFiles.existingInventoryPricing : messages.exportFiles.missingInventoryPricing;
        if (this.props.isExisting) {
           this.tableData.keys.push("price");
           this.tableData.headers.push("Price");
        }
        this.search();
        this.filterRef = React.createRef();
    }

    getExportData(response) {
        return response.data.content;
    }

    delete = async ({date, productId, price}) => {
        const requestBody = {
            date,
            productId,
            price
        }
        const response = await API.POST(apiEndpoints.deletePrice, requestBody);
        if (response.success) {
            this.search();
            this.props.enqueueSnackbar(this.title + messages.common.deleteSuccess, {
                variant: "success",
            });
        } else {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
            });
        }
    }

    replaceSortKey(sortKey) {
        if (this.props.isExisting) {
            if (sortKey === "categoryName") {
                return "product.category.categoryName";
            } else if (sortKey === "productName") {
                return "product.productName";
            } else if (sortKey === "measurementUnit") {
                return "product.measurementUnit";
            }
        }
        return sortKey;
    }

    prepareRequestBody() {
        let params;
        params = {};
        params.filterData = [];
        if (this.filterData) {
            for (const field in this.filterData) {
                let value = this.filterData[field].map(item => item.name);
                if (value && value.length > 0) {
                    params.filterData.push({
                        attrName: field,
                        attrValue: value,
                    });
                }
            }
        }
        return params;
    }

    edit = (object) => {
        const {date, productId: id, price = 0} = object;
        this.props.selectObject({id, date, price}, true);
    }

    async search(page = 0) {
        this.setState({pageno: page});
        const params = this.prepareRequestBody();
        const response = await this.getData(page, params);
        if (response.success) {
            this.setState({
                data: response.data.content,
                pages: response.data.totalPages,
                totalRecords: response.data.totalElements
            });
        }
    }

    render() {
        return (
            <div>
                <div className="list-section">
                    <div className="filter-section" style={{justifyContent: "end"}}>
                        {this.renderExport()}
                        <div className="top-button-wrapper">
                            <IconButtons
                                onClick={() => {
                                    this.setState({filterOpen: true});
                                }}
                                buttonClass="filterIcon"
                                innerRef={this.filterRef}
                                label={messages.common.filter}
                                icon={"FilterSVG"}
                            />
                        </div>
                        <Popper
                            open={this.state.filterOpen}
                            anchorEl={this.filterRef && this.filterRef.current}
                            placement="bottom-end"
                        >
                            <Filter
                                filterData={this.filterData}
                                options={this.props.dropdowns}
                                search={(data) => {
                                    this.filterData = data;
                                    this.search();
                                }}
                                close={() => this.setState({filterOpen: false})}
                            />
                        </Popper>
                    </div>
                    {this.state.isLoading ? (
                        this.renderLoader()
                    ) : (
                        <CommonTable
                            key={this.state.key}
                            tableData={this.tableData}
                            rows={this.state.data}
                            edit={this.edit}
                            delete={this.delete}
                            sortby={this.sortby}
                            sortkey={this.sortkey}
                            search={(sortkey, sortby) => {
                                this.sortby = sortby;
                                this.sortkey = sortkey;
                                this.search();
                            }}
                            hidedelete={!this.props.isExisting}
                        />
                    )}
                    {this.renderPagination()}
                </div>
            </div>
        );
    }
}

export default withSnackbar(List);
