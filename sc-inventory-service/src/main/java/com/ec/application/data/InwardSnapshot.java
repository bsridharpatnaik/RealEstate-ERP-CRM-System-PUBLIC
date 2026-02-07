package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InwardSnapshot {
    private Long inwardId;
    private Date inwardDate;
    private List<InwardLineSnapshot> lines = new ArrayList<>();
}
