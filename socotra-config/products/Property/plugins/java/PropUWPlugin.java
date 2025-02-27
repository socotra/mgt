package com.socotra.deployment.customer.property;

import com.socotra.coremodel.*;
import com.socotra.deployment.*;
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

        ResourceSelector tableRecordFetcher = ResourceSelectorFactory.getInstance().getSelector(quote);

        // Create Empty List of flags to create
        List<UnderwritingFlagCore> flagsToCreate = new ArrayList<>();

        if (quote.data().classOfBusiness().hasMulticlassRisk().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().classOfBusiness().hasMulticlassRiskDesc() != null &&
                quote.data().classOfBusiness().hasMulticlassRiskDesc().length() > 0) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().eligibility().hasElevatorMaintenanceAgreement().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Blocked. No elevator agreement"));
        }  else if (quote.data().generalInformation().leasesHoldHarmless().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Does not hold harmless wording"));
        } else if (quote.data().eligibility().compliesADA().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with ADA."));
        } else if (!quote.data().eligibility().locationHazardsTenants().contains("N/A")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Location Hazards."));
        }   else if (quote.data().eligibility().hasOccupancyPartialTenantOwnership().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Partial Tenant Ownership."));
        } else if (quote.data().eligibility().hasCommercialCooking().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        }  else if (!(quote.data().eligibility().buildingWiringPlumbing().contains("N/A"))) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Building, wiring, plumbing block"));
        } else if (!(quote.data().eligibility().ineligibleOccupants().contains("None"))) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Ineligible occupants"));
        } else if (!(quote.data().eligibility().buildingHazards().contains("Have an elevator")) && !(quote.data().eligibility().buildingHazards().contains("None"))) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Building hazards"));
        } else if (quote.propertyLocations().size() > 5) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. More than 5 locations."));
        } else if (quote.data().generalInformation().ownsAllProperties().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Applicant does not own all properties."));
        } else if (quote.data().generalInformation().hasHistoricalCoverageGap().contains("Non-renewed citing underwriting reasons")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Non-renewed citing underwriting reasons."));
        } else if(!quote.data().generalInformation().hasHistoricalCoverageGap().contains("N/A") &&
                !quote.data().generalInformation().hasHistoricalCoverageGap().contains("Non-renewed, carrier product no longer supported")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Historical gap reason."));
        } else if (quote.data().generalInformation().hasFinancialHistoryIssue().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Financial history issue."));
        } else if (quote.data().generalInformation().hasLegalLiabilityIssue().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Legal Liability issue."));
        } else if (quote.data().eligibility().compliesNFPA().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with NFPA."));
        } else if (quote.data().eligibility().priorCodeViolation().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Prior Code Violation."));
        } else if (quote.data().claimHistory() != null && quote.data().claimHistory().size() > 0) {
            for (ClaimHistory claim : quote.data().claimHistory()) {
                // only applicable to GL
                if (claim.claimStatus().equalsIgnoreCase("Open")) {
                    flagsToCreate.add(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.block)
                            .note("Refer to underwriter. Claim Open.")
                            .build());
                } else if (claim.claimLineOfBusiness().length() > 0) {
                    flagsToCreate.add(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.block)
                            .note("Refer to underwriter. Line of business claim was filed.")
                            .build());
                }
            }
        } else {
            for (PropertyLocation loc : quote.propertyLocations()) {
                String hazardHubScore = loc.data().riskScores().hhScore();
                String hazardHubScoreMaxDeductible = tableRecordFetcher.getTable(HailWindTornadoScores.class).getRecord(HailWindTornadoScores.makeKey(hazardHubScore)).orElseThrow().requiredDeductible();
                log.info("max hazard-hub deductible: {}", hazardHubScoreMaxDeductible);

                String[] coastalStates = { "AL", "CT", "DE", "GA", "LA", "MA", "MD", "NC", "NJ", "NY", "RI", "SC", "TX", "VA"};
                String currentState = loc.data().packageProductBasicInfo().locationAddress().state();

                for (PropertyBuilding building : loc.propertyBuildings()) {
                    String constructionType = building.data().propertyConstructionUpdates().constructionType();
                    String hasSprinklers = building.data().propertySafeguards().hasSprinklers();
                    String highestSquareFootTenant = loc.data().packageOccupancyInfo().highestSquareFootTenant();
                    log.info("highest square foot tenant: {}", highestSquareFootTenant);

                    // retrieve fire risk for tenant
                    String fireRisk = tableRecordFetcher.getTable(FireRisk.class).getRecord(FireRisk.makeKey(highestSquareFootTenant)).orElseThrow().fireRisk();
                    log.info("fire risk for tenant: {}", fireRisk);

                    // retrieve building limit
                    if (constructionType.equalsIgnoreCase("MNC") || constructionType.equalsIgnoreCase("FR")) {
                        constructionType = "MNC/FR";
                    }
                    if (hasSprinklers.equalsIgnoreCase("N/A")) {
                        hasSprinklers = "No";
                    }
                    int buildingLimit = tableRecordFetcher.getTable(FireRiskLimits.class).getRecord(FireRiskLimits.makeKey(fireRisk, constructionType, hasSprinklers)).orElseThrow().buildingLimit();
                    log.info("building limit: {}", Integer.toString(buildingLimit));

                    if ((Year.now().getValue() - building.data().propertyConstructionUpdates().yearBuilt()) > 40) {
                        flagsToCreate.add(
                                createUnderwritingFlag("block", "Refer to underwriter. House is older than 40 years."));
                    } else if (loc.data().packageProductBasicInfo().isNamedAdditionalInsuredOnParking().contains("No")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Parking not insured."));
                    } else if (loc.data().packageProductBasicInfo().publicParkingAnnualSales() !=null && loc.data().packageProductBasicInfo().publicParkingAnnualSales() > 0) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Public parking receipts greater than 0."));
                    }
                    else if (building.data().propertyBuildingDetails().packageBuildingDetails().isPoolFenced().equalsIgnoreCase("No")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Pool unfenced."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasDivingBoardOrSlide().equalsIgnoreCase("Yes")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Has divingboard/slide."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasDepthMarkers().equalsIgnoreCase("No")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No depth markers."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasShepherdsCrookRing().equalsIgnoreCase("No")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No shephers crook ring."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasSelfClosingGate().equalsIgnoreCase("No")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No self closing gate."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasStructuresNearPool().equalsIgnoreCase("Yes")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Structures near pool."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasWarningSigns().equalsIgnoreCase("No")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No warning signs."));
                    } else if (building.data().propertyBuildingDetails().packageBuildingDetails().hasSlidingGlassLocks()
                            .equalsIgnoreCase("No")) {
                        flagsToCreate
                                .add(createUnderwritingFlag("block", "Refer to underwriter. No sliding glass locks."));
                    }
                    else if (building.data().propertyConstructionUpdates().ppcFireClass().equalsIgnoreCase("9")  ||
                            building.data().propertyConstructionUpdates().ppcFireClass().equalsIgnoreCase("10") ||
                            building.data().propertyConstructionUpdates().ppcFireClass().equalsIgnoreCase("6X-7X") ||
                            building.data().propertyConstructionUpdates().ppcFireClass().equalsIgnoreCase("1Y-5Y")  ||
                            building.data().propertyConstructionUpdates().ppcFireClass().equalsIgnoreCase("6Y-8Y") ||
                            building.data().propertyConstructionUpdates().ppcFireClass().equalsIgnoreCase("10W")
                    ) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. PPC Fire Class is PC 9."));
                    }  else if (building.data().propertyBuildingInfo().stories() > 4) {
                        flagsToCreate
                                .add(createUnderwritingFlag("block", "Blocked. Building is taller than 4 stories."));
                    } else if (building.data().propertyBuildingInfo().area() > 25000) {
                        flagsToCreate.add(
                                createUnderwritingFlag("reject", "Rejected. Building is greater than 25k sq. ft.."));
                    } else if (building.data().propertyConstructionUpdates().roofCovering().equalsIgnoreCase("Wood")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Roof is made of wood."));
                    } else if (((Year.now().getValue()
                            - building.data().propertyConstructionUpdates().parkingLastUpdated()) > 30) &&
                            building.data().propertyConstructionUpdates().hasElectricVehicleCharging()
                                    .equalsIgnoreCase("Yes")) {
                        flagsToCreate.add(createUnderwritingFlag("reject",
                                "Rejected. Has electrical vehicle charging and parking lot was updated more than 30 years ago."));
                    } else if (building.data().propertyBuildingDetails().isHistorical().equalsIgnoreCase("Yes")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Historical building."));
                    } else if (!building.data().propertyBuildingDetails().undergroundHazards().contains("None")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Underground hazards."));
                    } else if (building.data().propertyBuildingOperationDetails().permitsSpaceHeaters().equalsIgnoreCase("Yes")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Permits space heaters."));
                    } else if (building.data().propertyBuildingOperationDetails().permitsBalconyCooking().equalsIgnoreCase("Yes")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Permits Balcony Cooking."));
                    } else if (hazardHubScore.equalsIgnoreCase("B") && building.data().buildingCoverageTerms().windHailDeductible().equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Hazard Hub Score B must have mininum deductible of 1%/$2,500 mininum."));
                    } else if ((hazardHubScore.equalsIgnoreCase("C") && building.data().buildingCoverageTerms().windHailDeductible().equalsIgnoreCase("Exclude Wind/Hail Coverage")) ||
                            (hazardHubScore.equalsIgnoreCase("C") && building.data().buildingCoverageTerms().windHailDeductible().equalsIgnoreCase("1.0%"))){
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Hazard Hub Score C must have mininum deductible of 2%/$2,500 mininum."));
                    } else if ((hazardHubScore.equalsIgnoreCase("D") && building.data().buildingCoverageTerms().windHailDeductible().equalsIgnoreCase("Exclude Wind/Hail Coverage")) ||
                            (hazardHubScore.equalsIgnoreCase("D") && building.data().buildingCoverageTerms().windHailDeductible().equalsIgnoreCase("1.0%")) ||
                            (hazardHubScore.equalsIgnoreCase("D") && building.data().buildingCoverageTerms().windHailDeductible().equalsIgnoreCase("2.0%"))){
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Hazard Hub Score D must have mininum deductible of 5%/$10,000 mininum."));
                    } else if (hazardHubScore.equalsIgnoreCase("F")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Hazard Hub Score F, exclude."));
                    } else if (loc.data().riskScores().wildfireScore().equalsIgnoreCase("C") || loc.data().riskScores().wildfireScore().equalsIgnoreCase("D") || loc.data().riskScores().wildfireScore().equalsIgnoreCase("E") || loc.data().riskScores().wildfireScore().equalsIgnoreCase("F")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Wild fire score is too high."));
                    } else if (loc.data().glSecurity().vacancyProcedures().contains("None of the above")) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No vacancy procedures."));
                    } else if (!loc.data().glSecurity().vacancyProcedures().contains("Change locks/Collect keys") ||
                            !loc.data().glSecurity().vacancyProcedures().contains("Property inspection") ||
                            !loc.data().glSecurity().vacancyProcedures().contains("Handle security deposit according to state and local laws") ||
                            !loc.data().glSecurity().vacancyProcedures().contains("Make necessary repairs") ||
                            !loc.data().glSecurity().vacancyProcedures().contains("Terminate lease agreement")
                    ) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Not all vacancy procedures selected."));
                    } else if (loc.data().glSecurity().securityArmed().equalsIgnoreCase("Armed")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Armed security."));
                    } else if (building.data().buildingCoverageTerms().buildingValue() > buildingLimit) {
                        flagsToCreate.add(createUnderwritingFlag("block", "Blocked. Building value exceeds maximum allotted value, due to frame, fire-risk, and construction."));
                    } else if (check(coastalStates, currentState)) {
                        if (!constructionType.equalsIgnoreCase("Frame")) {
                            constructionType = "Non-Frame";
                        }
                        log.info("construction type: {}", constructionType);
                        // retrieve miles to coast
                        int distanceToCoast = loc.data().riskScores().distanceToCoastScore();

                        // make underwriting decision based on distance
                        String decisionPercent = "";
                        String decisionDollar = "";
                        if (distanceToCoast <= 1) {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().oneMilePercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().oneMileDollar();
                        } else if (distanceToCoast < 2) {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().twoMilePercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().twoMileDollar();
                        } else if (distanceToCoast < 5) {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().fiveMilePercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().fiveMileDollar();
                        } else if (distanceToCoast < 10) {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().tenMilePercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().tenMileDollar();
                        } else if (distanceToCoast < 20) {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().twentyMilePercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().twentyMileDollar();
                        } else if (distanceToCoast < 30) {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().thirtyMilePercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().thirtyMileDollar();
                        } else {
                            decisionPercent = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().overThirtyPercent();
                            decisionDollar = tableRecordFetcher.getTable(DistanceToCoast.class).getRecord(DistanceToCoast.makeKey(currentState, constructionType)).orElseThrow().overThirtyDollar();
                        }
                        if (decisionPercent.equalsIgnoreCase("Ineligible")) {
                            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Distance to coast ineligible."));
                        } else if (!decisionPercent.equalsIgnoreCase("N/A")) {
                            String windHailDeductiblePercent = building.data().buildingCoverageTerms().windHailDeductible();
                            String windHailDeductibleDollar = building.data().buildingCoverageTerms().windHailDollarAmount();
                            if (!decisionPercent.equalsIgnoreCase(windHailDeductiblePercent)) {
                                if (decisionPercent.equalsIgnoreCase("Exclude Wind/Hail Coverage")) {
                                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Wind/Hail Coverage has been excluded for coastal state."));
                                } else if (decisionPercent.equalsIgnoreCase("2.0%") && windHailDeductiblePercent.equalsIgnoreCase("1.0%")) {
                                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Wind/Hail Deductible needs to be more than 1.0%."));
                                } else if (decisionPercent.equalsIgnoreCase("5.0%") && (windHailDeductiblePercent.equalsIgnoreCase("1.0%") || windHailDeductiblePercent.equalsIgnoreCase("2.0%"))) {
                                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Wind/Hail Deductible needs to be 5.0%."));
                                }
                            } else if (!decisionDollar.equalsIgnoreCase(windHailDeductibleDollar)) {
                                if (decisionDollar.equalsIgnoreCase("$5,000") && windHailDeductibleDollar.equalsIgnoreCase("$2,500")) {
                                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Wind/Hail Coverage ($) needs to be more than $2,500."));
                                } else if (decisionDollar.equalsIgnoreCase("$10,000") && (windHailDeductibleDollar.equalsIgnoreCase("$2,500") || windHailDeductibleDollar.equalsIgnoreCase("$5,000"))) {
                                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Wind/Hail Deductible needs to be $10,000."));
                                }
                            }
                        }
                    }
                }
                // refer any occupancy > 10%
                if (!loc.data().packageOccupancyInfo().vacantSquareFootage().equalsIgnoreCase("0")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Blocked. Vacancy Square Footage present."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().adultOnly() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().adultOnly() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy of adults."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().studentHousing() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().studentHousing() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy of students."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().subsidizedHousing() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().subsidizedHousing() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy of subsidized housing."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().elderlyHousing() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().elderlyHousing() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy for elderly housing."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().handicap() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().handicap() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy for handicap."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().halfwayHouse() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().halfwayHouse() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy for halfway-house."));
                } else if (loc.data().packageOccupancyInfo().occBreakdown().mentalDrugRehab() != null &&
                        loc.data().packageOccupancyInfo().occBreakdown().mentalDrugRehab() > 10) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. More than 10% occupancy for mental/drug rehab."));
                }
            }
        }

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