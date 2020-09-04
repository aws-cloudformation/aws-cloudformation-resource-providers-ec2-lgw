package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.cloudformation.proxy.Logger;

import java.time.Duration;

class ClientBuilder {
    private ClientBuilder() {
    }

    static Ec2Client getClient(Logger logger) {
        return Ec2Client.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .addExecutionInterceptor(new AttemptLoggingExecutionInterceptor(logger))
                // CloudFormation handler times out after 1 minute. There are instances of these apis  taking a long time
                .apiCallAttemptTimeout(Duration.ofSeconds(55))
                .apiCallTimeout(Duration.ofSeconds(59))
                .retryPolicy(RetryPolicy.builder().numRetries(5).build())
                .build())
            .build();
    }
}
