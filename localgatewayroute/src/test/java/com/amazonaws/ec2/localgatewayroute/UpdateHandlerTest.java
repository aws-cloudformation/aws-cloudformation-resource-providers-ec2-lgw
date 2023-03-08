package com.amazonaws.ec2.localgatewayroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyLocalGatewayRouteRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateHandlerTest extends TestBase {

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
    public void handleRequest_NoChanges_Success() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ACTIVE_VIF_GROUP_MODEL)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

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
    public void handleRequest_UpdateCreateOnlyProperties_Fails() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        Map<ResourceModel, String> testCases = new HashMap<ResourceModel, String>() {{
            put(ResourceModel.builder().localGatewayRouteTableId("invalid").build(), "LocalGatewayRouteTableId");
            put(ResourceModel.builder().destinationCidrBlock("invalid").build(), "DestinationCidrBlock");
            put(ResourceModel.builder().type("invalid").build(), "Type");
            put(ResourceModel.builder().state("invalid").build(), "State");
        }};

        testCases.forEach((model, propertyName) -> {
            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                    .desiredResourceState(model)
                    .build();
            final UpdateHandler handler = new UpdateHandler();
            final ProgressEvent<ResourceModel, CallbackContext> response
                    = handler.handleRequest(proxy, request, null, logger);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(model);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isEqualTo("Cannot update not updatable property " + propertyName);
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
        });
    }

    @Test
    public void handleRequest_UpdateRoute_Success() {
        // Testing all possible route updates:
        // 1. eni-a -> eni-b
        // 2. eni -> vif-group
        // 3. vif-group-a -> vif-group-b
        // 4. vif-group -> eni
        Map<String, SearchLocalGatewayRoutesResponse> routeTargetToSearchRoutesResponseMap = new HashMap<>();
        routeTargetToSearchRoutesResponseMap.put(NETWORK_INTERFACE_ID_2, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        routeTargetToSearchRoutesResponseMap.put(NETWORK_INTERFACE_ID_2, ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);
        routeTargetToSearchRoutesResponseMap.put(LOCAL_GATEWAY_VIF_GROUP_ID_2, ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);
        routeTargetToSearchRoutesResponseMap.put(LOCAL_GATEWAY_VIF_GROUP_ID_2, ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        routeTargetToSearchRoutesResponseMap.forEach((routeTarget, searchRoutesResponse) -> {
            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                    .thenReturn(searchRoutesResponse);

            String lgwRtbId = searchRoutesResponse.routes().get(0).localGatewayRouteTableId();

            ResourceModel.ResourceModelBuilder desiredModelBuilder = ResourceModel.builder()
                    .localGatewayRouteTableId(lgwRtbId)
                    .destinationCidrBlock(DESTINATION_CIDR);

            if (routeTarget.startsWith("lgw-vif")) {
                desiredModelBuilder.localGatewayVirtualInterfaceGroupId(routeTarget);
            } else {
                desiredModelBuilder.networkInterfaceId(routeTarget);
            }

            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                    .desiredResourceState(desiredModelBuilder.build())
                    .build();

            final UpdateHandler handler = new UpdateHandler();
            final ProgressEvent<ResourceModel, CallbackContext> response
                    = handler.handleRequest(proxy, request, null, logger);

            final ModifyLocalGatewayRouteRequest.Builder expectedModifyRouteRequestBuilder = ModifyLocalGatewayRouteRequest
                    .builder()
                    .localGatewayRouteTableId(lgwRtbId)
                    .destinationCidrBlock(DESTINATION_CIDR);

            if (routeTarget.startsWith("lgw-vif")) {
                expectedModifyRouteRequestBuilder.localGatewayVirtualInterfaceGroupId(routeTarget);
            } else {
                expectedModifyRouteRequestBuilder.networkInterfaceId(routeTarget);
            }
            verify(proxy).injectCredentialsAndInvokeV2(eq(expectedModifyRouteRequestBuilder.build()), any());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(desiredModelBuilder.build());
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        });
    }

    @Test
    public void handleRequest_UpdateVifGroupRoute_Success() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        ResourceModel model = ResourceModel.builder()
                .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID_3)
                .destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final ModifyLocalGatewayRouteRequest expectedModifyRouteRequest = ModifyLocalGatewayRouteRequest
                .builder()
                .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID_3)
                .destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
                .build();

        verify(proxy).injectCredentialsAndInvokeV2(eq(expectedModifyRouteRequest), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ErrorWhileModifyingRoute_Fails() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        ResourceModel model = ResourceModel.builder()
                .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID_3)
                .destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnauthorizedOperation")
                .build();
        final Ec2Exception unauthorizedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(ModifyLocalGatewayRouteRequest.class), any())).thenThrow(unauthorizedException);

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
