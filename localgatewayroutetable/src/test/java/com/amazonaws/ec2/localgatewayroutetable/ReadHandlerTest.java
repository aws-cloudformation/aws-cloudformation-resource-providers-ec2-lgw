package com.amazonaws.ec2.localgatewayroutetable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableMode;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static com.amazonaws.ec2.localgatewayroutetable.Translator.createModelFromRouteTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel model = ResourceModel.builder()
            .localGatewayId(LOCAL_GATEWAY_ID)
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .mode(LocalGatewayRouteTableMode.COIP.toString())
            .build();

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();

        final DescribeLocalGatewayRouteTablesResponse responseWithToken = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(Collections.emptyList())
                .nextToken("token")
                .build();

        final DescribeLocalGatewayRouteTablesResponse responseWithRouteTable = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(TEST_ROUTE_TABLE)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(responseWithToken)
                .thenReturn(responseWithRouteTable);


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromRouteTable(TEST_ROUTE_TABLE));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NotFound_Fails() {
        final ReadHandler handler = new ReadHandler();

        final DescribeLocalGatewayRouteTablesResponse response = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(Collections.emptyList())
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(response);

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_MultipleRouteTables_Fails() {
        final ReadHandler handler = new ReadHandler();

        final DescribeLocalGatewayRouteTablesResponse response = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(TEST_ROUTE_TABLE, TEST_ROUTE_TABLE)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(response);

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
