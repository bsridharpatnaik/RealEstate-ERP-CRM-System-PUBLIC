package com.ec.common.Data;

public class HourlyCountDto {
    private String hourRange;
    private Long count;

    // Constructor, getters, and setters

    public HourlyCountDto(String hourRange, Long count) {
        this.hourRange = hourRange;
        this.count = count;
    }

    public HourlyCountDto() {
    }

    public String getHourRange() {
        return hourRange;
    }

    public void setHourRange(String hourRange) {
        this.hourRange = hourRange;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
