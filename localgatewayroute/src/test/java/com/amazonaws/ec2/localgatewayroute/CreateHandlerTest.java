package com.amazonaws.ec2.localgatewayroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;

import java.util.HashMap;
import java.util.Map;

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

    private final CreateLocalGatewayRouteResponse CREATE_LGW_ROUTE_TO_VIF_GROUP_RESPONSE = CreateLocalGatewayRouteResponse
            .builder()
            .route(ACTIVE_VIF_GROUP_ROUTE)
            .build();

    private final CreateLocalGatewayRouteResponse CREATE_LGW_ROUTE_TO_ENI_RESPONSE = CreateLocalGatewayRouteResponse
            .builder()
            .route(ACTIVE_ENI_ROUTE)
            .build();

    final ResourceModel STARTING_VIF_GROUP_ROUTE_MODEL = ResourceModel.builder()
        .destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
        .build();

    final ResourceModel STARTING_ENI_ROUTE_MODEL = ResourceModel.builder()
        .destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID_3)
        .networkInterfaceId(NETWORK_INTERFACE_ID)
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
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(ACTIVE_VIF_GROUP_MODEL, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(BLACKHOLE_VIF_GROUP_MODEL, BLACKHOLE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            request.setDesiredResourceState(STARTING_VIF_GROUP_ROUTE_MODEL);

            // first search call returns no route, confirms it doesnt exist, then create is called which will create the
            // route, and then the second search will return the newly created route
            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE, searchRoutesResponse);
            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any()))
                .thenReturn(CREATE_LGW_ROUTE_TO_VIF_GROUP_RESPONSE);

            final CreateHandler handler = new CreateHandler();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(model);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        });
        verify(proxy, times(modelResourceMap.size()))
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());
    }

    @Test
    public void handleRequest_RouteAlreadyExists_Fails() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(STARTING_VIF_GROUP_ROUTE_MODEL, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_VIF_GROUP_ROUTE_MODEL, BLACKHOLE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_ENI_ROUTE_MODEL, ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_ENI_ROUTE_MODEL, BLACKHOLE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            request.setDesiredResourceState(model);
            final CreateHandler handler = new CreateHandler();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(searchRoutesResponse);

            assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
        });
    }

    @Test
    public void handleRequest_CreateRouteThrowsError_Fails() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(STARTING_VIF_GROUP_ROUTE_MODEL, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_VIF_GROUP_ROUTE_MODEL, BLACKHOLE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_ENI_ROUTE_MODEL, ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_ENI_ROUTE_MODEL, BLACKHOLE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidParameter")
                .build();

            final Ec2Exception invalidParamException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE, searchRoutesResponse);
            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any()))
                .thenThrow(invalidParamException);

            request.setDesiredResourceState(model);

            final CreateHandler handler = new CreateHandler();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isNull();
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        });
    }

    @Test
    public void handleRequest_CreateStarted_Success() {
        request.setDesiredResourceState(PENDING_VIF_GROUP_MODEL);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        final CallbackContext inProgressContext = CallbackContext.builder()
            .createStarted(true)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(ACTIVE_VIF_GROUP_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStarted_InProgress() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(PENDING_VIF_GROUP_MODEL, PENDING_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(PENDING_ENI_MODEL, PENDING_ENI_SERACH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            request.setDesiredResourceState(model);
            final CreateHandler handler = new CreateHandler();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(searchRoutesResponse);

            final CallbackContext inProgressContext = CallbackContext.builder()
                .createStarted(true)
                .build();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

            verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
            assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(CallbackContext.POLLING_DELAY_SECONDS);
            assertThat(response.getResourceModel()).isEqualTo(model);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        });
    }

    @Test
    public void handleRequest_CreateStarted_Failed() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(ACTIVE_VIF_GROUP_MODEL, DELETING_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(ACTIVE_ENI_MODEL, DELETING_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            request.setDesiredResourceState(model);

            final CreateHandler handler = new CreateHandler();

            //create had started but somehow not in active or pending state
            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(searchRoutesResponse);

            final CallbackContext inProgressContext = CallbackContext.builder()
                .createStarted(true)
                .build();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

            verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(model);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        });
    }

    @Test
    public void handleRequest_CreateStarted_ReadFails_Failed() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(ACTIVE_VIF_GROUP_MODEL, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(ACTIVE_VIF_GROUP_MODEL, BLACKHOLE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(ACTIVE_ENI_MODEL, ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(ACTIVE_ENI_MODEL, BLACKHOLE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnauthorizedOperation")
                .build();

            final Ec2Exception unauthorizedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

            request.setDesiredResourceState(model);

            final CreateHandler handler = new CreateHandler();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenThrow(unauthorizedException);

            final CallbackContext inProgressContext = CallbackContext.builder()
                .createStarted(true)
                .build();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

            verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(model);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        });
    }

    @Test
    public void handleRequest_RoutePending_Fails() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(STARTING_VIF_GROUP_ROUTE_MODEL, PENDING_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_ENI_ROUTE_MODEL, PENDING_ENI_SERACH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            request.setDesiredResourceState(model);

            final CreateHandler handler = new CreateHandler();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(searchRoutesResponse);

            assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
        });
    }

    @Test
    public void handleRequest_RouteDeleting_Fails() {
        Map<ResourceModel, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceModel, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(STARTING_VIF_GROUP_ROUTE_MODEL, DELETING_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(STARTING_ENI_ROUTE_MODEL, DELETING_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            request.setDesiredResourceState(model);

            final CreateHandler handler = new CreateHandler();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(searchRoutesResponse);

            assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
        });
    }

    @Test
    public void handleRequest_CreateRouteThrowsErrorIfBothLgwVifGrpIfAndNetworkInterfaceIdPassed_Fails() {
        request.setDesiredResourceState(ResourceModel.builder()
                .destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
                .networkInterfaceId(NETWORK_INTERFACE_ID)
                .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
                .build());

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CreateRouteThrowsErrorIfNeitherLgwVifGrpIfNorNetworkInterfaceIdPassed_Fails() {
        request.setDesiredResourceState(ResourceModel.builder()
                .destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
                .build());

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CreateEniRouteNotStarted_Success() {
        request.setDesiredResourceState(STARTING_ENI_ROUTE_MODEL);

        // first search call returns no route, confirms it doesnt exist, then create is called which will create the
        // route, and then the second search will return the newly created route
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE, ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any()))
            .thenReturn(CREATE_LGW_ROUTE_TO_ENI_RESPONSE);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(ACTIVE_ENI_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateEniRouteStarted_Success() {
        request.setDesiredResourceState(PENDING_ENI_MODEL);

        final CreateHandler handler = new CreateHandler();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        final CallbackContext inProgressContext = CallbackContext.builder()
            .createStarted(true)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(ACTIVE_ENI_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    private void testReadOnlyProperty(ResourceModel invalidModel, String propertyName) {
        request.setDesiredResourceState(invalidModel);
        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("Cannot set read-only property " + propertyName);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_InvalidRequestSetReadOnlyProperty_Failed() {
        ResourceModel.ResourceModelBuilder invalidModel = ResourceModel.builder()
                .destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
                .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID);
        testReadOnlyProperty(invalidModel.state("invalid").build(), "State");
        testReadOnlyProperty(invalidModel.type("invalid").build(), "Type");
    }
}
