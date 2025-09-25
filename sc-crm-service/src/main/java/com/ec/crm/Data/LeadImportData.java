package com.ec.crm.Data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Setter
@Getter
public class LeadImportData {

    // Setters
    // Getters
    @JsonProperty("Name")
    @NotBlank(message = "Name is required")
    private String name;

    @JsonProperty("MobileNo")
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNo;

    @JsonProperty("Assignee")
    @NotBlank(message = "Assignee is required")
    private String assignee;

    @JsonProperty("AssigneeId")
    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;

    // Default constructor (required for Jackson deserialization)
    public LeadImportData() {}

    // Parameterized constructor
    public LeadImportData(String name, String mobileNo, String assignee, Long assigneeId) {
        this.name = name;
        this.mobileNo = mobileNo;
        this.assignee = assignee;
        this.assigneeId = assigneeId;
    }

    // toString method (useful for debugging)
    @Override
    public String toString() {
        return "LeadImportData{" +
                "name='" + name + '\'' +
                ", mobileNo='" + mobileNo + '\'' +
                ", assignee='" + assignee + '\'' +
                ", assigneeId=" + assigneeId +
                '}';
    }

    // equals and hashCode (useful if you need to compare objects)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LeadImportData that = (LeadImportData) o;

        if (!name.equals(that.name)) return false;
        if (!mobileNo.equals(that.mobileNo)) return false;
        if (!assignee.equals(that.assignee)) return false;
        return assigneeId.equals(that.assigneeId);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + mobileNo.hashCode();
        result = 31 * result + assignee.hashCode();
        result = 31 * result + assigneeId.hashCode();
        return result;
    }
}