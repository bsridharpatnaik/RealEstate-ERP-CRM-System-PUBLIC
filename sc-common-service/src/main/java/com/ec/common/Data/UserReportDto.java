package com.ec.common.Data;

import java.util.List;

public class UserReportDto {
    private String username;
    private List<DateReportDto> dates;

    // Constructor, getters, and setters

    public UserReportDto(String username, List<DateReportDto> dates) {
        this.username = username;
        this.dates = dates;
    }

    public UserReportDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<DateReportDto> getDates() {
        return dates;
    }

    public void setDates(List<DateReportDto> dates) {
        this.dates = dates;
    }
}
