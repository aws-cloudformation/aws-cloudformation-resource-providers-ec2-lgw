package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.amazonaws.ec2.localgatewayroute.Translator.createModelFromRoute;
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
        .destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
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

        final SearchLocalGatewayRoutesResponse responseWithToken = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(Collections.emptyList())
            .nextToken("token")
            .build();

        final SearchLocalGatewayRoutesResponse responseWithAssociation = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(ACTIVE_VIF_GROUP_ROUTE)
            .build();

        when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(responseWithToken)
            .thenReturn(responseWithAssociation);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromRoute(ACTIVE_VIF_GROUP_ROUTE));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RouteNotFound_Fails() {
        final ReadHandler handler = new ReadHandler();

        final SearchLocalGatewayRoutesResponse response = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(Collections.emptyList())
            .build();

        when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(response);

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_MultipleRoutesFound_Fails() {
        final ReadHandler handler = new ReadHandler();

        final SearchLocalGatewayRoutesResponse response = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(ACTIVE_VIF_GROUP_ROUTE, ACTIVE_ENI_ROUTE)
            .build();

        when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(response);

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
