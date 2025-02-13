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
import java.time.Year;

public class GLUnderwritingPlugin implements UnderwritingPlugin {
    private static final Logger log = LoggerFactory.getLogger(GLUnderwritingPlugin.class);
    @Override
    public UnderwritingModification underwrite(GeneralLiabilityQuoteRequest generalLiabilityQuoteRequest) {

        GeneralLiabilityQuote quote = generalLiabilityQuoteRequest.quote();

        // Create Empty List of flags to create
        List<UnderwritingFlagCore> flagsToCreate = new ArrayList<>();
        if (quote.data().classOfBusiness().hasMulticlassRisk().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (quote.data().classOfBusiness().hasMulticlassRiskDesc() != null &&
                quote.data().classOfBusiness().hasMulticlassRiskDesc().length() > 0) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        } else if (!(quote.data().eligibility().buildingWiringPlumbing().contains("N/A"))) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Building, wiring, plumbing block"));
        } else if (!(quote.data().eligibility().ineligibleOccupants().contains("None"))) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Ineligible occupants"));
        } else if (quote.data().eligibility().hasElevatorMaintenanceAgreement().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Blocked. No elevator agreement"));
        }  else if (!(quote.data().eligibility().buildingHazards().contains("Have an elevator")) && !(quote.data().eligibility().buildingHazards().contains("None"))) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Building hazards"));
        }else if (quote.data().eligibility().hasCommercialCooking().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Business entails services outside of what is listed."));
        }  else if (quote.data().generalInformation().leasesHoldHarmless().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Does not hold harmless wording"));
        } else if (quote.data().eligibility().compliesADA().equalsIgnoreCase("No")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Does not comply with ADA."));
        } else if (!quote.data().eligibility().locationHazardsTenants().contains("N/A")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Location Hazards."));
        } else if (!quote.data().eligibility().locationHazardsOperations().contains("N/A")) {
            flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Location has hazardous operations."));
        } else if (quote.locations().size() > 5) {
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
        } else if (quote.data().eligibility().hasOccupancyPartialTenantOwnership().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Partial Tenant Ownership."));
        } else if (quote.data().claimHistory() != null && quote.data().claimHistory().size() > 0) {
            for (ClaimHistory claim : quote.data().claimHistory()) {
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
            for (Location loc : quote.locations()) {
                if (loc.data().packageProductBasicInfo().isNamedAdditionalInsuredOnParking().contains("No")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Parking not insured."));
                } else if (loc.data().packageProductBasicInfo().publicParkingAnnualSales() !=null && loc.data().packageProductBasicInfo().publicParkingAnnualSales() > 0) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Public parking receipts greater than 0."));
                } else if (loc.data().glOperationalDetails().leaseDisclaimsSecurityWarranties().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Lease disclaims security warranties"));
                } else if (loc.data().packageBuildingDetails().isPoolFenced().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Pool unfenced."));
                } else if (loc.data().packageBuildingDetails().hasDivingBoardOrSlide().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Has divingboard/slide."));
                } else if (loc.data().packageBuildingDetails().hasDepthMarkers().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No depth markers."));
                } else if (loc.data().packageBuildingDetails().hasShepherdsCrookRing().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No shephers crook ring."));
                } else if (loc.data().packageBuildingDetails().hasSelfClosingGate().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No self closing gate."));
                } else if (loc.data().packageBuildingDetails().hasStructuresNearPool().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Structures near pool."));
                } else if (loc.data().packageBuildingDetails().hasWarningSigns().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No warning signs."));
                } else if (loc.data().packageBuildingDetails().hasSlidingGlassLocks().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No sliding glass locks."));
                } else if (loc.data().glSecurity().hasPeepholesOrDeadbolts().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No peepholdes or deadbolts."));
                } else if (loc.data().glSecurity().doesBackgroundChecksEmployee().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No employee background-check."));
                } else if (loc.data().glSecurity().doesBackgroundChecksTenant().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No tenant background-check."));
                } else if (loc.data().disclaimsSecurity().equalsIgnoreCase("No")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No security disclaimers."));
                } else if (loc.data().glSecurity().vacancyProcedures().size() == 0) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No vacancy procedures."));
                } else if (loc.data().propertyBuildingOperationDetails().permitsBalconyCooking().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Permits Balcony Cooking."));
                } else if (loc.data().propertyBuildingOperationDetails().permitsSpaceHeaters().equalsIgnoreCase("Yes")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Permits space heaters."));
                } else if (loc.data().glSecurity().securityArmed().equalsIgnoreCase("Armed")) {
                    flagsToCreate.add(createUnderwritingFlag("reject", "Rejected. Armed security."));
                } else if (!loc.data().packageOccupancyInfo().vacantSquareFootage().equalsIgnoreCase("0")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Blocked. Vacancy Square Footage present."));
                } else if (loc.data().glSecurity().vacancyProcedures().contains("None of the above")) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. No vacancy procedures."));
                } else if (!loc.data().glSecurity().vacancyProcedures().contains("Change locks/Collect keys") ||
                        !loc.data().glSecurity().vacancyProcedures().contains("Property inspection") ||
                        !loc.data().glSecurity().vacancyProcedures().contains("Handle security deposit according to state and local laws") ||
                        !loc.data().glSecurity().vacancyProcedures().contains("Make necessary repairs") ||
                        !loc.data().glSecurity().vacancyProcedures().contains("Terminate lease agreement")
                ) {
                    flagsToCreate.add(createUnderwritingFlag("block", "Refer to underwriter. Not all vacancy procedures selected."));
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