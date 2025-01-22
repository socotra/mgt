package com.socotra.deployment.customer.property;

import com.socotra.coremodel.*;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;
import com.socotra.deployment.customer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.util.*;
import java.time.Year;

public class PropUWPlugin implements UnderwritingPlugin {
    private static final Logger log = LoggerFactory.getLogger(PropUWPlugin.class);

    @Override
    public UnderwritingModification underwrite(PropertyQuoteRequest propertyQuoteRequest) {

        PropertyQuote quote = propertyQuoteRequest.quote();

        // Create Empty List of flags to create
        List<UnderwritingFlagCore> flagsToCreate = new ArrayList<>();

//        for (int i = 0; i < 100; i++) {
//            Optional<UWRules> uwRulesRecord = tableRecordFetcher.getTable(UWRules.class)
//                    .getRecord(UWRules.makeKey("uwRule", Integer.toString(i)));
//
//            log.info("Processing rule ID: {}", i);
//
//            // If no rule is found, exit the loop
//            if (uwRulesRecord.isEmpty()) {
//                log.info("No more underwriting rules found, exiting.");
//                break;
//            }
//
//            // Process the found underwriting rule
//            UWRules uwRule = uwRulesRecord.get();
//            boolean uwResult = processUWRule(quote, uwRule);
//
//            // If the underwriting result fails and the message is not already there, reject the risk and return modification
//            if (uwResult) {
//                flagsToCreate.add(createUnderwritingFlag(uwRule.decision(), uwRule.message()));
//            }
//            log.info("Underwriting result for rule ID {}: {}", i, uwResult);
//        }

        // if (quote.data().propertyQuestions().hasCommercialCooking().equalsIgnoreCase("Yes")) {
        //     flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        // } else if (!(quote.data().propertyQuestions().buildingWiringPlumbing().contains("N/A"))) {
        //     flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Building, wiring, plumbing block"));
        // } else if (!(quote.data().propertyQuestions().ineligibleOccupants().contains("N/A"))) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Ineligible occupants"));
        // } else if (!(quote.data().propertyQuestions().buildingHazards().contains("N/A"))) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Ineligible occupants"));
        // } 
        // else if (quote.propertyLocations().size() > 5) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. More than 5 locations."));
        // } else if (quote.data().packageQuestions().ownsAllProperties().equalsIgnoreCase("No")) {
        //     flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Applicant does not own all properties."));
        // } else if (quote.data().packageQuestions().hasHistoricalCoverageGap().equalsIgnoreCase("Non-renewed citing underwriting reasons")) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Non-renewed citing underwriting reasons."));
        // } else if(!(quote.data().packageQuestions().hasHistoricalCoverageGap().equalsIgnoreCase("No")) ||
        //         !(quote.data().packageQuestions().hasHistoricalCoverageGap().equalsIgnoreCase("Non-renewed, carrier product no longer supported"))) {
        //     flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Historical gap reason."));
        // } else if (quote.data().packageQuestions().hasFinancialHistoryIssue().equalsIgnoreCase("Yes")) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Financial history issue."));
        // } else if (quote.data().packageQuestions().hasLegalLiabilityIssue().equalsIgnoreCase("Yes")) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Legal Liability issue."));
        // } else if (quote.data().packageQuestions().compliesNfPA().equalsIgnoreCase("No")) {
        //     flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with NFPA."));
        // } else if (quote.data().packageQuestions().priorCodeViolation().equalsIgnoreCase("Yes")) {
        //     flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Prior Code Violation."));
        // } else if (quote.data().packageQuestions().claimHistory().size() > 0) {
        //     for (ClaimHistory claim : quote.data().packageQuestions().claimHistory()) {
        //         // only applicable to GL
        //         if(claim.claimStatus().equalsIgnoreCase("Open")) {
        //             flagsToCreate.add(UnderwritingFlagCore.builder()
        //                     .level(UnderwritingLevel.block)
        //                     .note("Refer to underwriter. Claim Open.")
        //                     .build());
        //         }
        //     }
        // } else {
        //     for (PropertyLocation loc : quote.propertyLocations()) {
        //         if ((Year.now().getValue() - loc.data().yearBuilt()) > 40) {
        //             flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. House is older than 40 years."));
        //         } else if (loc.data().ppcFireClass().equalsIgnoreCase("9")) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. PPC Fire Class is PC 9."));
        //         } else if (loc.data().stories() > 4) {
        //             flagsToCreate.add(createUnderwritingFlag("block", "Blocked. Building is taller than 4 stories."));
        //         } else if (loc.data().area() > 25000) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Building is greater than 25k sq. ft.."));
        //         } else if (loc.data().roofCovering().equalsIgnoreCase("Wood")) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Roof is made of wood."));
        //         } else if (((Year.now().getValue() - loc.data().parkingLastUpdated()) > 30) &&
        //                 loc.data().hasElectricVehicleCharging().equalsIgnoreCase("Yes")
        //         ) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Has electrical vehicle charging and parking lot was updated more than 30 years ago."));
        //         } else if (loc.data().isHistorical().equalsIgnoreCase("Yes")) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Historical building."));
        //         } else if (!loc.data().undergroundHazards().equalsIgnoreCase("N/A")) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Underground hazards."));
        //         } else if (loc.data().permitsSpaceHeaters().equalsIgnoreCase("Yes")) {
        //             flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Permits space heaters."));
        //         } else if (loc.data().vacantSquareFootage().length()  > 0) {
        //             flagsToCreate.add(createUnderwritingFlag("block", "Blocked. Vacancy Square Footage present."));
        //         }
        //     }
        // }

        /* ensure return size of plugin is not exceeded */
        List<UnderwritingFlagCore> singleRetFlag = new ArrayList<>();
        if (flagsToCreate.size() > 0) {
            singleRetFlag.add(flagsToCreate.get(0));
        }
        return UnderwritingModification.builder()
                .flagsToCreate(singleRetFlag)
                .build();
    }

    /**
     * Creates an underwriting modification based on the level and note.
     */
    private UnderwritingFlagCore createUnderwritingFlag(String decision, String note) {

        UnderwritingLevel uwLevel = switch (decision) {
            case "reject" -> UnderwritingLevel.reject;
            case "info" -> UnderwritingLevel.info;
            case "decline" -> UnderwritingLevel.decline;
            case "block" -> UnderwritingLevel.block;
            case "approve" -> UnderwritingLevel.approve;
            default -> throw new IllegalArgumentException("Unsupported decision: " + decision);
        };

        return UnderwritingFlagCore.builder()
                .level(uwLevel)
                .note(note)
                .build();
    }

    /**
     * Processes an individual underwriting rule and evaluates it against the quote data.
     */
    private boolean processUWRule(PropertyQuote quote, UWRules uwRule) {
        String uwRuleField = uwRule.field();
        log.info("Field Value being evaulated: {}", uwRuleField);
        String uwRuleCondition = uwRule.condition();
        String uwRuleValue = uwRule.value();

        log.info("Evaluating the following rule: {} {} {}", uwRuleField, uwRuleCondition, uwRuleValue);

        // Retrieve the field value from the quote based on the UW rule key
//        String fieldValue = getFieldValueFromQuote(quote.element(), uwRuleKey);

        // Determine if the field and rule values are integers or strings, and evaluate the rule
        return evaluateString(uwRuleField, uwRuleCondition, uwRuleValue);
    }

    /**
     * Evaluates string-based underwriting rules.
     */
    private static boolean evaluateString(String fieldValue, String condition, String ruleValue) {
        return switch (condition.toLowerCase()) {
            case "=" -> fieldValue.equalsIgnoreCase(ruleValue);
            case "!=" -> !fieldValue.equalsIgnoreCase(ruleValue);
//            case "in" -> isIn(fieldValue, ruleValue);
//            case "not in" -> !isIn(fieldValue, ruleValue);
            default -> throw new IllegalArgumentException("Unsupported condition: " + condition);
        };
    }
}