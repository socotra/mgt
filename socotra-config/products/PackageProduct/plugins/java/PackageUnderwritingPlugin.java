package com.socotra.deployment.customer.packageproduct;

import com.socotra.coremodel.*;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.customer.PackageProductQuote;
import com.socotra.deployment.customer.UnderwritingPlugin;
import com.socotra.deployment.customer.*;

import javax.xml.crypto.Data;
import java.util.*;

public class PackageUnderwritingPlugin implements UnderwritingPlugin {

    @Override
    public UnderwritingModification underwrite(PackageProductQuoteRequest packageProductQuoteRequest) {

        PackageProductQuote quote = packageProductQuoteRequest.quote();

        // Create Empty List of flags to create
        List<UnderwritingFlagCore> flagsToCreate = new ArrayList<>();

        if (quote.data().glQuestions().hasMulticlassRisk().equalsIgnoreCase("Yes")) {
            flagsToCreate.add(UnderwritingFlagCore.builder()
                    .level(UnderwritingLevel.block)
                    .note("Refer to underwriter. Business entails services outside of what is listed.")
                    .build());
        }
        if (quote.data().glQuestions().hasMulticlassRiskDesc().length() > 0) {
            flagsToCreate.add(UnderwritingFlagCore.builder()
                    .level(UnderwritingLevel.block)
                    .note("Refer to underwriter. Business entails services outside of what is listed.")
                    .build());
        }
        // prop and gl
        if (quote.packageLocations().size() > 5) {
            flagsToCreate.add(UnderwritingFlagCore.builder()
                    .level(UnderwritingLevel.reject)
                    .note("Rejected. More than 5 locations.")
                    .build());
        }

        return UnderwritingModification.builder()
                .flagsToCreate(flagsToCreate)
                .build();

    }

}