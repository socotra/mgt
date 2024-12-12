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

        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);

        int zipCode = quote.data().packageQuestions().locationAddress_1().zipCode();
        log.info("zip-code");
        log.info(Integer.toString(zipCode));

        String state = quote.data().packageQuestions().locationAddress_1().state();
        log.info("state");
        log.info(state);

        int territory = tableRecordFetcher.getTable(Territory.class).getRecord(Territory.makeKey(zipCode)).orElseThrow().territory();
        log.info("territory");
        log.info(Integer.toString(territory));

        int classCode = quote.data().packageQuestions().classCode();
        log.info("class-code");
        log.info(Integer.toString(classCode));

        int exposure = quote.data().glQuestions().exposureCount();
        log.info("exposure");
        log.info(Integer.toString(exposure));

        int limitOfInsuranceBI =   100000;
        int limitOfInsurancePD =  100000;

        // retrieve value, cell L17
        int premiumBase = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().premiumBase();
        log.info(Integer.toString(premiumBase));

        // retrieve prem-ops factor
        BigDecimal premOpsFactor = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().ratePremOps();
        // calculate base-rate, cell I18
        BigDecimal baseRate = (premOpsFactor.multiply(BigDecimal.valueOf(exposure))).divide(BigDecimal.valueOf(premiumBase));
        log.info(baseRate.toString());

        // retrieve lcm factor, cell I19
        BigDecimal baseRateLCM = tableRecordFetcher.getTable(LCM.class).getRecord(LCM.makeKey(state)).orElseThrow().premOpsLCM();
        log.info(baseRateLCM.toString());

        // TODO: potentially need to sum these values
        BigDecimal baseRateTerritory = tableRecordFetcher.getTable(TerritoryRelativity.class).getRecord(TerritoryRelativity.makeKey(state, territory)).orElseThrow().premOpsFactors();
        // TODO: limit of liability SUMIFS
        // TODO: deductible SUMIFS
        // TODO: discount factors
        // TODO: affinity discount

        BigDecimal testDiscountFactor=new BigDecimal(".970");

        // will always be single as this is the GL rater
        String packageGrp = "Single";
        BigDecimal packageRate = tableRecordFetcher.getTable(PackageProductTable.class).getRecord(PackageProductTable.makeKey(packageGrp)).orElseThrow().packageFctr();

        int claimCount = quote.data().packageQuestions().claimHistory().size();
        log.info("claim count");
        log.info(Integer.toString(claimCount));
        BigDecimal claimsRate = tableRecordFetcher.getTable(PriorClaims.class).getRecord(PriorClaims.makeKey(claimCount)).orElseThrow().premOpsFctr();

        // TODO: see where this goes into the product
        String terrorism = "Yes";
        BigDecimal terrorismRate = tableRecordFetcher.getTable(Terrorism.class).getRecord(Terrorism.makeKey(terrorism)).orElseThrow().factor();

        BigDecimal finalRate = terrorismRate.multiply(claimsRate).multiply(packageRate).multiply(baseRateTerritory).multiply(baseRateLCM).multiply(baseRate).multiply(baseRateLCM).multiply(testDiscountFactor);
        log.info("finalRate");
        log.info(finalRate.toString());

        BigDecimal minimumPremium = new BigDecimal("500.0");
        if (finalRate.compareTo(minimumPremium) <= 0) {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(BigDecimal.valueOf(41.67))
                    .build());
        } else {
            BigDecimal months =new BigDecimal("12.0");
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