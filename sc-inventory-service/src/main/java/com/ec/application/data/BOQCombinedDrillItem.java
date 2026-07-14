package com.ec.application.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BOQCombinedDrillItem {
    private Long buildingTypeId;
    private String buildingTypeName;
    private Double boqTotal;
    private Double outwardTotal;
    private Double pct;          // outwardTotal / boqTotal * 100, null if no BOQ
    private List<LocationGroup> locations;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LocationGroup {
        private Long locationId;
        private String locationName;
        private Double boqTotal;
        private Double outwardTotal;
        private Double pct;
        private List<AreaEntry> areas;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AreaEntry {
        private Long areaId;
        private String areaName;
        private Double boqQty;
        private Double outwardQty;
        private Double pct;
    }
}
