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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;


public class PropertyRatingPlugin implements RatePlugin {
    private static final Logger log = LoggerFactory.getLogger(PropertyRatingPlugin.class);

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

        int exposure = quote.data().propertyQuestions().exposureCount();
        log.info("exposure");
        log.info(Integer.toString(exposure));

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
        log.info("base rate lcm");
        log.info(baseRateLCM.toString());

        BigDecimal baseRateTerritory = tableRecordFetcher.getTable(TerritoryRelativity.class).getRecord(TerritoryRelativity.makeKey(state, territory)).orElseThrow().premOpsFactors();
        log.info("base rate territory");
        log.info(baseRateTerritory.toString());

        // TODO: limit of liability SUMIFS
        BigDecimal limitOfInsuranceAgg = quote.location().limitOfInsuranceAggregate().value();
        log.info("limit of insurance agg");
        log.info(limitOfInsuranceAgg.toString());
        BigDecimal limitOfInsurancePerOccurrenceBI = quote.location().limitOfInsurancePerOccurrenceBI().value();
        BigDecimal limitOfInsurancePerOccurrencePD = quote.location().limitOfInsurancePerOccurrencePD().value();
        BigDecimal limitOfInsuranceMed = quote.location().limitOfInsuranceMedical().value();

        BigDecimal lolRate = new BigDecimal("0.0");
        BigDecimal medicalRate  = new BigDecimal("0.0");
        BigDecimal lolFinalRate = new BigDecimal("0.0");
        if(limitOfInsurancePerOccurrenceBI.compareTo(limitOfInsurancePerOccurrencePD) == 0) {
            lolRate = tableRecordFetcher.getTable(IFL14.class).getRecord(IFL14.makeKey(limitOfInsurancePerOccurrenceBI, limitOfInsuranceAgg)).orElseThrow().factorIFL();
            log.info("lol rate");
            log.info(lolRate.toString());

            medicalRate = tableRecordFetcher.getTable(IFL23.class).getRecord(IFL23.makeKey(limitOfInsuranceMed)).orElseThrow().factor();

            lolFinalRate = lolRate.multiply(medicalRate);
        }
        log.info("lol final rate");
        log.info(lolFinalRate.toString());

        // deductible bi
        // deudcitble pd
        // deductible bipd

        BigDecimal dedBI = quote.location().deductible_BI().value();
        log.info("ded-bi val");
        log.info(dedBI.toString());
        BigDecimal dedPD = quote.location().deductible_PD().value();
        BigDecimal dedBIPD = quote.location().deductible_BIPD().value();
//
        BigDecimal dedBIRate = tableRecordFetcher.getTable(DeductibleBI.class).getRecord(DeductibleBI.makeKey(dedBI)).orElseThrow().factorBI();
        log.info("ded-bi rate");
        log.info(dedBIRate.toString());
        BigDecimal dedPDRate = tableRecordFetcher.getTable(DeductiblePD.class).getRecord(DeductiblePD.makeKey(dedPD)).orElseThrow().factorPD();
        BigDecimal dedBIPDRate = tableRecordFetcher.getTable(DeductibleBIPD.class).getRecord(DeductibleBIPD.makeKey(dedBIPD)).orElseThrow().factorBIPD();

        BigDecimal finalDedRate = dedBIRate.multiply(dedPDRate).multiply(dedBIPDRate);
        log.info("deductible rate");
        log.info(finalDedRate.toString());


        // TODO: discount factors, tenure is for renewals only

        // affinity discount
        String affinityDiscount = quote.data().propertyQuestions().affinityDiscount();
        BigDecimal affinityRate = tableRecordFetcher.getTable(DiscountFactorsAffinity.class).getRecord(DiscountFactorsAffinity.makeKey(affinityDiscount)).orElseThrow().factor();

        // will always be single as this is the property rater
        String packageGrp = "Single";
        BigDecimal packageRate = tableRecordFetcher.getTable(PackageProductTable.class).getRecord(PackageProductTable.makeKey(packageGrp)).orElseThrow().packageFctr();

        int claimCount = quote.data().packageQuestions().claimHistory().size();
        log.info("claim count");
        log.info(Integer.toString(claimCount));
        BigDecimal claimsRate = tableRecordFetcher.getTable(PriorClaims.class).getRecord(PriorClaims.makeKey(claimCount)).orElseThrow().premOpsFctr();

        // TODO: see where this goes into the product
        String terrorism = "Yes";
        BigDecimal terrorismRate = tableRecordFetcher.getTable(Terrorism.class).getRecord(Terrorism.makeKey(terrorism)).orElseThrow().factor();

        BigDecimal finalRate = terrorismRate.multiply(claimsRate).multiply(packageRate).multiply(baseRateTerritory).multiply(baseRateLCM).multiply(baseRate).multiply(baseRateLCM).multiply(lolFinalRate).multiply(finalDedRate).multiply(affinityRate);
        log.info("finalRate");
        log.info(finalRate.toString());

        BigDecimal minimumPremium = new BigDecimal("500.0");
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