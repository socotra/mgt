package com.socotra.deployment.customer.packageproduct;
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
public class PackageDocSelectionPlugin implements DocumentSelectionPlugin {

    private static final Logger log = LoggerFactory.getLogger(PackageDocSelectionPlugin.class);
    @Override
    public Map<String, DocumentSelectionAction> selectDocuments(PackageProductQuoteRequest packageProductQuoteRequest) {
        Map<String, DocumentSelectionAction> result =  new HashMap<>();

        PackageProductQuote quote = packageProductQuoteRequest.quote();
        // NOTE: going live with only 1 location, may need a check in the future to ensure same documents are not duplicated

        result.put("Package_Dec_Page_1",DocumentSelectionAction.generate);
        if (quote.data().packageOptionalForms() != null && quote.data().packageOptionalForms().size() > 0) {
            for (PackageOptionalForms form : quote.data().packageOptionalForms()) {
                String currentForm = form.optionalForms();
                if (form.addToPolicy().equals("Yes")) {
                    result.put(currentForm, DocumentSelectionAction.generate);
                }
            }
        }
        if (quote.packageLocations().size() > 0) {
            for (PackageLocation loc : quote.packageLocations()) {
                BigDecimal zero = new BigDecimal("0.0");
                // general liability forms
                String currentState = loc.data().packageProductBasicInfo().locationAddress().state();
                log.info("current state for document generation: {}", currentState);
                if (currentState.equals("TX") || currentState.equals("IL") || currentState.equals("OH")) {
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

                    // only applies to package
                    for (PropertyBuilding building : loc.propertyBuildings()) {
                        if (building.data().propertyBuildingInfo().hasMortgage().equals("Yes")) {
                            result.put("CG_20_18_12_19", DocumentSelectionAction.generate);
                        }

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
                            // if (causeOfLossForm.equalsIgnoreCase("Special")) {
                            //     result.put("CP_10_46_10_12", DocumentSelectionAction.generate);
                            // }
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
                            // if (causeOfLossForm.equalsIgnoreCase("Special")) {
                            //     result.put("CP_10_46_10_12", DocumentSelectionAction.generate);
                            // }
                            result.put("IL_02_75_11_13", DocumentSelectionAction.generate);
                            // TODO: document needs to be provided
                            // result.put("IL_12_04_12_98", DocumentSelectionAction.generate);
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
                    result.put("CG_00_01_04_13",DocumentSelectionAction.generate);
                    result.put("CG_00_69_12_23",DocumentSelectionAction.generate);
                    result.put("CG_21_06_12_23", DocumentSelectionAction.generate);
                    // NOTE cannot have the above with the document below...
                    // result.put("CG_21_09_06_15",DocumentSelectionAction.generate);
                    // filter by class-code
                    String classCode = loc.data().classCode();
                    String[] classCodeCG_21_35 = { "16670" , "40046", "41665", "41666", "43117", "44222", "45224", "45225", "47221", "48177", "48178", "48252", "48924" };
                    String[] classCodeCG_21_38 = { "43200", "46822", "46881", "46882", "65007", "66122", "66123", "91130", "91636", "98751" };
                    String[] classCodeCG_21_01 = {"40059", "40061", "40063", "40064", "40066", "40067", "40069", "43421", "43422", "43424", "46911", "46912", "46915", "46916", "47318", "48441", "63217", "63218", "63219", "63220"};

                    if (check(classCodeCG_21_35, classCode)) {
                        result.put("CG_21_35_10_01",DocumentSelectionAction.generate);
                    } else if (check(classCodeCG_21_38, classCode)) {
                        result.put("CG_21_38_11_85",DocumentSelectionAction.generate);
                    } else if (check(classCodeCG_21_01, classCode)) {
                        // not yet provided
                        result.put("CG_21_01_12_19",DocumentSelectionAction.generate);
                    } else if (classCode.equalsIgnoreCase("47052")) {
                        result.put("CG_22_70_04_13",DocumentSelectionAction.generate);
                    }
                    result.put("CG_21_45_07_98",DocumentSelectionAction.generate);
                    // filter class code result.put("CG_21_52_04_13",DocumentSelectionAction.generate);

                    result.put("CG_21_67_12_04", DocumentSelectionAction.generate);
                    // TODO: document is not provided
                    // result.put("CG_21_90_01_06",DocumentSelectionAction.generate);
                    result.put("CG_21_96_03_05",DocumentSelectionAction.generate);
                    result.put("CG_40_04_12_19", DocumentSelectionAction.generate);
                    result.put("CG_40_32_05_23", DocumentSelectionAction.generate);
                    
                    // TODO: ensure other forms are not attached prior, does not seem other forms are also attached for launch
                    result.put("CG_40_14_12_20",DocumentSelectionAction.generate);
                    result.put("CG_40_35_12_23",DocumentSelectionAction.generate);

                    result.put("CG_DS_01_10_01",DocumentSelectionAction.generate);

                    result.put("MGT_GL_1000_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1001_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1002_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1003_01_25", DocumentSelectionAction.generate);
                    
                    // if MGT_GL_1002_01_25 is attached do not attach
                    // result.put("MGT_GL_1004_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1005_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1006_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1007_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1008_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1009_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1010_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1011_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1012_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1013_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1014_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1015_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_GL_1016_01_25",DocumentSelectionAction.generate);

                    result.put("MGT_IL_1000_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_IL_1001_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_IL_1002_01_25",DocumentSelectionAction.generate);
                    result.put("MGT_PR_1003_01_25",DocumentSelectionAction.generate);

                    result.put("IL_00_17_11_98",DocumentSelectionAction.generate);
                    result.put("IL_00_21_09_08",DocumentSelectionAction.generate);
                    result.put("IL_09_85_12_20",DocumentSelectionAction.generate);
                    result.put("IL_P_001_01_04",DocumentSelectionAction.generate);
                    result.put("CG_40_28_09_22",DocumentSelectionAction.generate);
                    if (currentState.equals("OH")) {
                        result.put("CG_21_49_09_99",DocumentSelectionAction.generate);
                        result.put("CG_21_47_12_07",DocumentSelectionAction.generate);
                        result.put("IL_02_44_09_07",DocumentSelectionAction.generate);
                        result.put("IL_12_01_11_85",DocumentSelectionAction.generate);
                    } else if (currentState.equals("TX")) {
                        result.put("CG_01_03_06_06",DocumentSelectionAction.generate);
                        result.put("CG_02_05_12_04",DocumentSelectionAction.generate);
                        result.put("CG_21_49_09_99",DocumentSelectionAction.generate);
                        result.put("IL_01_68_03_12",DocumentSelectionAction.generate);
                        result.put("IL_02_75_11_13",DocumentSelectionAction.generate);
                    } else if (currentState.equals("IL")) {
                        // result.put("CG_02_00_01_18",DocumentSelectionAction.generate);
                        result.put("CG_21_47_12_07",DocumentSelectionAction.generate);
                        result.put("IL_01_47_09_11",DocumentSelectionAction.generate);
                        result.put("IL_01_62_10_13",DocumentSelectionAction.generate);
                    }

                }

            }
        }
        log.info("request = {}, result = {}", packageProductQuoteRequest, result);
        return result;
    }

    private static boolean check(String[] arr, String toCheckValue)
    {
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