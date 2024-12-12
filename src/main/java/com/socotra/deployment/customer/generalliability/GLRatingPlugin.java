package com.socotra.deployment.customer.generalliability;
import com.socotra.deployment.customer.*;
import com.socotra.coremodel.*;
import com.socotra.deployment.*;
import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.deployment.customer.GeneralLiabilityQuote;
import com.socotra.deployment.customer.ChargeType;
import com.socotra.deployment.customer.RatePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;


public class GLRatingPlugin implements RatePlugin {
    private static final Logger log = LoggerFactory.getLogger(GLRatingPlugin.class);

    @Override
    public RatingSet rate(GeneralLiabilityQuoteRequest generalLiabilityQuoteRequest) {

        GeneralLiabilityQuote quote = generalLiabilityQuoteRequest.quote();

        List<RatingItem> ratingItems = new ArrayList<>();

//        const limitOfInsuranceBI = 100000;
//        const limitOfInsurancePD = 100000;
//        const zipCode = 49202;
//        const state = 'MI';
        int classCode = 10010;
        int exposure = 8;
        String state = "MI";

        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);
        // retrieve prem-ops factor
        BigDecimal premOpsFactor = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().ratePremOps();
//        BigDecimal baseRate = premOpsFactor.multiply(BigDecimal.valueOf(exposure));
//        log.info(baseRate.toString());

        // retrieve lcm factor
        BigDecimal baseRateLCM = tableRecordFetcher.getTable(LCM.class).getRecord(LCM.makeKey(state)).orElseThrow().premOpsLCM();
//        log.info(baseRateLCM.toString());

        ratingItems.add(RatingItem.builder()
                .elementLocator(quote.locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(100))
                .build());

        log.info("rateInsured -> {}", ratingItems);

        RatingSet ratingSet = RatingSet.builder()
                .ok(true)
                .addRatingItems(ratingItems)
                .build();
        log.info("rate -> {}", ratingSet);
        return ratingSet;
    }

}