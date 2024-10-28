package com.socotra.deployment.customer;

import java.util.UUID;

public class GlobalPrecommit implements PreCommitPlugin{

    @Override
    public ConsumerAccount preCommit(ConsumerAccountRequest consumerAccountRequest) {

        ConsumerAccount account = consumerAccountRequest.account();
        String currentCRMKey = account.data().crmKey();
        if (currentCRMKey != null) {
            return account;
        } else {
            String crmKey = UUID.randomUUID().toString();

            return account.toBuilder()
                    .data(
                            account.data().toBuilder().crmKey(crmKey).build()
                    ).build();
        }
    }
}
