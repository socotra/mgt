package com.socotra.deployment.customer;

import com.socotra.coremodel.RatingItem;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.customer.auto.AutoRatingFunctions;
import com.socotra.deployment.customer.edpm.EDPMRatingFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Rater {

    ResourceSelector resourceSelector;
    AutoRatingFunctions autoRatingFunctions;
    EDPMRatingFunctions epdmRatingFunctions;

    private static final Logger log = LoggerFactory.getLogger(Rater.class);

    public Rater(ResourceSelector resourceSelector) {
        this.resourceSelector = resourceSelector;
        this.autoRatingFunctions = new AutoRatingFunctions(resourceSelector);
        this.epdmRatingFunctions = new EDPMRatingFunctions(resourceSelector);
    }

    public List<RatingItem> rateQuickQuoteMotor(MotorLine motorLineQuickQuote) {

        log.info("rating quick quote");

        List<RatingItem> ratingItems = new ArrayList<>();

        // Get Vehicle Base Rate
        BigDecimal baseRate = BigDecimal.valueOf(autoRatingFunctions.getVehicleBaseRate(
                motorLineQuickQuote.vehicle().data().vehicleMake(),
                motorLineQuickQuote.vehicle().data().vehicleModel(),
                motorLineQuickQuote.vehicle().data().vehicleYear()
        ));

        // Get Plan Option Factor
        String planOption = motorLineQuickQuote.data().planOption();
        BigDecimal planOptionFactor = BigDecimal.valueOf(autoRatingFunctions.getPlanOptionFactor(planOption));
        BigDecimal premium = baseRate.multiply(planOptionFactor);

        ratingItems.add(RatingItem.builder()
                .elementLocator(motorLineQuickQuote.vehicle().locator())
                .chargeType(ChargeType.premium)
                .rate(premium)
                .build());

        return ratingItems;
    }

    public List<RatingItem> rateMotor(MotorLine motorLine) {

        List<RatingItem> ratingItems = new ArrayList<>();

        // Get Vehicle Base Rate
        BigDecimal baseRate = BigDecimal.valueOf(autoRatingFunctions.getVehicleBaseRate(
                motorLine.vehicle().data().vehicleMake(),
                motorLine.vehicle().data().vehicleModel(),
                motorLine.vehicle().data().vehicleYear()
        ));

        log.info("Base rate calculated as {}", baseRate);


        // Get Plan Option Factor
        String planOption = motorLine.data().planOption();
        BigDecimal planOptionFactor = BigDecimal.valueOf(autoRatingFunctions.getPlanOptionFactor(planOption));
        // Calculate premium and round with RoundingMode.HALF_UP
        BigDecimal premium = baseRate.multiply(planOptionFactor)
                .setScale(0, RoundingMode.HALF_UP);
        log.info("Plan option factor as {}", planOptionFactor);
        log.info("Total premium as {}", premium);

        ratingItems.add(RatingItem.builder()
                .elementLocator(motorLine.vehicle().locator())
                .chargeType(ChargeType.premium)
                .rate(premium)
                .build());

        // Apply Electric Discount
        applyElectricDiscount(ratingItems, motorLine, premium);

        return ratingItems;
    }

    private void applyElectricDiscount(List<RatingItem> ratingItems, MotorLine motorLine, BigDecimal premium) {
        BigDecimal electricDiscountFactor = BigDecimal.valueOf(autoRatingFunctions.getElectricDiscount(
                motorLine.vehicle().data().vehicleFuelType()));

        BigDecimal electricDiscountPremium = premium.multiply(BigDecimal.ONE.subtract(electricDiscountFactor))
                .negate().setScale(0, RoundingMode.HALF_UP);;

        log.info("Electric discount factor as {}", electricDiscountFactor);
        log.info("Electric discount premium as {}", electricDiscountPremium);

        if (electricDiscountPremium.compareTo(BigDecimal.ZERO) != 0) {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(motorLine.vehicle().locator())
                    .chargeType(ChargeType.electricDiscount)
                    .rate(electricDiscountPremium)
                    .build());
        }
    }

    public List<RatingItem> rateEPDM(EDPMLine epdmLine) {
        List<RatingItem> ratingItems = new ArrayList<>();

        // Safely process bikes
        Optional.ofNullable(epdmLine.bikes()).ifPresent(bikes -> {
            bikes.forEach(bike -> {
                BigDecimal baseRate = BigDecimal.valueOf(epdmRatingFunctions.getEPDMBaseRate(
                        "Bike", bike.data().make(), bike.data().purchaseYear()));

                ratingItems.add(RatingItem.builder()
                        .elementLocator(bike.locator())
                        .chargeType(ChargeType.premium)
                        .rate(baseRate)
                        .build());
            });
        });

        // Safely process scooters
        Optional.ofNullable(epdmLine.scooters()).ifPresent(scooters -> {
            scooters.forEach(scooter -> {
                BigDecimal baseRate = BigDecimal.valueOf(epdmRatingFunctions.getEPDMBaseRate(
                        "Scooter", scooter.data().make(), scooter.data().purchaseYear()));

                ratingItems.add(RatingItem.builder()
                        .elementLocator(scooter.locator())
                        .chargeType(ChargeType.premium)
                        .rate(baseRate)
                        .build());
            });
        });
        return ratingItems;
    }

}
