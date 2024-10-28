package com.socotra.deployment.customer.edpm;

import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.customer.BikeRate;

public class EDPMRatingFunctions {
    ResourceSelector resourceSelector;
    public EDPMRatingFunctions(ResourceSelector resourceSelector) {
        this.resourceSelector = resourceSelector;
    }

    public double getEPDMBaseRate(String edpmType, String make, String year) {

        var bikeBaseRateRecord = resourceSelector
                .getTable(BikeRate.class)
                .getRecord(BikeRate.makeKey(edpmType,make,year));

        return bikeBaseRateRecord.map(baseRate -> baseRate.rate().doubleValue()).orElse(55.0);
    }


}
