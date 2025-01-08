package com.socotra.deployment.customer.property;

import com.socotra.coremodel.*;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.customer.PropertyQuote;
import com.socotra.coremodel.UnderwritingFlagCore;
import com.socotra.coremodel.UnderwritingLevel;
import com.socotra.coremodel.UnderwritingModification;
import com.socotra.deployment.customer.UnderwritingPlugin;
import javax.xml.crypto.Data;
import java.util.List;

public class PropUWPlugin implements UnderwritingPlugin {

    @Override
    public UnderwritingModification underwrite(PropertyQuoteRequest propertyQuoteRequest) {

        PropertyQuote quote = propertyQuoteRequest.quote();

        // if ((quote.data().packageLocations() && quote.data().packageLocations().length > 5) || 
        //     (quote.data().propertyLocations() && quote.propertyLocations().length > 5) ||
        //         (quote.data().locations() && quote.locations().length > 5)) {
        //     return UnderwritingModification.builder()
        //             .flagsToCreate(List.of(UnderwritingFlagCore.builder()
        //                     .level(UnderwritingLevel.decline)
        //                     .note("Decline if more than 5 locations")
        //                     .build()))
        //             .build();
        // }
        // if ((quote.data().packageLocations() && quote.data().packageLocations().length > 5) || 
        //     (quote.data().propertyLocations() && quote.propertyLocations().length > 5) ||
        //         (quote.data().locations() && quote.locations().length > 5)) {
        //     return UnderwritingModification.builder()
        //             .flagsToCreate(List.of(UnderwritingFlagCore.builder()
        //                     .level(UnderwritingLevel.decline)
        //                     .note("Decline if more than 5 locations")
        //                     .build()))
        //             .build();
        // }
        // if (quote.data().packageQuestions().ownsAllProperties().equalsIgnoreCase("Yes")) {

        // }
        if (quote.data().packageQuestions().hasFinancialHistoryIssue().equalsIgnoreCase("Yes")) {
            return UnderwritingModification.builder()
                    .flagsToCreate(List.of(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.decline)
                            .note("Deny if financial history issue")
                            .build()))
                    .build();
        } else {
            return UnderwritingModification.builder()
                    .flagsToCreate(List.of(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.approve)
                            .build()))
                    .build();
        }
    }
}