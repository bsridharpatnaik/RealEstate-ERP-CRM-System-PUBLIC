package com.ec.common.Data;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AuthorizationEnum {
    ReadOnly,
    FullAccess;

    public AuthorizationEnum setFromString(String name) {
        return AuthorizationEnum.valueOf(name);
    }
}
