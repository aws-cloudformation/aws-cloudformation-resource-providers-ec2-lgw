package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.cloudformation.proxy.Logger;

public class AttemptLoggingExecutionInterceptor implements ExecutionInterceptor {

    private Logger logger;

    AttemptLoggingExecutionInterceptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        if (!context.httpResponse().isSuccessful()) {
            logger.log("Error when attempting to call EC2. Status: " + context.httpResponse().statusCode() +
                    ", " + context.httpResponse().statusText().orElse(""));
        }
    }
}
