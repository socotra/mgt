package com.socotra.deployment.customer.mobilitypackage;

import com.socotra.deployment.customer.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class MobilityPrecommit implements PreCommitPlugin {

    @Override
    public MobilityPackageQuote preCommit(MobilityPackageQuoteRequest mobilityPackageQuoteRequest) {

        // Retrieve the quote from the request
        MobilityPackageQuote quote = mobilityPackageQuoteRequest.quote();

        // Set Expiration Date
        // Retrieve and adjust the start date
        Optional<Instant> startDate = quote.startTime();
        Instant expirationDate = startDate
                .map(instant -> instant.plus(30, ChronoUnit.DAYS)) // Add 30 days if present
                .orElse(null); // Handle the absence of startDate (optional logic)

        // Get the planOption and ensure it is either Silver, Gold, or Platinum
        PlanOption planOption = PlanOption.fromString(quote.motorLine().data().planOption());

        // Update vehicle coverage based on the plan option
        var newVehicle = addVehicleCoverage(quote.motorLine().vehicle(), planOption);

        // Update EPDM Line if present
        Collection<BikeQuote> newBikes = new ArrayList<>();
        if (quote.EDPMLine() != null && quote.EDPMLine().bikes() != null) {
            newBikes = quote.EDPMLine().bikes().stream()
                    .map(bike -> bike.toBuilder()
                            .addCasseVelo(casseVeloQuoteBuilder -> {})  // Add casseVelo with no specific configuration
                            .franchiseVelo(FranchiseVelo.defaultOption()) // Default franchiseVelo
                            .build())
                    .collect(Collectors.toList());
        }

        // Update Scooters if present
        Collection<ScooterQuote> newScooters = new ArrayList<>();
        if (quote.EDPMLine() != null && quote.EDPMLine().scooters() != null) {
            newScooters = quote.EDPMLine().scooters().stream()
                    .map(scooter -> scooter.toBuilder()
                            .addRC(rcQuoteBuilder -> rcQuoteBuilder.limiteRC(LimiteRC.defaultOption()))  // Add default RC limit
                            .addProtectionConducteur(protectionConducteurQuoteBuilder -> protectionConducteurQuoteBuilder
                                    .limiteProtectionConducteur(LimiteProtectionConducteur.defaultOption())) // Add default protection conducteur
                            .build())
                    .collect(Collectors.toList());
        }

        // Return the updated quote with modified motorLine and EPDMLine
        return quote.toBuilder()
                .expirationTime(expirationDate)
                .motorLine(
                        quote.motorLine().toBuilder()
                                .vehicle(newVehicle)  // Set the updated vehicle coverage
                                .build()
                )
                .EDPMLine(quote.EDPMLine() != null ?
                        quote.EDPMLine().toBuilder()
                                .bikes(newBikes)      // Set updated bikes
                                .scooters(newScooters) // Set updated scooters
                                .build()
                        : null) // Set EPDMLine to null if not present
                .build();
    }

    // Method to add vehicle coverage based on the plan option (Silver, Gold, Platinum)
    private static VehicleQuote addVehicleCoverage(VehicleQuote currentVehicle, PlanOption planOption) {
        VehicleQuote.VehicleQuoteBuilder builder = currentVehicle.toBuilder();

        switch (planOption) {
            case SILVER:
                // For Silver plan, add specific coverages and clear others
                builder.addAssistance(b -> addAssistanceCoverageBasedOnPlan(b, planOption))
                        .addMiniOmnium(b -> addMiniOmniumCoverageBasedOnPlan(b, planOption))
                        .omnium(null)
                        .vehiculeRemplacement(null)
                        .accessoires(null)
                        .protectionConducteur(null);
                break;
            case GOLD:
                // For Gold plan, add additional protection coverage
                builder.addAssistance(b -> addAssistanceCoverageBasedOnPlan(b, planOption))
                        .addMiniOmnium(b -> addMiniOmniumCoverageBasedOnPlan(b, planOption))
                        .addProtectionConducteur(b -> addProtectionConducteurBasedOnPlan(b, planOption))
                        .omnium(null)
                        .vehiculeRemplacement(null)
                        .accessoires(null);
                break;
            case PLATINUM:
                // For Platinum plan, add full coverages
                builder.addAssistance(b -> addAssistanceCoverageBasedOnPlan(b, planOption))
                        .addOmnium(MobilityPrecommit::addOmniumCoverageBasedOnPlan)
                        .addProtectionConducteur(b -> addProtectionConducteurBasedOnPlan(b, planOption))
                        .addVehiculeRemplacement(MobilityPrecommit::addVehicleRemplacementBasedOnPlan)
                        .addAccessoires(MobilityPrecommit::addAccessoiresBasedOnPlan)
                        .miniOmnium(null);  // Clear Mini Omnium for Platinum plan
                break;
        }

        return builder.build();
    }

    // Add Mini Omnium coverage based on the plan option
    private static void addMiniOmniumCoverageBasedOnPlan(MiniOmniumQuote.MiniOmniumQuoteBuilder builder, PlanOption planOption) {
        switch (planOption) {
            case SILVER:
                builder.addBrisDeGlace(b -> b.franchiseBrisDeGlace(FranchiseBrisDeGlace.defaultOption()))
                        .addCatastrophesNaturelles(c -> c.franchiseCatastropheNaturelles(FranchiseCatastropheNaturelles.defaultOption()))
                        .addVolEtIncendie(v -> v.franchiseVol(FranchiseVol.defaultOption()));
                break;
            case GOLD:
                builder.addBrisDeGlace(b -> b.franchiseBrisDeGlace(FranchiseBrisDeGlace.ded100))
                        .addCatastrophesNaturelles(c -> c.franchiseCatastropheNaturelles(FranchiseCatastropheNaturelles.ded300))
                        .addVolEtIncendie(v -> v.franchiseVol(FranchiseVol.ded500));
                break;
            default:
                throw new IllegalArgumentException("Invalid plan option: " + planOption);
        }
    }

    // Add Omnium coverage based on the plan option
    private static void addOmniumCoverageBasedOnPlan(OmniumQuote.OmniumQuoteBuilder builder) {
        builder.addBrisDeGlace(b -> b.franchiseBrisDeGlace(FranchiseBrisDeGlace.defaultOption()))
                .addVandalisme(v -> v.franchiseVandalisme(FranchiseVandalisme.defaultOption()))
                .addDommagesAnimaux(d -> {})
                .addDommagesMateriels(d -> d.limiteDommagesMateriels(LimiteDommagesMateriels.defaultOption()))
                .addCatastrophesNaturelles(c -> c.franchiseCatastropheNaturelles(FranchiseCatastropheNaturelles.defaultOption()))
                .addVolEtIncendie(v -> v.franchiseVol(FranchiseVol.defaultOption()));
    }

    // Add Assistance coverage based on the plan option
    private static void addAssistanceCoverageBasedOnPlan(AssistanceQuote.AssistanceQuoteBuilder builder, PlanOption planOption) {
        switch (planOption) {
            case SILVER:
                builder.limiteAssistanceDepannage(LimiteAssistanceDepannage.limit200);
                break;
            case GOLD:
                builder.limiteAssistanceDepannage(LimiteAssistanceDepannage.limit500);
                break;
            case PLATINUM:
                builder.limiteAssistanceDepannage(LimiteAssistanceDepannage.limitUnlimited);
                break;
        }
    }

    // Add Protection Conducteur coverage based on the plan option
    private static void addProtectionConducteurBasedOnPlan(ProtectionConducteurQuote.ProtectionConducteurQuoteBuilder builder, PlanOption planOption) {
        switch (planOption) {
            case GOLD:
                builder.limiteProtectionConducteur(LimiteProtectionConducteur.limit25000);
                break;
            case PLATINUM:
                builder.limiteProtectionConducteur(LimiteProtectionConducteur.limit50000);
                break;
        }
    }

    // Add Vehicle Remplacement coverage
    private static void addVehicleRemplacementBasedOnPlan(VehiculeRemplacementQuote.VehiculeRemplacementQuoteBuilder builder) {
        builder.limiteVehiculeRemplacement(LimiteVehiculeRemplacement.defaultOption());
    }

    // Add Accessoires coverage
    private static void addAccessoiresBasedOnPlan(AccessoiresQuote.AccessoiresQuoteBuilder builder) {
        builder.limiteAccessoires(LimiteAccessoires.defaultOption());
    }
}
