package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.CallbackContext.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator.createModelFromVifGroupAssociation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final ResourceModel model = createModelFromVifGroupAssociation(TEST_ASSOCIATION);

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
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
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(Collections.emptyList())
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
                .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any());

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
    public void handleRequest_DeleteNotStarted_Failed() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(Collections.emptyList())
                .build();

        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnexpectedError")
                .build();

        final Ec2Exception unexpectedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any()))
                .thenThrow(unexpectedException);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_NotFound() {

        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidLocalGatewayRouteTableVirtualInterfaceGroupAssociationId.NotFound")
                .build();

        final Ec2Exception notFoundException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any()))
                .thenThrow(notFoundException);

        final DeleteHandler handler = new DeleteHandler();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InProgress() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(TEST_ASSOCIATION)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
                .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromVifGroupAssociation(TEST_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteStarted_Success() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(Collections.emptyList())
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any());

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
    public void handleRequest_DeleteStarted_Failed() {
        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnauthorizedOperation")
                .build();

        final Ec2Exception unauthorizedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenThrow(unauthorizedException);

        final DeleteHandler handler = new DeleteHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

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
