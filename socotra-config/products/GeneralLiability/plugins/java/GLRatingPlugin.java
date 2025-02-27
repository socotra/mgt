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

        BigDecimal testRate = new BigDecimal("700.0");
        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);
        {
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.premium)
                    .rate(testRate)
                    .build());
            ratingItems.add(RatingItem.builder()
                    .elementLocator(quote.locator())
                    .chargeType(ChargeType.tax)
                    .rate(testRate)
                    .build());
        }
        log.info("rateInsured -> {}", ratingItems);

        RatingSet ratingSet = RatingSet.builder()
                .ok(true)
                .addRatingItems(ratingItems)
                .build();
        log.info("rate -> {}", ratingSet);
        return ratingSet;

//        int zipCode = 0;
//        String state = "";
//        int territory = 0;
//        String classCode = "";
//        int premiumBase = 0;
//        BigDecimal premOpsFactor = new BigDecimal("0.0");
//        BigDecimal baseRate = new BigDecimal("0.0");
//        BigDecimal baseRateLCM = new BigDecimal("0.0");
//        BigDecimal baseRateTerritory = new BigDecimal("0.0");
//        BigDecimal limitOfInsuranceAgg = new BigDecimal("0.0");
//        BigDecimal lolRate = new BigDecimal("0.0");
//        BigDecimal medicalRate  = new BigDecimal("0.0");
//        BigDecimal lolFinalRate = new BigDecimal("0.0");
//        BigDecimal limitOfInsurancePerOccurrence = new BigDecimal("0.0");
//        BigDecimal limitOfInsuranceMed = new BigDecimal("0.0");
//        BigDecimal dedBI = new BigDecimal("0.0");
//        BigDecimal dedPD = new BigDecimal("0.0");
//        BigDecimal dedBIPD = new BigDecimal("0.0");
//        BigDecimal dedBIRate = new BigDecimal("0.0");
//        BigDecimal dedPDRate  = new BigDecimal("0.0");
//        BigDecimal dedBIPDRate = new BigDecimal("0.0");
//        BigDecimal finalDedRate = new BigDecimal("0.0");
//        BigDecimal lossOfElectronics = new BigDecimal("0.0");
//        BigDecimal cyberIncident = new BigDecimal("0.0");
//        BigDecimal terrorismRate = new BigDecimal("1.0");
//
//        // sum the deductible rate for each location
//        // sum the limit of insurance rate for each location
//        String occupancyType = quote.data().classOfBusiness().occupancyType();
//        log.info("occupancy type: {}", occupancyType);
//
//        for (Location loc : quote.locations()) {
//            if (loc.data().ordLawTria().contains("TRIA")) {
//                terrorismRate = tableRecordFetcher.getTable(Terrorism.class).getRecord(Terrorism.makeKey("Yes")).orElseThrow().factor();
//            }
//
//            // change to location address
//            zipCode = loc.data().packageProductBasicInfo().locationAddress().zipCode();
//            log.info("zip-code: {}", Integer.toString(zipCode));
//
//            state = loc.data().packageProductBasicInfo().locationAddress().state();
//            log.info("state: {}",  state);
//
//            territory = tableRecordFetcher.getTable(Territory.class).getRecord(Territory.makeKey(zipCode)).orElseThrow().territory();
//            log.info("territory: {}", Integer.toString(territory));
//
//            classCode = tableRecordFetcher.getTable(GLOccupancyClassCodes.class).getRecord(GLOccupancyClassCodes.makeKey(occupancyType)).orElseThrow().classCode();
//            if (classCode == null || classCode.length() == 0) {
//                classCode = loc.data().classCode();
//            }
//            log.info("class-code: {}", classCode);
//
//            // retrieve value, cell L17
//            // because of RTC values need to convert from decimal
//            premiumBase = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().premiumBase();
//            log.info("premium base from class codes: {} ", Integer.toString(premiumBase));
//
//            int exposureCount = loc.data().applicantsExposures();
//            log.info("exposure count: {}", Integer.toString(exposureCount));
//
//            // retrieve prem-ops factor
//            premOpsFactor = tableRecordFetcher.getTable(ClassCodes.class).getRecord(ClassCodes.makeKey(classCode)).orElseThrow().ratePremOps();
//            // calculate base-rate, cell I18
//            baseRate = (premOpsFactor.multiply(BigDecimal.valueOf(exposureCount))).divide(BigDecimal.valueOf(premiumBase));
//            log.info("base rate: {}", baseRate.toString());
//
//            // retrieve lcm factor, cell I19
//            baseRateLCM = tableRecordFetcher.getTable(LCM.class).getRecord(LCM.makeKey(state)).orElseThrow().premOpsLCM();
//            log.info("base rate lcm: {}", baseRateLCM.toString());
//
//            baseRateTerritory = tableRecordFetcher.getTable(TerritoryRelativity.class).getRecord(TerritoryRelativity.makeKey(state, territory)).orElseThrow().premOpsFactors();
//            log.info("base rate territory: {}", baseRateTerritory.toString());
//
//            // TODO: add additional coverages
//
//            limitOfInsuranceAgg = loc.limitOfInsuranceAggregate().value();
//            log.info("limit of insurance agg: {}", limitOfInsuranceAgg.toString());
//
//            limitOfInsurancePerOccurrence = loc.limitOfInsurancePerOccurrence().value();
//            limitOfInsuranceMed = loc.limitOfInsuranceMedical().value();
//
//            lolRate = tableRecordFetcher.getTable(IFL14.class).getRecord(IFL14.makeKey(limitOfInsurancePerOccurrence, limitOfInsuranceAgg)).orElseThrow().factorIFL();
//            log.info("limit of liability rate: {}", lolRate.toString());
//
//            medicalRate = tableRecordFetcher.getTable(IFL23.class).getRecord(IFL23.makeKey(limitOfInsuranceMed)).orElseThrow().factor();
//
//            lolFinalRate = lolRate.multiply(medicalRate).add(lolFinalRate);
//
//
//            log.info("limit of liability final rate: {}", lolFinalRate.toString());
//            // deductible bi
//            // deudcitble pd
//            // deductible bipd
//
//            dedBI = loc.liabilityDeductible().value();
//            log.info("ded-bi val: {}", dedBI.toString());
//            dedPD = loc.liabilityDeductible().value();
//            dedBIPD = loc.liabilityDeductible().value();
////
//            dedBIRate = tableRecordFetcher.getTable(DeductibleBI.class).getRecord(DeductibleBI.makeKey(dedBI)).orElseThrow().factorBI();
//            log.info("ded-bi rate: {}", dedBIRate.toString());
//            dedPDRate = tableRecordFetcher.getTable(DeductiblePD.class).getRecord(DeductiblePD.makeKey(dedPD)).orElseThrow().factorPD();
//            dedBIPDRate = tableRecordFetcher.getTable(DeductibleBIPD.class).getRecord(DeductibleBIPD.makeKey(dedBIPD)).orElseThrow().factorBIPD();
//
//            finalDedRate = dedBIRate.multiply(dedPDRate).multiply(dedBIPDRate).add(finalDedRate);
//            log.info("deductible rate: {}", finalDedRate.toString());
//
//            // TODO: claims made coverage default to NONE
//        }
//
//        // TODO: discount factors, tenure is for renewals only
//
//        // affinity discount
//        String affinityDiscount = quote.data().affinityDiscount();
//        BigDecimal affinityRate = tableRecordFetcher.getTable(DiscountFactorsAffinity.class).getRecord(DiscountFactorsAffinity.makeKey(affinityDiscount)).orElseThrow().factor();
//
//        // TODO: do not worry about rate capping...
//
//
//        // will always be single as this is the GL rater
//        String packageGrp = "Single";
//        BigDecimal packageRate = tableRecordFetcher.getTable(PackageProductTable.class).getRecord(PackageProductTable.makeKey(packageGrp)).orElseThrow().packageFctr();
//        log.info("package discount: {}", packageRate.toString());
//
//        // TODO: add IAM - no table currently
//        // TODO: add advance quote - no table currently
//
//        BigDecimal finalRate = (packageRate.multiply(baseRateTerritory).multiply(baseRateLCM).multiply(lolFinalRate).multiply(finalDedRate).multiply(affinityRate)).setScale(3, RoundingMode.CEILING);
//        log.info("final rate w/o TRIA: {}", finalRate.toString());
//
//        finalRate = (finalRate.multiply(baseRate)).setScale(3, RoundingMode.CEILING);
//        log.info("final premium: {}", finalRate.toString());
//
//        /* apply full pay discount */
//        BigDecimal fullPayDiscount = new BigDecimal(".950");
//        finalRate = (finalRate.multiply(fullPayDiscount)).setScale(3, RoundingMode.CEILING);
//        log.info("final premium w/ discount: {}", finalRate.toString());
//
//        BigDecimal terrorismCertificate = new BigDecimal("0.0");
//        terrorismCertificate = (terrorismRate.multiply(finalRate)).subtract(finalRate);
//        log.info("terrorism certificate amount: {}", terrorismCertificate.toString());
//
//        finalRate = (finalRate.multiply(terrorismRate)).setScale(3, RoundingMode.CEILING);
//        log.info("final premium w/ TRIA: {}", finalRate.toString());
//
//        BigDecimal minimumPremium = new BigDecimal("500.0");
//        log.info("minimumPremium: {}", minimumPremium);
//
//        // additional coverages
//        BigDecimal stateWideLCM = new BigDecimal("1.0");
//        for (Location loc : quote.locations()) {
//            classCode = tableRecordFetcher.getTable(GLOccupancyClassCodes.class).getRecord(GLOccupancyClassCodes.makeKey(occupancyType)).orElseThrow().classCode();
//            if (classCode == null || classCode.length() > 0) {
//                classCode = loc.data().classCode();
//            }
//            // get loss of electronic premium
//            BigDecimal currentCoveragePremium = new BigDecimal("0.0");
//            if (loc.lossOfElectronicDataCoverage() != null) {
//                BigDecimal lossOfElectronicPD = loc.lossOfElectronicDataCoverage().limitOfInsurancePerOccurrence().value();
//                log.info("lossOfElectronicPD: {}", lossOfElectronicPD.toString());
//
//                BigDecimal lossOfElectronicDed_PD = loc.lossOfElectronicDataCoverage().liabilityDeductible().value();
//                log.info("lossOfElectronic ded_PD: {}", lossOfElectronicDed_PD.toString());
//
//                if (lossOfElectronicPD.compareTo(limitOfInsurancePerOccurrence) == 1) {
//                    BigDecimal zero = new BigDecimal("0.0");
//                    lossOfElectronics.add(zero);
//                } else {
//                    BigDecimal lollossOfElectronicDataCoverageRate = tableRecordFetcher.getTable(IFL14.class).getRecord(IFL14.makeKey(lossOfElectronicPD, limitOfInsuranceAgg)).orElseThrow().factorIFL();
//                    log.info("lollossOfElectronicDataCoverage rate: {}", lollossOfElectronicDataCoverageRate.toString());
//
//                    BigDecimal lossOfElectronicDed_PDFactor = tableRecordFetcher.getTable(DeductiblePD.class).getRecord(DeductiblePD.makeKey(lossOfElectronicDed_PD)).orElseThrow().factorPD();
//
//                    // hazard factor
//                    String form = loc.lossOfElectronicDataCoverage().data().form();
//                    int hazardLevel = tableRecordFetcher.getTable(ElectronicHazardFactors.class).getRecord(ElectronicHazardFactors.makeKey(classCode)).orElseThrow().premOps();
//                    log.info("hazard level: {} ", hazardLevel);
//
//                    BigDecimal hazardFactor = tableRecordFetcher.getTable(ElectronicFactors.class).getRecord(ElectronicFactors.makeKey(form, hazardLevel)).orElseThrow().premOps();
//                    log.info("hazard rate: {} ", hazardFactor);
//                    currentCoveragePremium = (baseRate.multiply(baseRateLCM)
//                            .multiply(stateWideLCM)
//                            .multiply(baseRateTerritory)
//                            .multiply(lollossOfElectronicDataCoverageRate)
//                            .multiply(lossOfElectronicDed_PDFactor)
//                            .multiply(hazardFactor)).setScale(3, RoundingMode.CEILING);
//                    lossOfElectronics.add(currentCoveragePremium);
//
//                    log.info("current locations electronic premium: {} ", currentCoveragePremium.toString());
//                }
//            }
//
//            if (loc.cyberIncident() != null) {
//                BigDecimal cyberIncidentPD = loc.cyberIncident().limitOfInsurancePerOccurrence().value();
//                log.info("cyberIncidentPD: {}", cyberIncidentPD.toString());
//
//                BigDecimal cyberIncidentDed_PD = loc.cyberIncident().liabilityDeductible().value();
//                log.info("cyberIncident ded_PD: {}", cyberIncidentDed_PD.toString());
//
//                if (cyberIncidentPD.compareTo(limitOfInsurancePerOccurrence) == 1) {
//                    BigDecimal zero = new BigDecimal("0.0");
//                    cyberIncident.add(zero);
//                } else {
//                    BigDecimal cyberIncidentCoverageRate = tableRecordFetcher.getTable(IFL14.class).getRecord(IFL14.makeKey(cyberIncidentPD, limitOfInsuranceAgg)).orElseThrow().factorIFL();
//                    log.info("cyberIncidentCoverage rate: {}", cyberIncidentCoverageRate.toString());
//
//                    BigDecimal cyberIncidentDed_PDFactor = tableRecordFetcher.getTable(DeductiblePD.class).getRecord(DeductiblePD.makeKey(cyberIncidentDed_PD)).orElseThrow().factorPD();
//
//                    // hazard factor
//                    int hazardLevel = tableRecordFetcher.getTable(ElectronicHazardFactors.class).getRecord(ElectronicHazardFactors.makeKey(classCode)).orElseThrow().premOps();
//                    log.info("cyber-incident hazard level: {} ", hazardLevel);
//
//                    BigDecimal hazardFactor = tableRecordFetcher.getTable(CyberIncidentFactors.class).getRecord(CyberIncidentFactors.makeKey(hazardLevel)).orElseThrow().premOps();
//                    log.info("cyber incident hazard rate: {} ", hazardFactor);
//                    currentCoveragePremium = (baseRate.multiply(baseRateLCM)
//                            .multiply(stateWideLCM)
//                            .multiply(baseRateTerritory)
//                            .multiply(cyberIncidentCoverageRate)
//                            .multiply(cyberIncidentDed_PDFactor)
//                            .multiply(hazardFactor)).setScale(3, RoundingMode.CEILING);
//                    cyberIncident.add(currentCoveragePremium);
//
//                    log.info("current cyber-incident premium: {} ", currentCoveragePremium.toString());
//                }
//            }
//            log.info("additional loss of electronics premium: {} ", lossOfElectronics.toString());
//            log.info("additional cyber-incident premium: {} ", cyberIncident.toString());
//        }
//
//        finalRate = finalRate.add(lossOfElectronics).add(cyberIncident);
//        log.info("final rate with additional charges: {}", finalRate);
//
//        // taxes
//        BigDecimal allTaxes = new BigDecimal("0.00");
//        BigDecimal carrierPolicyFeeAmount = new BigDecimal("0.00");
//        BigDecimal stampingFeeAmount = new BigDecimal("0.00");
//        BigDecimal brokerFeeAmount = new BigDecimal("0.00");
//        BigDecimal installmentFeeAmount = new BigDecimal("0.00");
//        BigDecimal taxAmounts = new BigDecimal("0.00");
//        for (Location loc : quote.locations()) {
//            BigDecimal oneHundredth = new BigDecimal("100.00");
//            state = loc.data().packageProductBasicInfo().locationAddress().state();
//            log.info("state: {}", state);
//
//            BigDecimal premiumRisk = tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().minEP();
//            if (finalRate.compareTo(premiumRisk) == 1) {
//                premiumRisk = finalRate;
//            }
//            log.info("premium risk: {} ", premiumRisk.toString());
//            BigDecimal surplusLinesTax = (tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().surplusLinesTax().divide(oneHundredth)).multiply(premiumRisk);
//            taxAmounts = taxAmounts.add(surplusLinesTax);
//            log.info("surplus lines tax: {} ", surplusLinesTax.toString());
//
//            BigDecimal miscTax = tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().miscTax();
//            taxAmounts = taxAmounts.add(miscTax);
//            log.info("misc tax: {} ", miscTax.toString());
//
//            BigDecimal stampingFee = (tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().stampingFee()).multiply(premiumRisk);
//            stampingFeeAmount = stampingFeeAmount.add(stampingFee);
//            log.info("stamping fee: {} ", stampingFee.toString());
//
//            int carrierPolicyFeeAsInt = tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().policyFee();
//            BigDecimal carrierPolicyFee = BigDecimal.valueOf(carrierPolicyFeeAsInt);
//            carrierPolicyFeeAmount = carrierPolicyFeeAmount.add(carrierPolicyFee);
//            log.info("carrier policy fee: {} ", carrierPolicyFee.toString());
//
//            int brokerFeeAsInt = tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().brokerFee();
//            BigDecimal brokerFee = BigDecimal.valueOf(brokerFeeAsInt);
//            brokerFeeAmount = brokerFeeAmount.add(brokerFee);
//            log.info("broker fee: {} ", brokerFee.toString());
//
//            BigDecimal installmentFee = ((tableRecordFetcher.getTable(TaxFeesGL.class).getRecord(TaxFeesGL.makeKey(state)).orElseThrow().installmentFee()).divide(oneHundredth)).multiply(premiumRisk);
//            installmentFeeAmount = installmentFeeAmount.add(installmentFee);
//            log.info("installment fee: {} ", installmentFee.toString());
//
//            BigDecimal currentTaxesFees = premiumRisk.add(surplusLinesTax).add(miscTax).add(stampingFee).add(brokerFee).add(installmentFee).add(carrierPolicyFee);
//            log.info("current tax + fee amount: {} ", currentTaxesFees.toString());
//
//            allTaxes = allTaxes.add(taxAmounts);
//            log.info("final tax amountt: {} ", allTaxes.toString());
//        }
//
//        if (minimumPremium.compareTo(finalRate) == 1) {
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.premium)
//                    .rate(minimumPremium.subtract(terrorismCertificate))
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.tax)
//                    .rate(allTaxes)
//                    .build());
//             ratingItems.add(RatingItem.builder()
//                     .elementLocator(quote.locator())
//                     .chargeType(ChargeType.carrierPolicyFee)
//                     .rate(carrierPolicyFeeAmount)
//                     .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.stampingFee)
//                    .rate(stampingFeeAmount)
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.brokerFee)
//                    .rate(brokerFeeAmount)
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.installmentFee)
//                    .rate(installmentFeeAmount)
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.terrorism)
//                    .rate(terrorismCertificate)
//                    .build());
//        } else {
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.premium)
//                    .rate(finalRate.subtract(terrorismCertificate))
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.tax)
//                    .rate(allTaxes)
//                    .build());
//             ratingItems.add(RatingItem.builder()
//                     .elementLocator(quote.locator())
//                     .chargeType(ChargeType.carrierPolicyFee)
//                     .rate(carrierPolicyFeeAmount)
//                     .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.stampingFee)
//                    .rate(stampingFeeAmount)
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.brokerFee)
//                    .rate(brokerFeeAmount)
//                    .build());
//            ratingItems.add(RatingItem.builder()
//                    .elementLocator(quote.locator())
//                    .chargeType(ChargeType.terrorism)
//                    .rate(terrorismCertificate)
//                    .build());
//        }
//
//        log.info("rateInsured -> {}", ratingItems);
//
//        RatingSet ratingSet = RatingSet.builder()
//                .ok(true)
//                .addRatingItems(ratingItems)
//                .build();
//        log.info("rate -> {}", ratingSet);
//        return ratingSet;
    }


}