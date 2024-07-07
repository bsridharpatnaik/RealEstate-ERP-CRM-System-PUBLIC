package com.ec.crm.Enums;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.ec.crm.multitenant.ThreadLocalStorage;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum PropertyTypeEnum implements Serializable {
    HOUSE_2_BHK,
    HOUSE_3_BHK,
    PLOT,
    MULTIPLE,
    EMPTY,
    E_TYPE_5_BHK,
    F_TYPE_5_BHK,
    G_TYPE_4_BHK,
    H_TYPE_4_BHK,
    L_TYPE_3_BHK,
    I_TYPE_3_BHK,
    A_BLOCK_4_BHK,
    B_BLOCK_3_BHK,
    C_BLOCK_3_BHK,
    D_BLOCK_2_BHK,
    E_BLOCK_3_BHK,
    A_BLOCK,
    B_BLOCK,
    C_BLOCK,
    D_BLOCK,
    E_BLOCK,
    F_BLOCK,
    L_BLOCK,
    TYPE_1_6_BHK,
    TYPE_2_5_BHK,
    TYPE_3_5_BHK,
    TYPE_3_4_BHK,
    TYPE_4_5_BHK,
    TYPE_4_4_BHK,
    TYPE_5_4_BHK,
    TYPE_6_3_BHK,
    SARAFA,
    GENERAL,
    CHOPATI;

    public static List<String> getValidPropertyType() {
        List<PropertyTypeEnum> propertyTypes = null;
        String tenantName = ThreadLocalStorage.getTenantName();
        if(tenantName.toLowerCase().contains("egcity")) {
                System.out.println("Tenant name: " + tenantName);
                propertyTypes = new ArrayList<>(EnumSet.of(
                        HOUSE_2_BHK,
                        HOUSE_3_BHK,
                        PLOT,
                        MULTIPLE,
                        EMPTY
                ));
        }
        if(tenantName.toLowerCase().contains("suncitynx")) {
            propertyTypes = new ArrayList<>(EnumSet.of(
                    E_TYPE_5_BHK,
                    F_TYPE_5_BHK,
                    G_TYPE_4_BHK,
                    H_TYPE_4_BHK,
                    L_TYPE_3_BHK,
                    I_TYPE_3_BHK
            ));
        }

        if(tenantName.toLowerCase().contains("kalpavrish")) {
            propertyTypes = new ArrayList<>(EnumSet.of(
                    A_BLOCK_4_BHK,
                    B_BLOCK_3_BHK,
                    C_BLOCK_3_BHK,
                    D_BLOCK_2_BHK,
                    E_BLOCK_3_BHK
            ));
        }

        if(tenantName.toLowerCase().contains("smartcity")) {
            propertyTypes = new ArrayList<>(EnumSet.of(
                    A_BLOCK,
                    B_BLOCK,
                    C_BLOCK,
                    D_BLOCK,
                    E_BLOCK,
                    F_BLOCK,
                    L_BLOCK
            ));
        }

        if(tenantName.toLowerCase().contains("bhaavbhumi")) {
            propertyTypes = new ArrayList<>(EnumSet.of(
                    TYPE_1_6_BHK,
                    TYPE_2_5_BHK,
                    TYPE_3_5_BHK,
                    TYPE_3_4_BHK,
                    TYPE_4_5_BHK,
                    TYPE_4_4_BHK,
                    TYPE_5_4_BHK,
                    TYPE_6_3_BHK
            ));
        }

        if(tenantName.toLowerCase().contains("drgtrdcntr")) {
            propertyTypes = new ArrayList<>(EnumSet.of(
                    SARAFA,
                    GENERAL,
                    CHOPATI
            ));
        }

        if(propertyTypes==null){
            propertyTypes = new ArrayList<>(EnumSet.of(
                    HOUSE_2_BHK,
                    HOUSE_3_BHK,
                    PLOT,
                    MULTIPLE,
                    EMPTY
            ));
        }
        List<String> returnData = new ArrayList<>();
        propertyTypes.forEach(type -> returnData.add(type.toString()));
        return returnData;
    }
}
