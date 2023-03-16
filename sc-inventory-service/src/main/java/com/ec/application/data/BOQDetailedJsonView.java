package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BOQDetailedJsonView {
    Double status;
    Long finalLocationId;
    String finalLocationName;
    Double total_boq_quantity;
    Double total_outward_quantity;
}
