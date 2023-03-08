package com.amazonaws.ec2.localgatewayroutetable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesRequest;
import software.amazon.cloudformation.proxy.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
public class AttemptLoggingExecutionInterceptorTest {

    @Mock
    Logger logger;

    @BeforeEach
    public void setup() {
        logger = mock(Logger.class);
    }

    @Test
    public void testAfterTransmission_NoError_NoLog() {
        SdkHttpResponse response = SdkHttpResponse
            .builder()
            .statusCode(200)
            .build();
        Context.AfterTransmission context = InterceptorContext
            .builder()
            .httpResponse(response)
            .request(DescribeLocalGatewayRouteTablesRequest.builder().build())
            .build();
        AttemptLoggingExecutionInterceptor interceptor = new AttemptLoggingExecutionInterceptor(logger);
        interceptor.afterTransmission(context, new ExecutionAttributes());

        verifyZeroInteractions(logger);
    }

    @Test
    public void testAfterTransmission_Error_Logs() {
        SdkHttpResponse response = SdkHttpResponse
            .builder()
            .statusCode(500)
            .statusText("Transmission failed")
            .build();
        Context.AfterTransmission context = InterceptorContext
            .builder()
            .httpResponse(response)
            .request(DescribeLocalGatewayRouteTablesRequest.builder().build())
            .build();
        AttemptLoggingExecutionInterceptor interceptor = new AttemptLoggingExecutionInterceptor(logger);
        interceptor.afterTransmission(context, new ExecutionAttributes());

        verify(logger).log("Error when attempting to call EC2. Status: 500, Transmission failed");
    }
}
