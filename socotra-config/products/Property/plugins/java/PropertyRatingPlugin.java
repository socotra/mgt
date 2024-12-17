package com.socotra.deployment.customer.property;
import com.socotra.deployment.customer.*;
import com.socotra.coremodel.*;
import com.socotra.deployment.*;
import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.deployment.customer.PropertyQuote;
import com.socotra.deployment.customer.ChargeType;
import com.socotra.deployment.customer.RatePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;


public class PropertyRatingPlugin implements RatePlugin {
    private static final Logger log = LoggerFactory.getLogger(PropertyRatingPlugin.class);

    @Override
    public RatingSet rate(PropertyQuoteRequest propertyQuoteRequest) {

        PropertyQuote quote = propertyQuoteRequest.quote();

        log.info("property quote", quote);

        List<RatingItem> ratingItems = new ArrayList<>();

//        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);
//
//        int zipCode = quote.data().packageQuestions().locationAddress_1().zipCode();
//        log.info("zip-code");
//        log.info(Integer.toString(zipCode));
//
//        String state = quote.data().packageQuestions().locationAddress_1().state();
//        log.info("state");
//        log.info(state);

//        coinsurancePercent
        // Loop through all buildings
        for (PropertyLocation loc : quote.propertyLocations()) {
            // Rating Location
            log.info("Rating the location: {}", loc.data());

//            for (PropertyBuilding building : loc.propertyBuildings()) {
//                log.info("Rating the building: {}", building.data().coinsurancePercent());
//                // Rating Building Coverage
////                rateBuilding(building);
//            }
        }
        // TODO        retrieve territory
        String territory = "Balance of State - Inland - AL";


        BigDecimal finalRate = new BigDecimal("700.0");
        log.info("finalRate");
        log.info(finalRate.toString());

        BigDecimal minimumPremium = new BigDecimal("700.0");
        BigDecimal months =new BigDecimal("12.0");

        if (finalRate.compareTo(minimumPremium) <= 0) {
            BigDecimal monthlyAmount = minimumPremium.divide(months, 2, RoundingMode.HALF_UP);
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(monthlyAmount)
                    .build());
        } else {
            BigDecimal monthlyAmount = finalRate.divide(months);
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(monthlyAmount)
                    .build());
        }

        log.info("rateInsured -> {}", ratingItems);

        RatingSet ratingSet = RatingSet.builder()
                .ok(true)
                .addRatingItems(ratingItems)
                .build();
        log.info("rate -> {}", ratingSet);
        return ratingSet;
    }

}