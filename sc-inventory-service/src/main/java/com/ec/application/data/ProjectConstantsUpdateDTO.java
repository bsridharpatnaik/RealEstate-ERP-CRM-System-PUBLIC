package com.ec.application.data;

import javax.validation.constraints.NotNull;

public class ProjectConstantsUpdateDTO {

    @NotNull(message = "Value is required")
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
