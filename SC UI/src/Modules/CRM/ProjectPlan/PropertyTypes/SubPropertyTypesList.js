import React from 'react'
import Popover from '@material-ui/core/Popover';
import IconButton from '@material-ui/core/IconButton';
import MenuItem from '@material-ui/core/MenuItem';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import { Typography } from '@material-ui/core';
import { useHistory } from "react-router-dom";
import { setSession } from '../../../../helper';

const HtmlTooltip = withStyles((theme) => ({
    tooltip: {
        backgroundColor: "#f5f5f9",
        color: "rgba(0, 0, 0, 0.87)",
        maxWidth: 220,
        fontSize: theme.typography.pxToRem(12),
        border: "1px solid #dadde9",
    },
}))(Tooltip);

const ListPropertySubTypes = ({ property, subTypeEdit, onDelete, editable }) => {
    const [anchorEl, setAnchorEl] = React.useState(null);
    const [state, setState] = React.useState(null);
    const handleClick = (event, item) => {
        setAnchorEl(event.currentTarget);
        setState(item)
    };
    let history = useHistory();
    const redirectToCustomerInfo = (customerId, dealId, history) => {
        // setSession('isDealStructure', 1);
        // setSession('currentPropertyId', dealId);
        history.push("/crm/customerinfo/" + customerId);
    }

    const handleClose = () => {
        setAnchorEl(null);
    };

    const open = Boolean(anchorEl);
    const id = open ? 'simple-popover' : undefined;
    return (
        <div className="property-subtypes">
            {
                property.map(item => (
                    <div key={item.propertyNameId} className="property-subtypes-items">
                        {editable ? (
                            <IconButton
                                aria-label="more"
                                aria-controls="long-menu"
                                aria-haspopup="true"
                                onClick={(e) => handleClick(e, item)}
                                className={`property-sub-types-dropdown ${item.isBooked ? 'isBooked' : ''}`}
                            >
                                {item.name}<MoreVertIcon />
                            </IconButton>
                        ) : ((item.customerId || item.customerName || item.phase || item.plotSize || item.superBuiltupArea || item.unitDetails) ? (
                            <HtmlTooltip
                                title={
                                    <React.Fragment>                                        
                                        {item.customerName && (<Typography>Customer Name: {item.customerName}</Typography>)}
                                        {item.phase && (<Typography>Phase: {item.phase}</Typography>)}
                                        {item.plotSize && (<Typography>Plot Size: {item.plotSize}</Typography>)}
                                        {item.superBuiltupArea && (<Typography>Super Builtup Area: {item.superBuiltupArea}</Typography>)}
                                        {item.unitDetails && (<Typography>Plot Area: {item.unitDetails}</Typography>)}
                                    </React.Fragment>
                                }
                            >
                                <IconButton
                                    aria-label="more"
                                    aria-controls="long-menu"
                                    aria-haspopup="true"
                                    className={`property-sub-types-dropdown no-edit ${item.isBooked ? 'isBooked' : ''}`}
                                    onClick={(e) => {
                                        item.customerId && redirectToCustomerInfo(item.customerId, item.dealId, history);
                                    }}
                                >
                                    {item.name}
                                </IconButton>
                            </HtmlTooltip>
                        ) : (
                            <IconButton
                                aria-label="more"
                                aria-controls="long-menu"
                                aria-haspopup="true"
                                className={`property-sub-types-dropdown no-edit ${item.isBooked ? 'isBooked' : ''}`}
                            >
                                {item.name}
                            </IconButton>
                        ))}
                        <Popover
                            id={id}
                            open={open}
                            anchorEl={anchorEl}
                            onClose={handleClose}
                            className="property-subtypes-dropdown"
                            anchorOrigin={{
                                vertical: 'bottom',
                                horizontal: 'center',
                            }}
                            transformOrigin={{
                                vertical: 'top',
                                horizontal: 'center',
                            }}
                        >
                            <MenuItem onClick={() => {
                                handleClose()
                                subTypeEdit(state)
                            }}>Edit</MenuItem>
                            <MenuItem onClick={() => {
                                handleClose()
                                onDelete("SUB_TYPE", state)
                            }}>Delete</MenuItem>
                        </Popover>
                    </div>
                ))
            }
        </div>
    )
}

export default ListPropertySubTypes;