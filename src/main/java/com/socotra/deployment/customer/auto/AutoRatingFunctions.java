package com.socotra.deployment.customer.auto;

import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.customer.BaseRate;
import com.socotra.deployment.customer.Discounts;
import com.socotra.deployment.customer.PlanOptions;

public class AutoRatingFunctions {
    ResourceSelector resourceSelector;
    public AutoRatingFunctions(ResourceSelector resourceSelector) {
        this.resourceSelector = resourceSelector;
    }

    public double getPlanOptionFactor(String planOption) {
        // Get Table
        var planOptionRecord = resourceSelector
                .getTable(PlanOptions.class)
                .getRecord(PlanOptions.makeKey(planOption));

        if (planOptionRecord.isPresent()) {
            return planOptionRecord.get().factor().doubleValue();
        } else if (planOption.equalsIgnoreCase("Silver")) {
            return 1.25;
        } else if (planOption.equalsIgnoreCase("Gold")) {
            return 1.48;
        } else {
            return 2.30;
        }
    }

    public double getVehicleBaseRate(String make, String model, String year) {

        var vehicleBaseRateRecord = resourceSelector
                .getTable(BaseRate.class)
                .getRecord(BaseRate.makeKey(make,model,year));

        return vehicleBaseRateRecord.map(baseRate -> baseRate.rate().doubleValue()).orElse(100.0);

    }

    public double getElectricDiscount(String fuelType) {

        var electricDiscountRecord = resourceSelector
                .getTable(Discounts.class)
                .getRecord(Discounts.makeKey("vehicleFuelType",fuelType));

        return electricDiscountRecord.map(baseRate -> baseRate.factor().doubleValue()).orElse(1.0);


    }
}
