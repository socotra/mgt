package com.socotra.deployment.customer.mobilitypackage;

public enum PlanOption {
    SILVER, GOLD, PLATINUM;

    /**
     * Converts a string to a PlanOption enum, case-insensitive.
     * Throws an IllegalArgumentException if the string doesn't match any valid plan.
     */
    public static PlanOption fromString(String planOption) {
        switch (planOption.toUpperCase()) {
            case "SILVER":
                return SILVER;
            case "GOLD":
                return GOLD;
            case "PLATINUM":
                return PLATINUM;
            default:
                throw new IllegalArgumentException("Invalid plan option: " + planOption);
        }
    }
}
