import React from "react";
import "./style.scss";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import List from "./list";
import Edit from "./edit";
import {API} from "../../axios";
import {apiEndpoints} from "../../endpoints";
import {Slide} from "@material-ui/core";
import Common from "../../Shared/CommonIndex";
import {messages} from "../../messages";
import Button from "@material-ui/core/Button";

function TabPanel(props) {
    const {children, value, index, ...other} = props;

    return (
        <Typography
            component="div"
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}
        >
            {value === index && (
                <Box className="padding-0" p={3}>
                    {children}
                </Box>
            )}
        </Typography>
    );
}

class Pricing extends Common {
    state = {
        selectedTab: 0,
        addEdit: false,
        isEdit: false,
        selectedObject: {},
        dropdowns: null
    };

    async componentDidMount() {
        this.getDropdowns();
    }

    selectObject = (selectedObject, isEdit = false) => {
        this.setState({selectedObject, addEdit: true, isEdit});
    }

    async getDropdowns() {
        const response = await API.GET(apiEndpoints.pricingDropdowns);
        if (response.success) {
            const dropdowns = response.data;
            this.setState({dropdowns});
        }
    }

    render() {
        const {addEdit, isEdit, selectedObject, dropdowns} = this.state;
        return (
            <div className="page">
                <Slide
                    direction="left"
                    in={!addEdit}
                    mountOnEnter
                    unmountOnExit
                    timeout={{ exit: 0 }}
                >
                    <div>
                        <div className="top-tabs">
                            <Tabs
                                indicatorColor="primary"
                                textColor="primary"
                                onChange={(e, value) => this.setState({selectedTab: value})}
                                value={this.state.selectedTab}
                            >
                                <Tab label="Existing Prices" id="simple-tabpanel-0"/>
                                <Tab label="Missing Prices" id="simple-tabpanel-1"/>
                            </Tabs>
                            <Button
                                onClick={() => this.setState({ addEdit: true })}
                                color="primary"
                                variant="contained"
                                disabled={!dropdowns}
                                classes={{
                                    root: "add-button",
                                    label: "add-label",
                                }}
                            >
                                {`${messages.common.add} ${messages.common.pricingHeader}`}
                            </Button>
                        </div>
                        <TabPanel value={this.state.selectedTab} index={0}>
                            <List isExisting={true} selectObject={this.selectObject} dropdowns={dropdowns} />
                        </TabPanel>
                        <TabPanel value={this.state.selectedTab} index={1}>
                            <List isExisting={false} selectObject={this.selectObject} dropdowns={dropdowns} />
                        </TabPanel>
                    </div>
                </Slide>
                <Slide
                    direction="right"
                    in={addEdit && dropdowns}
                    mountOnEnter
                    unmountOnExit
                    timeout={{ exit: 0 }}
                >
                <Edit
                    id="1"
                    back={() => {
                        this.setState({isEdit: false, addEdit: false, selectedObject: {}});
                    }}
                    data={selectedObject}
                    isEdit={isEdit}
                    dropdowns={dropdowns}
                />
                </Slide>
            </div>
        );
    }
}

export default Pricing;
