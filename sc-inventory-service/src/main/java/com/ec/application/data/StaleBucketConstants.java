package com.ec.application.data;

import java.util.ArrayList;
import java.util.List;

public final class StaleBucketConstants {

    private StaleBucketConstants() {
    }

    public static List<IdNameDTO> getAllBuckets() {
        List<IdNameDTO> list = new ArrayList<>();
        for (StaleAgeBucket bucket : StaleAgeBucket.values()) {
            list.add(new IdNameDTO(bucket.name(), bucket.getLabel()));
        }
        return list;
    }
}
