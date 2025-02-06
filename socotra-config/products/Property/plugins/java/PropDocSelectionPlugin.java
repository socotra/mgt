package com.socotra.deployment.customer.property;
import com.socotra.coremodel.DocumentConfig;
import com.socotra.coremodel.DocumentTrigger;
import com.socotra.coremodel.TransactionCategory;
import com.socotra.deployment.customer.*;
import java.math.BigDecimal;
import com.socotra.coremodel.DocumentSelectionAction;
import com.socotra.deployment.customer.DocumentSelectionPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class PropDocSelectionPlugin implements DocumentSelectionPlugin {

    private static final Logger log = LoggerFactory.getLogger(PropDocSelectionPlugin.class);
    @Override
    public Map<String, DocumentSelectionAction> selectDocuments(PropertyQuoteRequest propertyQuoteRequest) {
        Map<String, DocumentSelectionAction> result =  new HashMap<>();
        result.put("CP_00_10_10_12", DocumentSelectionAction.generate);
        PropertyQuote quote = propertyQuoteRequest.quote();
        // NOTE: going live with only 1 location, may need a check in the future to ensure same documents are not duplicated
        if (quote.data().propertyOptionalForms() != null && quote.data().propertyOptionalForms().size() > 0) {
            for (PropertyOptionalForms form : quote.data().propertyOptionalForms()) {
                String currentForm = form.optionalForms();
                if (form.addToPolicy().equalsIgnoreCase("Yes")) {
                    result.put(currentForm, DocumentSelectionAction.generate);
                }
            }
        }
        if (quote.propertyLocations().size() > 0) {
            for (PropertyLocation loc : quote.propertyLocations()) {
                BigDecimal zero = new BigDecimal("0.0");
                // property forms
                String currentState = loc.data().packageProductBasicInfo().locationAddress().state();
                log.info("current state for document generation: {}", currentState);
                if (currentState.equalsIgnoreCase("TX") || currentState.equalsIgnoreCase("IL") || currentState.equalsIgnoreCase("OH")) {
                    result.put("CP_00_10_10_12", DocumentSelectionAction.generate);
                    
                    // filter the below by business income and extra expense
                    if (loc.businessIncomeCoverage() != null && loc.extraExpenseCoverage() != null) {
                        result.put("CP_00_30_10_12", DocumentSelectionAction.generate);
                    } else if (loc.businessIncomeCoverage() != null && loc.extraExpenseCoverage() == null) {
                        result.put("CP_00_32_10_12", DocumentSelectionAction.generate);
                    }

                    result.put("CP_00_90_07_88", DocumentSelectionAction.generate);
                    result.put("CP_01_40_07_06", DocumentSelectionAction.generate);
                    result.put("CP_01_42_03_12", DocumentSelectionAction.generate);

                    // for ord law being attached
                    if (loc.data().ordLawTria().size() > 0 && loc.data().ordLawTria().contains("Ord Law")) {
                        result.put("CP_04_05_09_17", DocumentSelectionAction.generate);
                    }
                    // for safety questions add
                    for (PropertyBuilding building : loc.propertyBuildings()) {
                        String hasSprinklers = building.data().propertySafeguards().hasSprinklers();
                        if (hasSprinklers.equalsIgnoreCase("Yes") ||
                                !building.data().propertySafeguards().burglarSafeguards().contains("None") ||
                                !building.data().propertySafeguards().fireSafeguards().contains("None")
                        ) {
                            result.put("CP_04_11_09_17", DocumentSelectionAction.generate);
                        }

                        String windHailDeductible = building.data().buildingCoverageTerms().windHailDeductible();
                        // if wind hail is not excluded, add the below
                        if (!windHailDeductible.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                            result.put("MGT_PR_1004_01_25", DocumentSelectionAction.generate);
                        }
                        if (loc.data().hasCommercialCondo().equalsIgnoreCase("Yes")) {
                            result.put("MGT_PR_1000_01_25", DocumentSelectionAction.generate);
                        }
                        // cause of loss form
                        String causeOfLossForm = loc.data().packageLocationCoverageTerms().causeOfLossForm();
                        if (causeOfLossForm.equalsIgnoreCase("Broad")) {
                            result.put("CP_10_20_10_12", DocumentSelectionAction.generate);
                        } else if (causeOfLossForm.equalsIgnoreCase("Special")) {
                            result.put("CP_10_30_09_17", DocumentSelectionAction.generate);
                        }

                        // NOTE: logic of marking paragraph in dynamic document
                        String roofCovering = building.data().propertyConstructionUpdates().roofCovering();
                        int roofAge = Integer.parseInt(building.data().roofAge());
                        if (roofAge >= 11 ||
                            (roofAge <= 10 && roofCovering.equalsIgnoreCase("Metal"))) {
                            result.put("CP_10_36_10_12", DocumentSelectionAction.generate);
                        }
                        
                        result.put("CP_10_75_12_20", DocumentSelectionAction.generate);
                        result.put("IL_00_17_11_98", DocumentSelectionAction.generate);
                        result.put("IL_09_52_01_15", DocumentSelectionAction.generate);
                        result.put("IL_09_85_12_20", DocumentSelectionAction.generate);
                        result.put("IL_P_001_01_04", DocumentSelectionAction.generate);

                        if (currentState.equalsIgnoreCase("OH")) {
                            if (causeOfLossForm.equalsIgnoreCase("Special")) {
                                result.put("CP_10_46_10_12", DocumentSelectionAction.generate);
                            }
                            if (windHailDeductible.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                                result.put("CP_10_54_06_07", DocumentSelectionAction.generate);
                            }
                            result.put("IL_09_35_07_02", DocumentSelectionAction.generate);
                            result.put("CP_01_23_04_08", DocumentSelectionAction.generate);
                            result.put("IL_02_44_09_07", DocumentSelectionAction.generate);
                        } else if (currentState.equalsIgnoreCase("TX")) {
                            result.put("CP_01_42_03_12", DocumentSelectionAction.generate);

                            // NOTE: not in package
                            result.put("CP_02_02_08_24", DocumentSelectionAction.generate);
                            if (causeOfLossForm.equalsIgnoreCase("Special")) {
                                result.put("CP_10_46_10_12", DocumentSelectionAction.generate);
                            }
                            result.put("IL_02_75_11_13", DocumentSelectionAction.generate);
                            result.put("IL_12_04_12_98", DocumentSelectionAction.generate);
                            result.put("IL_N_178_03_13", DocumentSelectionAction.generate);
                        } else if (currentState.equalsIgnoreCase("IL")) {
                            result.put("CP_01_49_06_07", DocumentSelectionAction.generate);
                            // wind hail is not excluded, add the below
                            if (windHailDeductible.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                                result.put("CP_10_54_06_07", DocumentSelectionAction.generate);
                            }
                            result.put("IL_01_18_02_17", DocumentSelectionAction.generate);
                            result.put("IL_02_84_01_18", DocumentSelectionAction.generate);
                            result.put("IL_09_35_07_02", DocumentSelectionAction.generate);

                        }
                    }
                }

            }
        }
        log.info("request = {}, result = {}", propertyQuoteRequest, result);
        return result;
    }

    private static boolean check(int[] arr, int toCheckValue)
    {
        // check if the specified element
        // is present in the array or not
        // using Linear Search method
        boolean test = false;
        for (int element : arr) {
            if (element == toCheckValue) {
                test = true;
                break;
            }
        }
        return test;
    }
}