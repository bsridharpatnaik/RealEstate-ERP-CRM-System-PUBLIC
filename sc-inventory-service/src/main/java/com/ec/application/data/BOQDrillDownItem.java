package com.ec.application.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BOQDrillDownItem {
    private Long buildingTypeId;
    private String buildingTypeName;
    private Double typeTotal;
    private List<LocationGroup> locations;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LocationGroup {
        private Long locationId;
        private String locationName;
        private Double locationTotal;
        private List<AreaEntry> areas;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AreaEntry {
        private Long areaId;
        private String areaName;
        private Double qty;
    }
}
