package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.LambdaWrapper;

class ClientBuilder {
    private ClientBuilder() {
    }

    static Ec2Client getClient() {
        return Ec2Client.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder().numRetries(5).build())
                .build())
            .build();
    }
}
