package com.ec.crm.Data;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import Deserializers.ToUsernameSerializer;
import com.ec.crm.Enums.*;
import com.ec.crm.Model.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.NotAudited;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadActivityExportDTO {

    @JsonProperty("Lead Id")
    Long leadId;

    @JsonProperty("Customer Name")
    String customerName;

    @JsonProperty("Primary Mobile")
    String primaryMobile;

    @JsonProperty("Secondary Mobile")
    String secondaryMobile;

    @JsonProperty("Email Id")
    String emailId;

    @JsonProperty("Purpose")
    String purpose;

    @JsonProperty("Occupation")
    String occupation;

    @JsonProperty("Date of Birth")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date dateOfBirth;

    @JsonProperty("Broker")
    String broker;

    @JsonProperty("Address")
    String address;

    @JsonProperty("Source")
    String source;

    @JsonProperty("Property Type")
    String propertyType;

    @JsonProperty("Sentiment")
    String sentiment;

    @JsonProperty("Stagnant Days")
    Long stagnantDaysCount;

    @JsonProperty("Assignee")
    @JsonSerialize(using = ToUsernameSerializer.class)
    Long assignee;

    @JsonProperty("Lead Created By")
    @JsonSerialize(using = ToUsernameSerializer.class)
    Long leadCreator;

    @JsonProperty("Lead Status")
    String leadStatus;

    @JsonProperty("Loan Status")
    String loanStatus;

    @JsonProperty("Customer Status")
    String customerStatus;

    @JsonProperty("Activity ID")
    Long leadActivityId;

    @JsonProperty("Activity Date Time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
    Date activityDateTime;

    @JsonProperty("Title")
    String title;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Is Open?")
    String isOpen;

    @JsonProperty("Creator")
    @JsonSerialize(using = ToUsernameSerializer.class)
    Long creator;

    @JsonProperty("Closed By")
    @JsonSerialize(using = ToUsernameSerializer.class)
    Long closedBy;

    @JsonProperty("Closing Comment")
    String closingComment;

    @JsonProperty("Activity Type")
    String activityType;

    @JsonProperty("Deal Lost Reason")
    String dealLostReason;

    @JsonProperty("Is Latest Activity?")
    String isLatest;

    @JsonProperty("Follow Up Count")
    String followUpCount;

    public LeadActivityExportDTO(LeadActivity la) {
        this.followUpCount = la.getFollowUpCount()==null?"":la.getFollowUpCount().toString();
        this.isLatest = la.getIsLatest() == 1 ? "true" : "false";
        this.dealLostReason = la.getDealLostReason() == null ? "" : la.getDealLostReason().name();
        this.activityType = la.getActivityType().name();
        this.closingComment = la.getClosingComment() == null ? "" : la.getClosingComment();
        this.closedBy = la.getClosedBy() == null ? null : la.getClosedBy();
        this.creator = la.getCreatorId();
        this.isOpen = la.getIsOpen().toString();
        this.description = la.getDescription() == null ? "" : la.getDescription();
        this.title = la.getTitle() == null ? "" : la.getTitle();
        this.activityDateTime = la.getActivityDateTime();
        this.leadActivityId = la.getLeadActivityId();
        this.customerName = la.getLead().getCustomerName();
        this.loanStatus = la.getLead().getLoanStatus() == null ? "" : la.getLead().getLoanStatus();
        this.customerStatus = la.getLead().getCustomerStatus()==null?"":la.getLead().getCustomerStatus();
        this.leadStatus = la.getLead().getStatus().toString();
        this.leadCreator = la.getLead().getCreatorId();
        this.assignee = la.getLead().getAsigneeId();
        this.stagnantDaysCount = la.getLead().getStagnantDaysCount() == null ? 0 : la.getLead().getStagnantDaysCount();
        this.sentiment = la.getLead().getSentiment() == null ? "" : la.getLead().getSentiment().name();
        this.propertyType = la.getLead().getPropertyType() == null ? "" : la.getLead().getPropertyType().name();
        this.address = concatAddress(la.getLead().getAddress());
        this.emailId = la.getLead().getEmailId()==null?"":la.getLead().getEmailId();
        this.source = la.getLead().getSource() == null ? "" : la.getLead().getSource().getSourceName();
        this.broker = la.getLead().getBroker() == null ? "" : la.getLead().getBroker().getBrokerName();
        this.occupation = la.getLead().getOccupation() == null ? "" : la.getLead().getOccupation();
        this.dateOfBirth = la.getLead().getDateOfBirth() == null ? null : la.getLead().getDateOfBirth();
        this.purpose = la.getLead().getPurpose() == null ? "" : la.getLead().getPurpose();
        this.secondaryMobile = la.getLead().getSecondaryMobile()==null?"":la.getLead().getSecondaryMobile();
        this.primaryMobile = la.getLead().getPrimaryMobile()==null?"":la.getLead().getPrimaryMobile();
        this.leadId = la.getLead().getLeadId();
    }

    private String concatAddress(Address address) {
        String strAddress = "";
        if (address == null)
            return "";

        if (address.getAddr_line1() != null)
            strAddress = strAddress + " " + address.getAddr_line1();
        if (address.getAddr_line2() != null)
            strAddress = strAddress + " " + address.getAddr_line2();
        if (address.getCity() != null)
            strAddress = strAddress + " " + address.getCity();
        if (address.getPincode() != null)
            strAddress = strAddress + " " + address.getPincode();
        return strAddress;
    }
}
