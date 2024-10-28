package com.socotra.deployment.customer.auto;

import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.deployment.customer.AutoQuote;
import com.socotra.deployment.customer.ChargeType;
import com.socotra.deployment.customer.RatePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AutoRatingPlugin implements RatePlugin {
    private static final Logger log = LoggerFactory.getLogger(AutoRatingPlugin.class);

    @Override
    public RatingSet rate(AutoQuoteRequest autoQuoteRequest) {

        AutoQuote quote = autoQuoteRequest.quote();

        List<RatingItem> ratingItems = new ArrayList<>();

        ratingItems.add(RatingItem.builder()
                .elementLocator(quote.motorLine().vehicle().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(100))
                .build());

        return RatingSet.builder().ratingItems(ratingItems).ok(true).build();
    }

}
