package com.socotra.deployment.customer.mobilitypackage;

import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;
import com.socotra.deployment.customer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MobilityRatingPlugin implements RatePlugin {

    private static final Logger log = LoggerFactory.getLogger(MobilityRatingPlugin.class);

    @Override
    public RatingSet rate(MobilityPackageQuickQuoteRequest mobilityPackageQuickQuoteRequest) {
        MobilityPackageQuickQuote quickQuote = mobilityPackageQuickQuoteRequest.quote();
        ResourceSelector resourceSelector = ResourceSelectorFactory.getInstance().getSelector(quickQuote);

        Rater rater = new Rater(resourceSelector);
        List<RatingItem> ratingItems = new ArrayList<>(rater.rateQuickQuoteMotor(quickQuote.motorLine()));

        return RatingSet.builder().ratingItems(ratingItems).ok(true).build();
    }

    @Override
    public RatingSet rate(MobilityPackageQuoteRequest mobilityPackageQuoteRequest) {

        MobilityPackageQuote quote = mobilityPackageQuoteRequest.quote();
        ResourceSelector resourceSelector = ResourceSelectorFactory.getInstance().getSelector(quote);

        // Initiate Rating Function
        Rater rater = new Rater(resourceSelector);
        List<RatingItem> ratingItems = new ArrayList<>(rater.rateMotor(quote.motorLine()));

        if (quote.EDPMLine() != null) {
            ratingItems.addAll(rater.rateEPDM(quote.EDPMLine()));
        }

        BigDecimal premium = ratingItems.stream()
                .filter(item -> ChargeType.premium.equals(item.chargeType()))
                .map(RatingItem::rate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Premium calculated: {}", premium);
        log.info("Number of policy lines: {}", quote.element().elements().size());

        // Add Package Discount if applicable
        applyPackageDiscount(ratingItems, resourceSelector, quote, premium);

        return RatingSet.builder().ratingItems(ratingItems).ok(true).build();
    }

    private void applyPackageDiscount(List<RatingItem> ratingItems, ResourceSelector resourceSelector,
                                      MobilityPackageQuote quote, BigDecimal premium) {
        if (quote.element().elements().size() > 1 && premium.compareTo(BigDecimal.ZERO) > 0) {
            var packageDiscountRecord = resourceSelector.getTable(Discounts.class)
                    .getRecord(Discounts.makeKey("bundle", Integer.toString(quote.element().elements().size())));

            log.info("Package Discount Factor found: {}", packageDiscountRecord);

            double packageDiscountFactor = packageDiscountRecord.map(
                    discount -> discount.factor().doubleValue()).orElse(1.0);

            BigDecimal packageDiscountPremium = premium.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(packageDiscountFactor)))
                    .negate().setScale(0, RoundingMode.HALF_UP);;

            log.info("Package Discount Premium calculated: {}", packageDiscountPremium);

            if (packageDiscountPremium.compareTo(BigDecimal.ZERO) != 0) {
                ratingItems.add(RatingItem.builder()
                        .elementLocator(quote.locator())
                        .chargeType(ChargeType.packageDiscount)
                        .rate(packageDiscountPremium)
                        .build());
            }
        }
    }

}
