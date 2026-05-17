package com.ec.application.data;

import com.ec.application.model.Contact;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({
        "Name", "Contact Type", "Mobile No", "Email",
        "GST Number", "Contact Person", "Contact Person Mobile",
        "Address Line 1", "Address Line 2", "City", "State", "PIN Code",
        "Account Name", "Account Number", "Bank Name", "Branch Name", "IFSC Code"
})
public class ContactExportDAO {

    @JsonProperty("Name")
    String name;

    @JsonProperty("Contact Type")
    String contactType;

    @JsonProperty("Mobile No")
    String mobileNo;

    @JsonProperty("Email")
    String emailId;

    @JsonProperty("GST Number")
    String gstNumber;

    @JsonProperty("Contact Person")
    String contactPerson;

    @JsonProperty("Contact Person Mobile")
    String contactPersonMobileNo;

    @JsonProperty("Address Line 1")
    String addrLine1;

    @JsonProperty("Address Line 2")
    String addrLine2;

    @JsonProperty("City")
    String city;

    @JsonProperty("State")
    String state;

    @JsonProperty("PIN Code")
    String zip;

    @JsonProperty("Account Name")
    String accountName;

    @JsonProperty("Account Number")
    String accountNumber;

    @JsonProperty("Bank Name")
    String bankName;

    @JsonProperty("Branch Name")
    String branchName;

    @JsonProperty("IFSC Code")
    String ifscCode;

    public ContactExportDAO(Contact c) {
        this.name                  = c.getName();
        this.contactType           = c.getContactType();
        this.mobileNo              = c.getMobileNo();
        this.emailId               = c.getEmailId();
        this.gstNumber             = c.getGstNumber();
        this.contactPerson         = c.getContactPerson();
        this.contactPersonMobileNo = c.getContactPersonMobileNo();
        this.addrLine1             = c.getAddr_line1();
        this.addrLine2             = c.getAddr_line2();
        this.city                  = c.getCity();
        this.state                 = c.getState();
        this.zip                   = c.getZip();
        this.accountName           = c.getAccountName();
        this.accountNumber         = c.getAccountNumber();
        this.bankName              = c.getBankName();
        this.branchName            = c.getBranchName();
        this.ifscCode              = c.getIfscCode();
    }
}
