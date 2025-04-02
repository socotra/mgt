package com.socotra.deployment.customer.packageproduct;

import com.socotra.coremodel.DocumentDataSnapshot;
import com.socotra.coremodel.QuotePricing;
import com.socotra.deployment.DataFetcherFactory;
import com.socotra.deployment.customer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.text.*;
import java.math.BigDecimal;

public class PackageDocSnapshotPlugin implements DocumentDataSnapshotPlugin {

    private static final Logger log = LoggerFactory.getLogger(DocumentDataSnapshotPlugin.class);

    @Override
    public DocumentDataSnapshot dataSnapshot(PackageProductQuoteRequest packageProductQuoteRequest) {
        var quote = packageProductQuoteRequest.quote();
        var pricing = DataFetcherFactory.get().getQuotePricing(quote.locator());
        // NOTE: NOT WORKING
        Map<String, Object> pricingData = getPricingData(pricing);
        Map<String, Object> locationData = new HashMap<>();
        Map<String, Object> combinedData = new HashMap<>();
        Applicant account = DataFetcherFactory.get().getAccount(quote.accountLocator());

        log.info("quote: {}", quote);

        String affinityDiscount = quote.data().affinityDiscount();
        if (!affinityDiscount.equalsIgnoreCase("None")) {
            combinedData.put("hasAffinityDiscount", "Yes");
        }
        combinedData.put("isPackage", "Yes");

        combinedData.put("applicantName", account.data().applicantName());
        combinedData.put("applicantPhoneNumber", account.data().applicantPhoneNumber());
        combinedData.put("line1", account.data().applicantMailingAddress().line1());
        combinedData.put("city", account.data().applicantMailingAddress().city());
        combinedData.put("state", account.data().applicantMailingAddress().state());
        combinedData.put("zipCode", account.data().applicantMailingAddress().zipCode());
        combinedData.put("applicantLegalEntity", account.data().applicantLegalEntity());

        // // Policy Data Mapping
        String productName = "Commercial Package Policy";

        // get today
        String pattern = "MM/dd/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.
        String todayAsString = df.format(today);

        // TODO: get effective to and from dates in readable format
        // Policy Effective From: 2025-01-31T18:26:18Z
        //
        combinedData.put("productName", productName);
        combinedData.put("quote", quote);
        combinedData.put("currentDay", todayAsString);
        combinedData.put("pricing", pricingData);
        combinedData.put("occupancyType", quote.data().classOfBusiness().occupancyType());

        // form data
        if (quote.data().packageOptionalForms() != null && quote.data().packageOptionalForms().size() > 0) {
            for (PackageOptionalForms form : quote.data().packageOptionalForms()) {
                String currentForm = form.optionalForms().replace("_", "");
                log.info("current form: {}", currentForm);
                if (form.addToPolicy().equals("Yes")) {
                    combinedData.put(currentForm, currentForm);
                }
            }
        }
        int locationCount = 0;
        if (quote.packageLocations().size() > 0) {
            for (PackageLocation loc : quote.packageLocations()) {
                locationCount = locationCount + 1;

                locationData.put("addressLine1", loc.data().packageProductBasicInfo().locationAddress().line1());
                locationData.put("addressLine2", loc.data().packageProductBasicInfo().locationAddress().line2());
                locationData.put("city", loc.data().packageProductBasicInfo().locationAddress().city());
                locationData.put("state", loc.data().packageProductBasicInfo().locationAddress().state());
                locationData.put("zipCode", loc.data().packageProductBasicInfo().locationAddress().zipCode());
                locationData.put("causeOfLossForm", loc.data().packageLocationCoverageTerms().causeOfLossForm());
                locationData.put("coinsurancePercent", loc.data().packageLocationCoverageTerms().coinsurancePercent());
                locationData.put("applicantsExposures", Integer.toString(loc.data().applicantsExposures()));
                locationData.put("classCode", loc.data().classCode());

                locationData.put("perOccurence", loc.limitOfInsurancePerOccurrence().value().toString());
                locationData.put("aggregate", loc.limitOfInsuranceAggregate().value().toString());
                locationData.put("liabilityDeductible", loc.liabilityDeductible().value().toString());
                locationData.put("medicalLimit", loc.limitOfInsuranceMedical().value().toString());

                for (PropertyBuilding building : loc.propertyBuildings()) {
                    if (building.data().propertySafeguards().hasSprinklers().equalsIgnoreCase("Yes")) {
                        locationData.put("hasSprinklerDiscount", "Yes");
                    }
                    locationData.put("yearBuilt", Integer.toString(building.data().propertyConstructionUpdates().yearBuilt()));
                    locationData.put("squareFoot", Integer.toString(building.data().propertyBuildingInfo().area()));
                    locationData.put("buildingValue", "$" + Integer.toString(building.data().buildingCoverageTerms().buildingValue()));
                    locationData.put("contentValue", "$" + Integer.toString(building.data().buildingCoverageTerms().contentValue()));
                    locationData.put("mortgageeName", building.data().propertyBuildingInfo().mortgageeHolder().mortgageeName());
                    locationData.put("mortgageeHolderAddressLine1", building.data().propertyBuildingInfo().mortgageeHolder().mortgageeAddress().line1());
                    locationData.put("mortgageeHolderAddressCity", building.data().propertyBuildingInfo().mortgageeHolder().mortgageeAddress().city());
                    locationData.put("mortgageeHolderAddressState", building.data().propertyBuildingInfo().mortgageeHolder().mortgageeAddress().state());
                    locationData.put("mortgageeHolderAddresszipCode", building.data().propertyBuildingInfo().mortgageeHolder().mortgageeAddress().zipCode());

                    locationData.put("aopDeductible", building.data().buildingCoverageTerms().aopDeductible());
                    locationData.put("windHailDeductible", building.data().buildingCoverageTerms().windHailDeductible());

                    String protectDevices = "";
                    String hasSprinklers = building.data().propertySafeguards().hasSprinklers();
                    if (hasSprinklers.equals("Yes") && !building.data().propertySafeguards().fireSafeguards().contains("None") && !building.data().propertySafeguards().burglarSafeguards().contains("None")) {
                        protectDevices = "Sprinkler System, Fire-Alarm System, Burglary Protective Safeguards";
                    } else if ((hasSprinklers.equals("Yes") && !building.data().propertySafeguards().fireSafeguards().contains("None")) ||
                            (building.data().propertySafeguards().fireSafeguards().equals("Yes") && !building.data().propertySafeguards().burglarSafeguards().contains("None")) ||
                            (!building.data().propertySafeguards().fireSafeguards().contains("None") && !building.data().propertySafeguards().burglarSafeguards().contains("None"))
                    ) {
                        protectDevices = "2 of 3Systems";
                    } else if (hasSprinklers.equals("Yes")) {
                        protectDevices = "Sprinkler System";
                    } else if (!building.data().propertySafeguards().fireSafeguards().contains("None")) {
                        protectDevices = "Fire-Alarm System";
                    } else if (!building.data().propertySafeguards().burglarSafeguards().contains("None")) {
                        protectDevices = "Burglary Protective Safeguards";
                    }
                    locationData.put("protectDevices", protectDevices);
                }

                BigDecimal zero = new BigDecimal("0.0");
                String currentState = loc.data().packageProductBasicInfo().locationAddress().state();
                log.info("current state for document generation: {}", currentState);
                if (currentState.equals("TX") || currentState.equals("IL") || currentState.equals("OH")) {
                    combinedData.put("CP00101012", "CP_00_10_10_12");

                    // filter the below by business income and extra expense
                    if (loc.businessIncomeCoverage() != null && loc.extraExpenseCoverage() != null) {
                        combinedData.put("CP00301012", "CP_00_30_10_12");
                    } else if (loc.businessIncomeCoverage() != null && loc.extraExpenseCoverage() == null) {
                        combinedData.put("CP00321012", "CP_00_32_10_12");
                    }

                    combinedData.put("CP00900788", "CP_00_90_07_88");
                    combinedData.put("CP01400706", "CP_01_40_07_06");
                    combinedData.put("CP01420312", "CP_01_42_03_12");

                    if ((loc.liabilityDeductible().value()).compareTo(zero) > 0) {
                        combinedData.put("CG03000196", "CG_03_00_01_96");
                    }

                    // for ord law being attached
                    if (loc.data().ordLawTria().size() > 0 && loc.data().ordLawTria().contains("Ord Law")) {
                        if (loc.data().ordLawTria().contains("Ord Law")) {
                            combinedData.put("CP04050917", "CP_04_05_09_17");
                        }
                        if (loc.data().ordLawTria().contains("TRIA")) {
                            combinedData.put("CG21900106", "CG_21_90_01_06");
                        }
                    }
                    

                    combinedData.put("CG00010413", "CG_00_01_04_13");
                    combinedData.put("CG00691223", "CG_00_69_12_23");
                    combinedData.put("CG21061223", "CG_21_06_12_23");
                    String classCode = loc.data().classCode();
                    String[] classCodeCG_21_35 = { "16670", "40046", "41665", "41666", "43117", "44222", "45224",
                            "45225", "47221", "48177", "48178", "48252", "48924" };
                    String[] classCodeCG_21_38 = { "43200", "46822", "46881", "46882", "65007", "66122", "66123",
                            "91130", "91636", "98751" };
                    String[] classCodeCG_21_01 = { "40059", "40061", "40063", "40064", "40066", "40067", "40069",
                            "43421", "43422", "43424", "46911", "46912", "46915", "46916", "47318", "48441", "63217",
                            "63218", "63219", "63220" };

                    if (check(classCodeCG_21_35, classCode)) {
                        combinedData.put("CG21351001", "CG_21_35_10_01");
                    } else if (check(classCodeCG_21_38, classCode)) {
                        combinedData.put("CG21381185", "CG_21_38_11_85");
                    } else if (check(classCodeCG_21_01, classCode)) {
                        combinedData.put("CG21011219", "CG_21_01_12_19");
                    } else if (classCode.equalsIgnoreCase("47052")) {
                        combinedData.put("CG22700413", "CG_22_70_04_13");
                    }
                    combinedData.put("CG21450798", "CG_21_45_07_98");
                    // TODO: filter class code
                    // result.put("CG_21_52_04_13",DocumentSelectionAction.generate);

                    combinedData.put("CG21671204", "CG_21_67_12_04");
                    // TODO
                    // result.put("CG_21_90_01_06",DocumentSelectionAction.generate);
                    combinedData.put("CG21960305", "CG_21_96_03_05");
                    combinedData.put("CG40041219", "CG_40_04_12_19");
                    combinedData.put("CG40320523", "CG_40_32_05_23");

                    // TODO: ensure other forms are not attached prior, does not seem other forms
                    // are also attached for launch
                    combinedData.put("CG40141220", "CG_40_14_12_20");
                    combinedData.put("CG40351223", "CG_40_35_12_23");

                    combinedData.put("CGDS011001", "CG_DS_01_10_01");

                    combinedData.put("MGTGL10000125", "MGT_GL_1000_01_25");
                    combinedData.put("MGTGL10010125", "MGT_GL_1001_01_25");
                    combinedData.put("MGTGL10020125", "MGT_GL_1002_01_25");
                    combinedData.put("MGTGL10030125", "MGT_GL_1003_01_25");

                    // if MGT_GL_1002_01_25 is attached do not attach
                    // result.put("MGT_GL_1004_01_25",DocumentSelectionAction.generate);
                    combinedData.put("MGTGL10050125", "MGT_GL_1005_01_25");
                    combinedData.put("MGTGL10060125", "MGT_GL_1006_01_25");
                    combinedData.put("MGTGL10070125", "MGT_GL_1007_01_25");
                    combinedData.put("MGTGL10080125", "MGT_GL_1008_01_25");
                    combinedData.put("MGTGL10090125", "MGT_GL_1009_01_25");
                    combinedData.put("MGTGL10100125", "MGT_GL_1010_01_25");
                    combinedData.put("MGTGL10110125", "MGT_GL_1011_01_25");
                    combinedData.put("MGTGL10120125", "MGT_GL_1012_01_25");
                    combinedData.put("MGTGL10130125", "MGT_GL_1013_01_25");
                    combinedData.put("MGTGL10140125", "MGT_GL_1014_01_25");
                    combinedData.put("MGTGL10150125", "MGT_GL_1015_01_25");
                    combinedData.put("MGTGL10160125", "MGT_GL_1016_01_25");

                    combinedData.put("MGTIL10000125", "MGT_IL_1000_01_25");
                    combinedData.put("MGTIL10010125", "MGT_IL_1001_01_25");
                    combinedData.put("MGTIL10020125", "MGT_IL_1002_01_25");

                    combinedData.put("IL00171198", "IL_00_17_11_98");
                    combinedData.put("IL00210908", "IL_00_21_09_08");
                    combinedData.put("IL09851220", "IL_09_85_12_20");
                    combinedData.put("ILP0010104", "IL_P_001_01_04");
                    combinedData.put("CG40280922", "CG_40_28_09_22");
                    if (currentState.equals("OH")) {
                        combinedData.put("CG21490999", "CG_21_49_09_99");
                        combinedData.put("CG21471207", "CG_21_47_12_07");
                        combinedData.put("IL02440907", "IL_02_44_09_07");
                        combinedData.put("IL12011185", "IL_12_01_11_85");
                    } else if (currentState.equals("TX")) {
                        combinedData.put("CG01030606", "CG_01_03_06_06");
                        combinedData.put("CG02051204", "CG_02_05_12_04");
                        combinedData.put("CG21490999", "CG_21_49_09_99");
                        combinedData.put("IL01680312", "IL_01_68_03_12");
                        combinedData.put("CG21490999", "CG_21_49_09_99");
                        combinedData.put("IL02751113", "IL_02_75_11_13");
                    } else if (currentState.equals("IL")) {
                        // TODO: MGT needs to privde
                        // result.put("CG_02_00_01_18",DocumentSelectionAction.generate);
                        combinedData.put("CG21471207", "CG_21_47_12_07");
                        combinedData.put("IL01470911", "IL_01_47_09_11");
                        combinedData.put("IL02751113", "IL_02_75_11_13");
                        combinedData.put("IL01621013", "IL_01_62_10_13");
                    }

                    // for safety questions add
                    for (PropertyBuilding building : loc.propertyBuildings()) {
                        String hasSprinklers = building.data().propertySafeguards().hasSprinklers();
                        if (hasSprinklers.equalsIgnoreCase("Yes") ||
                                !building.data().propertySafeguards().burglarSafeguards().contains("None") ||
                                !building.data().propertySafeguards().fireSafeguards().contains("None")
                        ) {
                            combinedData.put("CP04110917", "CP_04_11_09_17");
                        }

                        String windHailDeductible = building.data().buildingCoverageTerms().windHailDeductible();
                        // if wind hail is not excluded, add the below
                        if (!windHailDeductible.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                            combinedData.put("MGTPR10040125", "MGT_PR_1004_01_25");
                        }
                        if (loc.data().hasCommercialCondo().equalsIgnoreCase("Yes")) {
                            combinedData.put("MGTPR10000125", "MGT_PR_1000_01_25");
                        }
                        // cause of loss form
                        String causeOfLossForm = loc.data().packageLocationCoverageTerms().causeOfLossForm();
                        if (causeOfLossForm.equalsIgnoreCase("Broad")) {
                            combinedData.put("CP10201012", "CP_10_20_10_12");
                        } else if (causeOfLossForm.equalsIgnoreCase("Special")) {
                            combinedData.put("CP_10_30_09_17", "CP10300917");
                        }

                        // NOTE: logic of marking paragraph in dynamic document
                        String roofCovering = building.data().propertyConstructionUpdates().roofCovering();
                        int roofAge = Integer.parseInt(building.data().roofAge());
                        if (roofAge >= 11 ||
                                (roofAge <= 10 && roofCovering.equalsIgnoreCase("Metal"))) {
                            combinedData.put("CP10361012", "CP_10_36_10_12");
                        }

                        combinedData.put("CP10751220", "CP_10_75_12_20");
                        combinedData.put("IL00171198", "IL_00_17_11_98");
                        combinedData.put("IL09520115", "IL_09_52_01_15");
                        combinedData.put("IL09851220", "IL_09_85_12_20");
                        combinedData.put("ILP0010104", "IL_P_001_01_04");


                        if (currentState.equalsIgnoreCase("OH")) {
                            // if (causeOfLossForm.equalsIgnoreCase("Special")) {
                            //     combinedData.put("CP10461012", "CP_10_46_10_12");
                            // }
                            if (windHailDeductible.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                                combinedData.put("CP10540607", "CP_10_54_06_07");
                            }
                            combinedData.put("IL09350702", "IL_09_35_07_02");
                            combinedData.put("CP01230408", "CP_01_23_04_08");
                            combinedData.put("IL02440907", "IL_02_44_09_07");
                        } else if (currentState.equalsIgnoreCase("TX")) {
                            combinedData.put("CP01420312", "CP_01_42_03_12");

                            // NOTE: not in package
                            combinedData.put("CP02020824", "CP_02_02_08_24");
                            // if (causeOfLossForm.equalsIgnoreCase("Special")) {
                            //     combinedData.put("CP10461012", "CP_10_46_10_12");
                            // }
                            combinedData.put("IL02751113", "IL_02_75_11_13");
                            combinedData.put("IL02751113", "IL_02_75_11_13");
                            // TODO: document needs to be provided
                            // combinedData.put("IL12041298", "IL_12_04_12_98");
                            combinedData.put("ILN1780313", "IL_N_178_03_13");
                        } else if (currentState.equalsIgnoreCase("IL")) {
                            combinedData.put("CP01490607", "CP_01_49_06_07");
                            // wind hail is not excluded, add the below
                            if (windHailDeductible.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                                combinedData.put("CP10540607", "CP_10_54_06_07");
                            }
                            combinedData.put("IL01180217", "IL_01_18_02_17");
                            combinedData.put("IL02840118", "IL_02_84_01_18");
                            combinedData.put("IL09350702", "IL_09_35_07_02");

                        }
                    }
                }
            }
        }

        combinedData.put("location", locationData);
        log.info("combined data: {}", combinedData);

        return DocumentDataSnapshot.builder()
                .renderingData(combinedData)
                .build();
    }

    private static Map<String, Object> getPricingData(QuotePricing pricing) {
        Map<String, Object> pricingData = new HashMap<>();
        double premium = 0;
        double taxes = 0;
        double stampingFee = 0;
        double carrierPolicyFee = 0;
        double installmentFee = 0;
        double brokerFee = 0;
        double terrosimCertificate = 0;

        log.info("Pricing Data: {}", pricing);

        for (var item : pricing.items()) {

            if (item.chargeType().equalsIgnoreCase("premium")) {
                premium += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("tax")) {
                taxes += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("stampingFee")) {
                stampingFee += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("carrierPolicyFee")) {
                carrierPolicyFee += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("installmentFee")) {
                installmentFee += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("brokerFee")) {
                brokerFee += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("terrorism")) {
                terrosimCertificate += item.rate().doubleValue();
            }
        }
        
        if (premium > 0.0) {
            pricingData.put("premium", "$" + Double.toString(premium));
        }
        if (taxes > 0.0) {
            pricingData.put("tax", "$" + Double.toString(taxes));
        }
        double premiumTaxes = premium + taxes;
        if (premiumTaxes > 0.0) {
            pricingData.put("premiumAndTaxes", "$" + Double.toString(premiumTaxes));
        }
        double premiumAndTaxesAndTerrorism = premium + taxes + terrosimCertificate;
        if (premiumAndTaxesAndTerrorism > 0.0) {
            pricingData.put("premiumAndTaxesAndTerrorism", "$" + Double.toString(premiumAndTaxesAndTerrorism));
        }
        if (stampingFee > 0.0) {
            pricingData.put("stampingFee", "$" + Double.toString(stampingFee));
        }
        if (carrierPolicyFee > 0.0) {
            pricingData.put("carrierPolicyFee", "$" + Double.toString(carrierPolicyFee));
        }
        if (installmentFee > 0.0) {
            pricingData.put("installmentFee", "$" + Double.toString(installmentFee));
        }
        if (brokerFee > 0.0) {
            pricingData.put("brokerFee", "$" + Double.toString(brokerFee));
        }
        if (terrosimCertificate > 0.0) {
            pricingData.put("terrosimCertificate", "$" + Double.toString(terrosimCertificate));
        }
        double premiumAndTaxesAndFees = brokerFee + stampingFee + carrierPolicyFee + installmentFee + premium + taxes + terrosimCertificate; 
        if (premiumAndTaxesAndFees > 0.0) {
            pricingData.put("premiumAndTaxesAndFees", "$" + Double.toString(premiumAndTaxesAndFees));
        }
        return pricingData;
    }

    private static boolean check(String[] arr, String toCheckValue) {
        // check if the specified element
        // is present in the array or not
        // using Linear Search method
        boolean test = false;
        for (String element : arr) {
            if (element.equalsIgnoreCase(toCheckValue)) {
                test = true;
                break;
            }
        }
        return test;
    }
}