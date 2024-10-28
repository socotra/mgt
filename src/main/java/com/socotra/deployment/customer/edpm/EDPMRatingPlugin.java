package com.socotra.deployment.customer.edpm;

import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;
import com.socotra.deployment.customer.*;
import com.socotra.deployment.customer.RatePlugin;

import java.util.ArrayList;
import java.util.List;

public class EDPMRatingPlugin implements RatePlugin {

    @Override
    public RatingSet rate(EDPMQuoteRequest edpmQuoteRequest) {

        EDPMQuote quote = edpmQuoteRequest.quote();
        ResourceSelector resourceSelector = ResourceSelectorFactory.getInstance().getSelector(quote);

        Rater rater = new Rater(resourceSelector);

        List<RatingItem> ratingItems = new ArrayList<>(rater.rateEPDM(quote.EDPMLine()));

        return RatingSet.builder().ratingItems(ratingItems).ok(true).build();
    }

    @Override
    public RatingSet rate(EDPMRequest edpmRequest) {
        return RatePlugin.super.rate(edpmRequest);
    }
}
