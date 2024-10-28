package com.socotra.deployment.customer.mobilitypackage;

import com.socotra.coremodel.*;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;
import com.socotra.deployment.customer.MobilityPackageQuote;
import com.socotra.deployment.customer.UWRules;
import com.socotra.deployment.customer.UnderwritingPlugin;
import com.socotra.deployment.customer.auto.AutoRatingPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Plugin to perform underwriting rules for MobilityPackage.
 */
public class MobilityUWPlugin implements UnderwritingPlugin {
    private static final Logger log = LoggerFactory.getLogger(AutoRatingPlugin.class);

    /**
     * Main underwriting method that processes a series of rules and applies underwriting flags.
     */
    @Override
    public UnderwritingModification underwrite(MobilityPackageQuoteRequest mobilityPackageQuoteRequest) {

        // Create Empty List of flags to create
        List<UnderwritingFlagCore> flagsToCreate = new ArrayList<>();

        MobilityPackageQuote quote = mobilityPackageQuoteRequest.quote();

        List<String> existingFlagsNotes = getNotesFromFlags(quote);

        // Init some variables
        ResourceSelector resourceSelector = ResourceSelectorFactory.getInstance().getSelector(mobilityPackageQuoteRequest.quote());

        // Iterate through underwriting rules, max 100
        for (int i = 1; i < 100; i++){

            // Fetch the UW rule for the current index
            Optional<UWRules> uwRulesRecord = resourceSelector.getTable(UWRules.class)
                    .getRecord(UWRules.makeKey("uwRule", "Motor", Integer.toString(i)));

            log.info("Processing rule ID: {}", i);

            // If no rule is found, exit the loop
            if (uwRulesRecord.isEmpty()) {
                log.info("No more underwriting rules found, exiting.");
                break;
            }

            // Process the found underwriting rule
            UWRules uwRule = uwRulesRecord.get();
            boolean uwResult = processUWRule(quote, uwRule);

            // If the underwriting result fails and the message is not already there, reject the risk and return modification
            if (uwResult && !existingFlagsNotes.contains(uwRule.message())) {
                flagsToCreate.add(createUnderwritingFlag(uwRule.decision(), uwRule.message()));
            }
            log.info("Underwriting result for rule ID {}: {}", i, uwResult);
        }

        // Check if the created list is too small and then either add approve or pass as is
        if (flagsToCreate.isEmpty()) {
            flagsToCreate.add(createUnderwritingFlag("approve", "Risk is acceptable"));
        }

        return UnderwritingModification.builder()
                .flagsToCreate(flagsToCreate)
                .build();
    }

    /**
     * Function to return a list of notes already present on a quote
     *
     * @param quote
     * @return List of notes
     */
    private static List<String> getNotesFromFlags(MobilityPackageQuote quote) {

        // Fetch existing underwriting flags for this quote
         var underwritingFlags = DataFetcher.getInstance().getQuoteUnderwritingFlags(quote.locator());

        // Initialize combinedFlags by combining both clearedFlags and flags, handling nulls
        List<UnderwritingFlag> combinedFlags = new ArrayList<>();

        if (underwritingFlags != null) {
            combinedFlags.addAll(Optional.ofNullable(underwritingFlags.clearedFlags()).orElse(new ArrayList<>()));
            combinedFlags.addAll(Optional.ofNullable(underwritingFlags.flags()).orElse(new ArrayList<>()));
        }

        log.info("Found following existing flags: {}", combinedFlags);

        if (combinedFlags.isEmpty()) {
            return new ArrayList<>();
        }

        return combinedFlags.stream()
                .map(flag -> flag.note().orElse("No Note"))  // Use "No Note" if Optional is empty
                .toList();
    }

    /**
     * Processes an individual underwriting rule and evaluates it against the quote data.
     */
    private boolean processUWRule(MobilityPackageQuote quote, UWRules uwRule) {
        String uwRuleKey = uwRule.key();
        String uwRuleCondition = uwRule.condition();
        String uwRuleValue = uwRule.value();

        log.info("Evaluating the following rule: {} {} {}", uwRuleKey, uwRuleCondition, uwRuleValue);

        // Retrieve the field value from the quote based on the UW rule key
        String fieldValue = getFieldValueFromQuote(quote.element(), uwRuleKey);

        // Determine if the field and rule values are integers or strings, and evaluate the rule
        if (isStringInteger(fieldValue) && isStringInteger(uwRuleValue)) {
            return evaluateUWRules("Integer", fieldValue, uwRuleCondition, uwRuleValue);
        } else {
            return evaluateUWRules("String", fieldValue, uwRuleCondition, uwRuleValue);
        }
    }

    private static String getFieldValueFromQuote(Element element, String keyPath) {

        List<String> keys = Arrays.stream(keyPath.split("\\."))
                .filter(key -> !key.isEmpty())  // Remove empty parts caused by leading dots
                .toList();

        // Start the recursive search with the main element and the first part of the key
        return getFieldValueFromHierarchy(element, keys, 0);
    }

    /**
     * Recursive helper method to navigate through the element hierarchy.
     *
     * @param element The current element being searched.
     * @param keys    The list of key components (e.g., ["motorLine", "vehicle", "totalSumInsured"]).
     * @param index   The current index of the key component being processed.
     * @return The field value if found, or null if not found.
     */
    private static String getFieldValueFromHierarchy(Element element, List<String> keys, int index) {
        // Base case: If we have reached the last key in the path, try to fetch the value from the element's data
        if (index == keys.size() - 1) {
            if (element.data().containsKey(keys.get(index))) {
                return element.data().get(keys.get(index)).toString();
            }
            return null;
        }

        // Recursive case: Look for the child elements that match the current key
        String currentKey = keys.get(index);
        for (Element childElement : element.elements()) {
            if (childElement.type().equalsIgnoreCase(currentKey + "Quote")) {
                // Recursively search within this matching child element
                String value = getFieldValueFromHierarchy(childElement, keys, index + 1);
                if (value != null) {
                    return value;
                }
            }
        }

        // Return null if no matching element or value is found
        return null;
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
     * Evaluates underwriting rules by comparing field values based on the type.
     */
    private static boolean evaluateUWRules(String type, Object fieldValue, String condition, Object ruleValue) {
        if (type.equalsIgnoreCase("Integer")) {
            Integer fieldInt = Integer.parseInt(fieldValue.toString());
            Integer ruleInt = Integer.parseInt(ruleValue.toString());

            return evaluateInteger(fieldInt, condition, ruleInt);
        } else if (type.equalsIgnoreCase("String")) {
            String fieldStr = fieldValue.toString();
            String ruleStr = ruleValue.toString();

            return evaluateString(fieldStr, condition, ruleStr);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /**
     * Evaluates integer-based underwriting rules.
     */
    private static boolean evaluateInteger(Integer fieldValue, String condition, Integer ruleValue) {
        return switch (condition) {
            case "<" -> fieldValue < ruleValue;
            case ">" -> fieldValue > ruleValue;
            case "=" -> Objects.equals(fieldValue, ruleValue);
            case "<=" -> fieldValue <= ruleValue;
            case ">=" -> fieldValue >= ruleValue;
            default -> throw new IllegalArgumentException("Unsupported condition: " + condition);
        };
    }

    /**
     * Evaluates string-based underwriting rules.
     */
    private static boolean evaluateString(String fieldValue, String condition, String ruleValue) {
        return switch (condition.toLowerCase()) {
            case "=" -> fieldValue.equalsIgnoreCase(ruleValue);
            case "!=" -> !fieldValue.equalsIgnoreCase(ruleValue);
            case "in" -> isIn(fieldValue, ruleValue);
            case "not in" -> !isIn(fieldValue, ruleValue);
            default -> throw new IllegalArgumentException("Unsupported condition: " + condition);
        };
    }

    /**
     * Checks if a string is present in a comma-separated list.
     */
    private static boolean isIn(String fieldValue, String ruleValue) {
        // Assume ruleValue is a comma-separated string of valid values
        List<String> validValues = Arrays.asList(ruleValue.split(","));
        return validValues.contains(fieldValue);
    }

    /**
     * Determines if a string can be parsed into an integer.
     */
    public static boolean isStringInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
