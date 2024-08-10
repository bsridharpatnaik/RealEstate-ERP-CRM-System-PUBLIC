package com.ec.common.Data;

import java.time.LocalDate;
import java.util.List;

public class DateReportDto {
    private String date;
    private String firstCall;
    private String lastCall;
    private List<HourlyCountDto> hourlyCounts;

    // Constructor, getters, and setters

    public DateReportDto(String date, String firstCall, String lastCall, List<HourlyCountDto> hourlyCounts) {
        this.date = date;
        this.firstCall = firstCall;
        this.lastCall = lastCall;
        this.hourlyCounts = hourlyCounts;
    }

    public DateReportDto() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFirstCall() {
        return firstCall;
    }

    public void setFirstCall(String firstCall) {
        this.firstCall = firstCall;
    }

    public String getLastCall() {
        return lastCall;
    }

    public void setLastCall(String lastCall) {
        this.lastCall = lastCall;
    }

    public List<HourlyCountDto> getHourlyCounts() {
        return hourlyCounts;
    }

    public void setHourlyCounts(List<HourlyCountDto> hourlyCounts) {
        this.hourlyCounts = hourlyCounts;
    }
}
