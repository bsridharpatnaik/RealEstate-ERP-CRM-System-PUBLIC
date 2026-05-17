import React from "react";
import Grid from '@material-ui/core/Grid';
import Divider from '@material-ui/core/Divider';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import IconButton from "@material-ui/core/IconButton";
import { EditIcon, DeleteCircleIcon } from "../../../../Shared/Icons/Index.js";
import AddSubPropertyType from './AddSubPropertyType';
import SubPropertyTypesList from './SubPropertyTypesList';


const PropertyTypesListItem = ({ property, onEdit, onDelete, fetchlist, subTypeEdit, editable }) => {
    return (
        <div className="project-plan-plot">
            <Grid container>
                <Grid item xs={12}>
                    <Grid container justify="space-between">
                        <Grid item>
                            <Box display="flex" alignItems="center">
                                <Typography variant="h6" className="property-type-title" gutterBottom>
                                    {property.propertyType}
                                </Typography>
                                <Box display="flex" className="property-counter">
                                    <Typography variant="body1" className="red" gutterBottom>
                                        {property.bookedProperties}
                                    </Typography>
                                    <Typography variant="body1" className="green" gutterBottom>
                                        /{" "}{property.totalProperties}
                                    </Typography>
                                </Box>
                            </Box>
                        </Grid>
                        {editable && (
                            <Grid item>
                                <IconButton
                                    onClick={() => onEdit(property)}
                                    aria-label="back"
                                    className="back-icon"
                                >
                                    {EditIcon({ fontSize: "medium" })}
                                </IconButton>
                                <IconButton
                                    onClick={() => onDelete("MAIN_TYPE", property)}
                                    aria-label="back"
                                    className="back-icon"
                                >
                                    {DeleteCircleIcon({ fontSize: "medium" })}
                                </IconButton>
                            </Grid>
                        )}
                    </Grid>
                    <Divider />
                </Grid>
                {editable && (
                    <Grid item xs={12}>
                        <AddSubPropertyType
                            propertyTypeId={property.propertyTypeId}
                            fetchlist={fetchlist}
                        />
                    </Grid>
                )}
                <Grid item xs={12}>
                    <SubPropertyTypesList
                        property={property.propertyNames}
                        subTypeEdit={subTypeEdit}
                        onDelete={onDelete}
                        editable={editable}
                    />
                </Grid>
            </Grid>
        </div>
    );
}

export default PropertyTypesListItem
