package com.ec.application.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdNameDTO {
    private String id;
    private String name;

    public IdNameDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // getters
}
