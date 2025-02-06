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

        if (quote.data().classOfBusiness().hasMulticlassRisk().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().classOfBusiness().hasMulticlassRiskDesc() != null &&
                quote.data().classOfBusiness().hasMulticlassRiskDesc().length() > 0) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().generalInformation().leasesHoldHarmless().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Does not hold harmless wording"));
        } else if (quote.data().eligibility().compliesADA().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with ADA."));
        } else if (!quote.data().eligibility().locationHazardsTenants().contains("N/A")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Location Hazards."));
        }   else if (quote.data().eligibility().hasOccupancyPartialTenantOwnership().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Partial Tenant Ownership."));
        }

        else if (quote.data().eligibility().hasCommercialCooking().equalsIgnoreCase("Yes")) {
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
                for (PropertyBuilding building : loc.propertyBuildings()) {
                    if ((Year.now().getValue() - building.data().propertyConstructionUpdates().yearBuilt()) > 40) {
                        flagsToCreate.add(
                                createUnderwritingFlag("block", "Refer to underwriter. House is older than 40 years."));
                    } else if (loc.data().packageProductBasicInfo().isNamedAdditionalInsuredOnParking().contains("No")) {
                        flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Parking not insured."));
                    } else if (loc.data().packageProductBasicInfo().publicParkingAnnualSales() !=null && loc.data().packageProductBasicInfo().publicParkingAnnualSales() >= 0) {
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
}