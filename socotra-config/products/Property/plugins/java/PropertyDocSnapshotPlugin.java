package com.socotra.deployment.customer.property;

import com.socotra.coremodel.DocumentDataSnapshot;
import com.socotra.coremodel.QuotePricing;
import com.socotra.deployment.DataFetcherFactory;
import com.socotra.deployment.customer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.text.*;
import java.math.BigDecimal;

public class PropertyDocSnapshotPlugin implements DocumentDataSnapshotPlugin {

    private static final Logger log = LoggerFactory.getLogger(DocumentDataSnapshotPlugin.class);

    @Override
    public DocumentDataSnapshot dataSnapshot(PropertyQuoteRequest propertyQuoteRequest) {
        var quote = propertyQuoteRequest.quote();
        var pricing = DataFetcherFactory.get().getQuotePricing(quote.locator());
        // NOTE: NOT WORKING
        Map<String, Object> pricingData = getPricingData(pricing);
        Map<String, Object> locationData = new HashMap<>();
        Map<String, Object> combinedData = new HashMap<>();
        Applicant account = DataFetcherFactory.get().getAccount(quote.accountLocator());


        log.info("quote: {}", quote);

        String affinityDiscount = quote.data().affinityDiscount();
        log.info("quote: {}", quote);
        combinedData.put("hasAffinityDiscount", "□");
        if (!affinityDiscount.equalsIgnoreCase("None")) {
            combinedData.put("hasAffinityDiscount", "X");
        }
        combinedData.put("isProperty", "Yes");

        combinedData.put("applicantName", account.data().applicantName());
        combinedData.put("applicantPhoneNumber", account.data().applicantPhoneNumber());
        combinedData.put("line1", account.data().applicantMailingAddress().line1());
        combinedData.put("city", account.data().applicantMailingAddress().city());
        combinedData.put("state", account.data().applicantMailingAddress().state());
        combinedData.put("zipCode", account.data().applicantMailingAddress().zipCode());
        combinedData.put("applicantLegalEntity", account.data().applicantLegalEntity());

        combinedData.put("occupancyType", quote.data().classOfBusiness().occupancyType());

//        // Policy Data Mapping
        String productName = "Commercial Property";

        // get today
        String pattern = "MM/dd/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.
        String todayAsString = df.format(today);

        // TODO: get effective to and from dates in readable format
//        Policy Effective From: 2025-01-31T18:26:18Z
        //
        combinedData.put("productName", productName);
        combinedData.put("quote", quote);
        combinedData.put("currentDay", todayAsString);
        combinedData.put("pricing", pricingData);

        // form data
        if (quote.data().propertyOptionalForms() != null && quote.data().propertyOptionalForms().size() > 0) {
            for (PropertyOptionalForms form : quote.data().propertyOptionalForms()) {
                String currentForm = form.optionalForms().replace("_", "");
                log.info("current form: {}", currentForm);
                if (form.addToPolicy().equals("Yes")) {
                    combinedData.put(currentForm, currentForm);
                }
            }
        }

        if (quote.propertyLocations().size() > 0) {
            for (PropertyLocation loc : quote.propertyLocations()) {

                locationData.put("addressLine1", loc.data().packageProductBasicInfo().locationAddress().line1());
                locationData.put("addressLine2", loc.data().packageProductBasicInfo().locationAddress().line2());
                locationData.put("city", loc.data().packageProductBasicInfo().locationAddress().city());
                locationData.put("state", loc.data().packageProductBasicInfo().locationAddress().state());
                locationData.put("zipCode", loc.data().packageProductBasicInfo().locationAddress().zipCode());
                locationData.put("causeOfLossForm", loc.data().packageLocationCoverageTerms().causeOfLossForm());
                locationData.put("coinsurancePercent", loc.data().packageLocationCoverageTerms().coinsurancePercent());

                for (PropertyBuilding building : loc.propertyBuildings()) {
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
                // property forms
                String currentState = loc.data().packageProductBasicInfo().locationAddress().state();
                log.info("current state for document generation: {}", currentState);
                if (currentState.equalsIgnoreCase("TX") || currentState.equalsIgnoreCase("IL") || currentState.equalsIgnoreCase("OH")) {
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

                    // for ord law being attached
                    if (loc.data().ordLawTria().size() > 0 && loc.data().ordLawTria().contains("Ord Law")) {
                        combinedData.put("CP04050917", "CP_04_05_09_17");
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
                            if (causeOfLossForm.equalsIgnoreCase("Special")) {
                                combinedData.put("CP10461012", "CP_10_46_10_12");
                            }
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
                            if (causeOfLossForm.equalsIgnoreCase("Special")) {
                                combinedData.put("CP10461012", "CP_10_46_10_12");
                            }
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

        pricingData.put("premium", "$" + Double.toString(premium));
        pricingData.put("tax", "$" + Double.toString(taxes));
        pricingData.put("premiumAndTaxes", "$" + Double.toString(premium + taxes));
        pricingData.put("premiumAndTaxesAndTerrorism", "$" + Double.toString(premium + taxes + terrosimCertificate));
        pricingData.put("stampingFee", "$" + Double.toString(stampingFee));
        pricingData.put("carrierPolicyFee", "$" + Double.toString(carrierPolicyFee));
        pricingData.put("installmentFee", "$" + Double.toString(installmentFee));
        pricingData.put("brokerFee", "$" + Double.toString(brokerFee));
        pricingData.put("terrosimCertificate", "$" + Double.toString(terrosimCertificate));
        pricingData.put("premiumAndTaxesAndFees", "$" + Double.toString(brokerFee + stampingFee + carrierPolicyFee + installmentFee + premium + taxes + terrosimCertificate));
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