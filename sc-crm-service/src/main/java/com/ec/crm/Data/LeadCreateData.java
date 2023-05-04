package com.ec.crm.Data;

import java.util.Date;

import org.springframework.lang.NonNull;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import Deserializers.ToTitleCaseDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LeadCreateData {
    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String customerName;
    @NonNull
    String primaryMobile;
    String secondaryMobile;
    String emailId;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String purpose;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String occupation;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date dateOfBirth;
    Long brokerId;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String addressLine1;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String addressLine2;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String city;
    String pincode;
    Long sourceId;
    PropertyTypeEnum propertyType;
    SentimentEnum sentiment;
    Long assigneeId;

    Boolean isProspectLead;

    @Override
    public String toString() {
        return "LeadCreateData [customerName=" + customerName + ", primaryMobile=" + primaryMobile
                + ", secondaryMobile=" + secondaryMobile + ", emailId=" + emailId + ", purpose=" + purpose
                + ", occupation=" + occupation + ", dateOfBirth=" + dateOfBirth + ", brokerId=" + brokerId
                + ", addressLine1=" + addressLine1 + ", addressLine2=" + addressLine2 + ", city=" + city + ", pincode="
                + pincode + ", sourceId=" + sourceId + ", propertyType=" + propertyType + ", sentiment=" + sentiment
                + ", assigneeId=" + assigneeId + "]";
    }
}
