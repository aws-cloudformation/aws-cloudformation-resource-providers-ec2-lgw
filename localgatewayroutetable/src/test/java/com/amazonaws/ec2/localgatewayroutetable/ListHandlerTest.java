package com.amazonaws.ec2.localgatewayroutetable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

import static com.amazonaws.ec2.localgatewayroutetable.Translator.createModelFromRouteTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final DescribeLocalGatewayRouteTablesResponse responseWithToken = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(PENDING_ROUTE_TABLE)
                .nextToken("token")
                .build();

        final DescribeLocalGatewayRouteTablesResponse responseWithRouteTable = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(TEST_ROUTE_TABLE)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(responseWithToken)
                .thenReturn(responseWithRouteTable);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(
                Arrays.asList(
                        createModelFromRouteTable(PENDING_ROUTE_TABLE),
                        createModelFromRouteTable(TEST_ROUTE_TABLE)
                )
        );
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
