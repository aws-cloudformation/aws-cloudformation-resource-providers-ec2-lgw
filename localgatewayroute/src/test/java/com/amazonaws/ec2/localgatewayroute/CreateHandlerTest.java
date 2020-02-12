package com.amazonaws.ec2.localgatewayroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final SearchLocalGatewayRoutesResponse activeSearchLgwRoutesResponse = SearchLocalGatewayRoutesResponse
        .builder()
        .routes(LocalGatewayRoute.builder().destinationCidrBlock(DESTINATION_CIDR)
            .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
            .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
            .state(LocalGatewayRouteState.ACTIVE)
            .type(LocalGatewayRouteType.STATIC)
            .build())
        .build();

    private final SearchLocalGatewayRoutesResponse deletingSearchLgwRoutesResponse = SearchLocalGatewayRoutesResponse
        .builder()
        .routes(LocalGatewayRoute.builder().destinationCidrBlock(DESTINATION_CIDR)
            .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
            .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
            .state(LocalGatewayRouteState.DELETING)
            .type(LocalGatewayRouteType.STATIC)
            .build())
        .build();

    final ResourceModel startingModel = ResourceModel.builder()
        .destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
        .build();

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_CreateNotStarted_Success() {
        request.setDesiredResourceState(startingModel);

        // first search call returns no route, confirms it doesnt exist, then create is called which will create the
        // route, and then the second search will return the newly created route
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(emptyLocalGatewayRoutesResponse, activeSearchLgwRoutesResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any()))
            .thenReturn(activeSearchLgwRoutesResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(activeModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RouteAlreadyExists_Fails() {
        request.setDesiredResourceState(startingModel);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(activeSearchLgwRoutesResponse);

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handleRequest_CreateStarted_Success() {
        request.setDesiredResourceState(pendingModel);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(activeSearchLgwRoutesResponse);

        final CallbackContext inProgressContext = CallbackContext.builder()
            .createStarted(true)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(activeModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStarted_InProgress() {
        request.setDesiredResourceState(pendingModel);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(pendingSearchLgwRoutesResponse);

        final CallbackContext inProgressContext = CallbackContext.builder()
            .createStarted(true)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(CallbackContext.POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(pendingModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStarted_Failed() {
        request.setDesiredResourceState(activeModel);

        final CreateHandler handler = new CreateHandler();

        //create had started but somehow not in active or pending state
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(deletingSearchLgwRoutesResponse);

        final CallbackContext inProgressContext = CallbackContext.builder()
            .createStarted(true)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(activeModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RoutePending_Fails() {
        request.setDesiredResourceState(startingModel);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(pendingSearchLgwRoutesResponse);

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handleRequest_RouteDeleting_Fails() {
        request.setDesiredResourceState(startingModel);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(deletingSearchLgwRoutesResponse);

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));

    }

}
