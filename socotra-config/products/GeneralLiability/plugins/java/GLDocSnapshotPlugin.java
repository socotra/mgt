package com.socotra.deployment.customer.generalliability;
import com.socotra.deployment.customer.*;
import com.socotra.coremodel.DocumentDataSnapshot;
import com.socotra.deployment.DataFetcherFactory;
import com.socotra.deployment.customer.DocumentDataSnapshotPlugin;
import com.socotra.deployment.customer.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GLDocSnapshotPlugin implements DocumentDataSnapshotPlugin {

    private static final Logger log = LoggerFactory.getLogger(DocumentDataSnapshotPlugin.class);

    @Override
    public DocumentDataSnapshot dataSnapshot(GeneralLiabilityQuoteRequest generalLiabilityQuoteRequest) {

        var account = DataFetcherFactory.get().getAccount(generalLiabilityQuoteRequest.quote().accountLocator());
        log.info("In dataSnapshot plugin, account is: {}", account);

        Map<String, Object> combinedData = new HashMap<>();
        combinedData.put("policy", generalLiabilityQuoteRequest.quote());
        combinedData.put("account", account);

        log.info("In dataSnapshot plugin, combined data is: {}", combinedData);
        return DocumentDataSnapshot.builder()
                .renderingData(combinedData)
                .build();
    }
}