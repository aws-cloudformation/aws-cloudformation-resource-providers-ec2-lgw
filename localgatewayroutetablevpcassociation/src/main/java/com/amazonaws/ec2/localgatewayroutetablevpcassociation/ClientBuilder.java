package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.proxy.Logger;

import java.time.Duration;

class ClientBuilder {
    private ClientBuilder() {
    }

    private static Duration SOCKET_TIMEOUT = Duration.ofSeconds(55);

    private static SdkHttpClient httpClient = ApacheHttpClient.builder()
        .socketTimeout(SOCKET_TIMEOUT)
        .build();

    static Ec2Client getClient(Logger logger) {
        return Ec2Client.builder()
            .httpClient(httpClient)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .addExecutionInterceptor(new AttemptLoggingExecutionInterceptor(logger))
                // CloudFormation handler times out after 1 minute. There are instances of these apis taking a long time
                .apiCallAttemptTimeout(SOCKET_TIMEOUT)
                .apiCallTimeout(Duration.ofSeconds(59))
                .retryPolicy(RetryPolicy.builder().numRetries(5).build())
                .build())
            .build();
    }
}
