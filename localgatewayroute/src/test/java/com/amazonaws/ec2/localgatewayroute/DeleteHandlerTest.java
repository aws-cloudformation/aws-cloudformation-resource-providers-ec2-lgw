package com.amazonaws.ec2.localgatewayroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.DeleteLocalGatewayRouteRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesResponse;
import software.amazon.cloudformation.proxy.*;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.ec2.localgatewayroute.CallbackContext.POLLING_DELAY_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(ACTIVE_VIF_GROUP_MODEL)
        .build();

    private final ResourceHandlerRequest<ResourceModel> requestForEniRoute = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(ACTIVE_ENI_MODEL)
        .build();

    private final CallbackContext inProgressContext = CallbackContext.builder()
        .deleteStarted(true)
        .build();


    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_DeleteNotStarted_Success() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RouteTableIdNotFound() {
        Map<ResourceHandlerRequest<ResourceModel>, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceHandlerRequest<ResourceModel>, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(request, EMPTY_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(requestForEniRoute, EMPTY_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidLocalGatewayRouteTableID.NotFound")
                .build();

            final Ec2Exception notFoundException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

            when(proxy.injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any()))
                .thenThrow(notFoundException);

            final DeleteHandler handler = new DeleteHandler();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, model, null, logger);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(model.getDesiredResourceState());
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        });
    }


    @Test
    public void handleRequest_DestinationCidrNotFound() {
        Map<ResourceHandlerRequest<ResourceModel>, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceHandlerRequest<ResourceModel>, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(request, EMPTY_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(requestForEniRoute, EMPTY_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidRoute.NotFound")
                .build();

            final Ec2Exception notFoundException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

            when(proxy.injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any()))
                .thenThrow(notFoundException);

            final DeleteHandler handler = new DeleteHandler();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, model, null, logger);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(model.getDesiredResourceState());
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        });
    }

    @Test
    public void handleRequest_InProgress() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(ACTIVE_VIF_GROUP_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteStarted_Success() {
        Map<ResourceHandlerRequest<ResourceModel>, SearchLocalGatewayRoutesResponse> modelResourceMap = new HashMap<ResourceHandlerRequest<ResourceModel>, SearchLocalGatewayRoutesResponse>();
        modelResourceMap.put(request, EMPTY_SEARCH_LGW_ROUTES_RESPONSE);
        modelResourceMap.put(requestForEniRoute, EMPTY_SEARCH_LGW_ROUTES_RESPONSE);

        modelResourceMap.forEach((model, searchRoutesResponse) -> {
            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenReturn(searchRoutesResponse);

            final DeleteHandler handler = new DeleteHandler();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, model, inProgressContext, logger);

            verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isNull();
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        });
    }

    @Test
    public void handleRequest_DeleteStartedReadFails_Failed() {
        Map<ResourceHandlerRequest<ResourceModel>, ResourceModel> modelResourceMap = new HashMap<ResourceHandlerRequest<ResourceModel>, ResourceModel>();
        modelResourceMap.put(request, ACTIVE_VIF_GROUP_MODEL);
        modelResourceMap.put(requestForEniRoute, ACTIVE_ENI_MODEL);

        modelResourceMap.forEach((modelRequest, expectedModelResponse) -> {
            final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnexpectedError")
                .build();

            final Ec2Exception unexpectedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

            Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenThrow(unexpectedException);

            final DeleteHandler handler = new DeleteHandler();

            final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, modelRequest, inProgressContext, logger);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(expectedModelResponse);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
        });
    }

    @Test
    public void handleRequest_EniRouteDeleteNotStarted_Success() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(EMPTY_SEARCH_LGW_ROUTES_RESPONSE);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, requestForEniRoute, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleEniRouteRequest_InProgress() {
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, requestForEniRoute, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(ACTIVE_ENI_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
