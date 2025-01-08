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


public class GLRatingPlugin implements RatePlugin {
    private static final Logger log = LoggerFactory.getLogger(GLRatingPlugin.class);

    @Override
    public RatingSet rate(GeneralLiabilityQuoteRequest generalLiabilityQuoteRequest) {

        GeneralLiabilityQuote quote = generalLiabilityQuoteRequest.quote();

        List<RatingItem> ratingItems = new ArrayList<>();

        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);

        // TODO: limit of liability SUMIFS
        int zipCode = 0;
        String state = "";
        int territory = 0;
        int classCode = 0;
        int premiumBase = 0;
        BigDecimal premOpsFactor = new BigDecimal("0.0");
        BigDecimal baseRate = new BigDecimal("0.0");
        BigDecimal baseRateLCM = new BigDecimal("0.0");
        BigDecimal baseRateTerritory = new BigDecimal("0.0");
        BigDecimal limitOfInsuranceAgg = new BigDecimal("0.0");
        BigDecimal lolRate = new BigDecimal("0.0");
        BigDecimal medicalRate  = new BigDecimal("0.0");
        BigDecimal lolFinalRate = new BigDecimal("0.0");
        BigDecimal limitOfInsurancePerOccurrenceBI = new BigDecimal("0.0");
        BigDecimal limitOfInsurancePerOccurrencePD = new BigDecimal("0.0");
        BigDecimal limitOfInsuranceMed = new BigDecimal("0.0");
        BigDecimal dedBI = new BigDecimal("0.0");
        BigDecimal dedPD = new BigDecimal("0.0");
        BigDecimal dedBIPD = new BigDecimal("0.0");
        BigDecimal dedBIRate = new BigDecimal("0.0");
        BigDecimal dedPDRate  = new BigDecimal("0.0");
        BigDecimal dedBIPDRate = new BigDecimal("0.0");
        BigDecimal finalDedRate = new BigDecimal("0.0");
        BigDecimal lossOfElectronics = new BigDecimal("0.0");

        // sum the deductible rate for each location
        // sum the limit of insurance rate for each location
        int exposure = quote.locations().size();
        log.info("exposure");
        log.info(Integer.toString(exposure));

        for (Location loc : quote.locations()) {
            // change to location address
            zipCode = loc.data().locationAddress().zipCode();
            log.info("zip-code");
            log.info(Integer.toString(zipCode));

            state = loc.data().locationAddress().state();
            log.info("state");
            log.info(state);

            territory = tableRecordFetcher.getTable(Territory.class).getRecord(Territory.makeKey(zipCode)).orElseThrow().territory();
            log.info("territory");
            log.info(Integer.toString(territory));

            classCode = loc.data().classCode();
            log.info("class-code");
            log.info(Integer.toString(classCode));

            // retrieve value, cell L17
            // because of RTC values need to convert from decimal
            premiumBase = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().premiumBase();
            log.info("premium base from class codes: {} ", Integer.toString(premiumBase));

            // retrieve prem-ops factor
            premOpsFactor = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().ratePremOps();
            // calculate base-rate, cell I18
            baseRate = (premOpsFactor.multiply(BigDecimal.valueOf(exposure))).divide(BigDecimal.valueOf(premiumBase));
            log.info(baseRate.toString());

            // retrieve lcm factor, cell I19
            baseRateLCM = tableRecordFetcher.getTable(LCM.class).getRecord(LCM.makeKey(state)).orElseThrow().premOpsLCM();
            log.info("base rate lcm");
            log.info(baseRateLCM.toString());

            baseRateTerritory = tableRecordFetcher.getTable(TerritoryRelativity.class).getRecord(TerritoryRelativity.makeKey(state, territory)).orElseThrow().premOpsFactors();
            log.info("base rate territory");
            log.info(baseRateTerritory.toString());

            // TODO: add additional coverages

            limitOfInsuranceAgg = loc.limitOfInsuranceAggregate().value();
            log.info("limit of insurance agg");
            log.info(limitOfInsuranceAgg.toString());

            limitOfInsurancePerOccurrenceBI = loc.limitOfInsurancePerOccurrenceBI().value();
            limitOfInsurancePerOccurrencePD = loc.limitOfInsurancePerOccurrencePD().value();
            limitOfInsuranceMed = loc.limitOfInsuranceMedical().value();

            if(limitOfInsurancePerOccurrenceBI.compareTo(limitOfInsurancePerOccurrencePD) == 0) {
                lolRate = tableRecordFetcher.getTable(IFL14.class).getRecord(IFL14.makeKey(limitOfInsurancePerOccurrenceBI, limitOfInsuranceAgg)).orElseThrow().factorIFL();
                log.info("lol rate");
                log.info(lolRate.toString());

                medicalRate = tableRecordFetcher.getTable(IFL23.class).getRecord(IFL23.makeKey(limitOfInsuranceMed)).orElseThrow().factor();

                lolFinalRate = lolRate.multiply(medicalRate).add(lolFinalRate);

            }
            log.info("lol final rate");
            log.info(lolFinalRate.toString());
            // deductible bi
            // deudcitble pd
            // deductible bipd

            dedBI = loc.deductible_BI().value();
            log.info("ded-bi val");
            log.info(dedBI.toString());
            dedPD = loc.deductible_PD().value();
            dedBIPD = loc.deductible_BIPD().value();
//
            dedBIRate = tableRecordFetcher.getTable(DeductibleBI.class).getRecord(DeductibleBI.makeKey(dedBI)).orElseThrow().factorBI();
            log.info("ded-bi rate");
            log.info(dedBIRate.toString());
            dedPDRate = tableRecordFetcher.getTable(DeductiblePD.class).getRecord(DeductiblePD.makeKey(dedPD)).orElseThrow().factorPD();
            dedBIPDRate = tableRecordFetcher.getTable(DeductibleBIPD.class).getRecord(DeductibleBIPD.makeKey(dedBIPD)).orElseThrow().factorBIPD();

            finalDedRate = dedBIRate.multiply(dedPDRate).multiply(dedBIPDRate).add(finalDedRate);
            log.info("deductible rate");
            log.info(finalDedRate.toString());
        }

        // TODO: discount factors, tenure is for renewals only

        // affinity discount
        String affinityDiscount = quote.data().glQuestions().affinityDiscount();
        BigDecimal affinityRate = tableRecordFetcher.getTable(DiscountFactorsAffinity.class).getRecord(DiscountFactorsAffinity.makeKey(affinityDiscount)).orElseThrow().factor();

        // will always be single as this is the GL rater
        String packageGrp = "Single";
        BigDecimal packageRate = tableRecordFetcher.getTable(PackageProductTable.class).getRecord(PackageProductTable.makeKey(packageGrp)).orElseThrow().packageFctr();
        log.info("package discount: {}", packageRate.toString());

        // TODO: add IAM - no table currently
        // TODO: add advance quote - no table currently

        // TODO: see where this goes into the product
        String terrorism = quote.data().packageQuestions().terrorismCoverage();
        BigDecimal terrorismRate = tableRecordFetcher.getTable(Terrorism.class).getRecord(Terrorism.makeKey(terrorism)).orElseThrow().factor();

        BigDecimal finalRate = (terrorismRate.multiply(packageRate).multiply(baseRateTerritory).multiply(baseRateLCM).multiply(lolFinalRate).multiply(finalDedRate).multiply(affinityRate)).setScale(3, RoundingMode.CEILING);
        log.info("final rate: {}", finalRate.toString());

        finalRate = (finalRate.multiply(baseRate)).setScale(0, RoundingMode.CEILING);
        log.info("final premium: {}", finalRate.toString());

        /* apply full pay discount */
        BigDecimal fullPayDiscount = new BigDecimal(".950");
        finalRate = (finalRate.multiply(fullPayDiscount)).setScale(0, RoundingMode.CEILING);
        log.info("final premium w/ discount: {}", finalRate.toString());

        BigDecimal minimumPremium = new BigDecimal("500.0");
        log.info("minimumPremium: {}", minimumPremium);

        // additional coverages
        BigDecimal stateWideLCM = new BigDecimal("1.0");
        for (Location loc : quote.locations()) {
            // get loss of electronic premium
            BigDecimal currentCoveragePremium = new BigDecimal("0.0");
            if (loc.lossOfElectronicDataCoverage()) {
                BigDecimal lossOfElectronicPD = loc.lossOfElectronicDataCoverage().limitOfInsurancePerOccurrencePD().value();
                log.info("lossOfElectronicPD: {}", lossOfElectronicPD.toString());

                BigDecimal lossOfElectronicDed_PD = loc.lossOfElectronicDataCoverage().deductible_PD().value();
                log.info("lossOfElectronic ded_PD: {}", lossOfElectronicded_PD.toString());

                if (lossOfElectronicPD.compareTo(limitOfInsurancePerOccurrencePD) == 1) {
                    BigDecimal zero = new BigDecimal("0.0");
                    lossOfElectronics.add(zero);
                } else {
                    BigDecimal lollossOfElectronicDataCoverageRate = tableRecordFetcher.getTable(IFL14.class).getRecord(IFL14.makeKey(lossOfElectronicPD, limitOfInsuranceAgg)).orElseThrow().factorIFL();
                    log.info("lollossOfElectronicDataCoverage rate: {}", lollossOfElectronicDataCoverageRate.toString());

                    BigDecimal lossOfElectronicDed_PDFactor = tableRecordFetcher.getTable(DeductiblePD.class).getRecord(DeductiblePD.makeKey(lossOfElectronicDed_PD)).orElseThrow().factorPD();
                    currentCoveragePremium = baseRate.multiply(baseRateLCM).multiply(stateWideLCM).multiply(baseRateTerritory).multiply(lollossOfElectronicDataCoverageRate).multiply(lossOfElectronicDed_PDFactor);
                    // TODO: hazard factor
                    lossOfElectronics.add(currentCoveragePremium);
                }
            }
        }


        if (minimumPremium.compareTo(finalRate) == 1) {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(minimumPremium)
                    .build());
        } else {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(finalRate)
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