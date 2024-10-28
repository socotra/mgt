package com.socotra.deployment.customer.mobilitypackage;

import com.socotra.coremodel.DocumentDataSnapshot;
import com.socotra.coremodel.QuotePricing;
import com.socotra.deployment.DataFetcherFactory;
import com.socotra.deployment.customer.ConsumerAccount;
import com.socotra.deployment.customer.DocumentDataSnapshotPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MobilityDataSnaphot implements DocumentDataSnapshotPlugin {

    private static final Logger log = LoggerFactory.getLogger(DocumentDataSnapshotPlugin.class);
    @Override
    public DocumentDataSnapshot dataSnapshot(MobilityPackageQuoteRequest mobilityPackageQuoteRequest) {

        var quote= mobilityPackageQuoteRequest.quote();
        var pricing = DataFetcherFactory.get().getQuotePricing(quote.locator());
        ConsumerAccount account = DataFetcherFactory.get().getAccount(quote.accountLocator());

        Map<String, Object> combinedData = new HashMap<>();
        // Pricing Data Mapping
        Map<String, Object> pricingData = getPricingData(pricing);


        // Account Data Mapping
        Map<String, Object> accountData = new HashMap<>();

        accountData.put("firstName", account.data().firstName());
        accountData.put("lastName", account.data().lastName());
        accountData.put("dob", account.data().dob());
        accountData.put("contactEmail", account.data().contactEmail());
        accountData.put("contactPhone", account.data().contactPhone());

        // Policy Data Mapping
        Map<String, Object> policyData = new HashMap<>();

        policyData.put("vehicle", quote.motorLine().vehicle());
        String startTime = quote.startTime().get().toString();
        policyData.put("startDate", startTime);
        policyData.put("quoteNumber", quote.locator());
        policyData.put("planOption", quote.motorLine().data().planOption());
        policyData.put("RCLimit",quote.motorLine().RC().limiteRC().displayName());

        combinedData.put("policy", policyData);
        combinedData.put("account", accountData);
        combinedData.put("pricing", pricingData);

        return DocumentDataSnapshot.builder()
                .renderingData(combinedData)
                .build();
    }

    private static Map<String, Object> getPricingData(QuotePricing pricing) {
        Map<String, Object> pricingData = new HashMap<>();
        double premium = 0;
        double discounts = 0;

        log.info("Pricing Data: {}", pricing);

        for (var item : pricing.items()) {

            if (item.chargeType().equalsIgnoreCase("premium")) {
                premium += item.rate().doubleValue();
            } else if (item.chargeType().equalsIgnoreCase("credit")) {
                discounts += item.rate().doubleValue();
            }
        }

        pricingData.put("premium", premium);
        pricingData.put("discounts", discounts);
        pricingData.put("total", premium+discounts);
        return pricingData;
    }
}
