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
        int classCode = 60010;
        int territory = 705;
        int exposure = 8;
        String state = "TX";
        int limitOfInsuranceBI =   100000;
        int limitOfInsurancePD =  100000;

        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);

        // retrieve value, cell L17
        int premiumBase = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().premiumBase();
        log.info(premiumBase.toString());

        // retrieve prem-ops factor
        BigDecimal premOpsFactor = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().ratePremOps();
        // calculate base-rate, cell I18
        BigDecimal baseRate = premOpsFactor.multiply(BigDecimal.valueOf(exposure))/premiumBase;
//        log.info(baseRate.toString());

        // retrieve lcm factor, cell I19
        BigDecimal baseRateLCM = tableRecordFetcher.getTable(LCM.class).getRecord(LCM.makeKey(state)).orElseThrow().premOpsLCM();
//        log.info(baseRateLCM.toString());

        // TODO: needs to sum these values
        BigDecimal baseRateTerritory = tableRecordFetcher.getTable(TerritoryRelativity.class).getRecord(TerritoryRelativity.makeKey(state, territory)).orElseThrow().premOpsFactors();

        // TODO: limit of liability SUMIFS
        // TODO: deductible SUMIFS
        // TODO: discount factors
        // TODO: affinity discount

        BigDecimal testDiscountFactor = 0.970;

        String packageGrp = "Single";
        BigDecimal packageRate = tableRecordFetcher.getTable(PackageProductTable.class).getRecord(PackageProductTable.makeKey(packageGrp)).orElseThrow().packageFctr();

        String claims = "None";
        BigDecimal claimsRate = tableRecordFetcher.getTable(PriorClaims.class).getRecord(PriorClaims.makeKey(claims)).orElseThrow().premOpsFctr();

        String terrorism = "Yes";
        BigDecimal terrorismRate = tableRecordFetcher.getTable(Terrorism.class).getRecord(Terrorism.makeKey(terrorism)).orElseThrow().factor();

        BigDecimal finalRate = terrorismRate*claimsRate*packageRate*baseRateTerritory*baseRateLCM*baseRate*testDiscountFactor;

        if (finalRate <= 500) {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(BigDecimal.valueOf(41.67))
                    .build());
        } else {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(BigDecimal.valueOf(finalRate/12))
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