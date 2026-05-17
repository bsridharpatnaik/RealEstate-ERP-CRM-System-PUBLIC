import React from 'react'
import { withSnackbar } from 'notistack';
import { apiEndpoints } from '../../../../endpoints';
import { messages } from "../../../../messages";
import AddForm from "../../../../Shared/AddForm";
import Button from "../../../../Shared/Button";

class AddPropertyType extends AddForm {
    title = messages.common.propertySubTypes
    addurl = apiEndpoints.properyTypes + this.props.propertyTypeId + '/create';
    showToaster(response) {
        if (response.success) {
            this.props.enqueueSnackbar(this.title + messages.common.addSuccess, {
                variant: "success",
            });
            this.props.fetchlist()
        } else {
            this.props.enqueueSnackbar(response.errorMessage, {
                variant: "error",
            });
        }
    }

    render() {
        return (
            <div className="add-property">
                <form onSubmit={(e) => this.add(e)}>
                    {this.renderTextField({
                        fieldname: "name",
                        placeholder: "Enter Plot / House No.",
                        validation: "maxlength",
                        lengthConstraint: 100,
                        errorMessage: messages.common.max100,                        
                    })}
                    <Button
                        type="submit"
                        buttonClass="blue"
                        label={messages.common.add}
                        disabled={this.state.isUpdating}
                    />
                </form>
            </div>
        );
    }
}

export default withSnackbar(AddPropertyType);