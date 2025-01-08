package com.socotra.deployment.customer.generalliability;

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

public class GLUnderwritingPlugin implements UnderwritingPlugin {
    private static final Logger log = LoggerFactory.getLogger(GLUnderwritingPlugin.class);
    @Override
    public UnderwritingModification underwrite(GeneralLiabilityQuoteRequest generalLiabilityQuoteRequest) {

        GeneralLiabilityQuote quote = generalLiabilityQuoteRequest.quote();
        log.info("Processing rule ID: {}", "hello");
//        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);

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

        if (quote.data().glQuestions().hasMulticlassRisk().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().glQuestions().hasMulticlassRiskDesc().length() > 0) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().glQuestions().leasesHoldHarmless().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Does not hold harmless wording"));
        } else if (quote.data().glQuestions().compliesADA().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with ADA."));
        } else if (quote.data().glQuestions().locationHazardsTenants().length() > 0) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Location Hazards."));
        } else if (quote.data().glQuestions().locationHazardsOperations().length() > 0) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Location has hazardous operations."));
        } else if (quote.locations().size() > 5) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. More than 5 locations."));
        } else if (quote.data().packageQuestions().ownsAllProperties().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Applicant does not own all properties."));
        } else if (quote.data().packageQuestions().hasHistoricalCoverageGap().equalsIgnoreCase("Non-renewed citing underwriting reasons")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Non-renewed citing underwriting reasons."));
        } else if(!(quote.data().packageQuestions().hasHistoricalCoverageGap().equalsIgnoreCase("No")) ||
                !(quote.data().packageQuestions().hasHistoricalCoverageGap().equalsIgnoreCase("Non-renewed, carrier product no longer supported"))) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Historical gap reason."));
        } else if (quote.data().packageQuestions().hasFinancialHistoryIssue().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Financial history issue."));
        } else if (quote.data().packageQuestions().hasLegalLiabilityIssue().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Legal Liability issue."));
        } else if (quote.data().packageQuestions().compliesNfPA().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with NFPA."));
        } else if (quote.data().packageQuestions().priorCodeViolation().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Prior Code Violation."));
        } else if (quote.data().packageQuestions().claimHistory().size() > 0) {
            for (ClaimHistory claim : quote.data().packageQuestions().claimHistory()) {
                // only applicable to GL
                if(claim.claimStatus().equalsIgnoreCase("Open")) {
                    flagsToCreate.add(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.block)
                            .note("Refer to underwriter. Claim Open.")
                            .build());
                }
            }
        } else {
            for (Location loc : quote.locations()) {
                if (loc.data().hasOccupancyPartialTenantOwnership().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Partial Tenant Ownership."));
                } else if (loc.data().isNamedAdditionalInsuredOnParking().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Parking not insured."));
                } else if (loc.data().isPoolFenced().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Pool unfenced."));
                } else if (loc.data().hasDivingBoardOrSlide().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Has divingboard/slide."));
                } else if (loc.data().hasDepthMarkers().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No depth markers."));
                } else if (loc.data().hasShepherdsCrookRing().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No shephers crook ring."));
                } else if (loc.data().hasSelfClosingGate().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No self closing gate."));
                } else if (loc.data().hasStructuresNearPool().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Structures near pool."));
                } else if (loc.data().hasWarningSigns().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No warning signs."));
                } else if (loc.data().hasSlidingGlassLocks().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No sliding glass locks."));
                } else if (loc.data().hasPeepholesOrDeadbolts().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No peepholdes or deadbolts."));
                } else if (loc.data().doesBackgroundChecksEmployee().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No employee background-check."));
                } else if (loc.data().doesBackgroundChecksTenant().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No tenant background-check."));
                } else if (loc.data().disclaimsSecurity().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No security disclaimers."));
                } else if (loc.data().vacancyProcedures().length() == 0) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No vacancy procedures."));
                } else if (loc.data().permitsBalconyCooking().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Permits Balcony Cooking."));
                } else if (loc.data().securityArmed().equalsIgnoreCase("Armed")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Armed security."));
                }
            }
        }

        /* ensure return size of plugin is not exceeded */
        List<UnderwritingFlagCore> singleRetFlag = new ArrayList<>();
        singleRetFlag.add(flagsToCreate.get(0));
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
    private boolean processUWRule(GeneralLiabilityQuote quote, UWRules uwRule) {
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