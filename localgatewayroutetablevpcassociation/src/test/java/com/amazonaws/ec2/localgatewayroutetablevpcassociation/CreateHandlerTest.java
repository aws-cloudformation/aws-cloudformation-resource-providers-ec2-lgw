package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVpcAssociationRequest;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVpcAssociationResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
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
import java.util.HashSet;
import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.createModelFromAssociation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final ResourceModel model = ResourceModel.builder()
        .vpcId(VPC_ID)
        .localGatewayRouteTableId(ROUTE_TABLE_ID)
        .build();

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    private final CallbackContext inProgressContext = CallbackContext.builder()
        .createStarted(true)
        .build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_CreateNotStarted_Success() {
        final CreateLocalGatewayRouteTableVpcAssociationResponse createResponse = CreateLocalGatewayRouteTableVpcAssociationResponse
            .builder()
            .localGatewayRouteTableVpcAssociation(TEST_ASSOCIATION)
            .build();

        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION)
            .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any()))
            .thenReturn(createResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());
        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateTagsRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromAssociation(TEST_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateWithTagsNotStarted_Success() {
        final CreateLocalGatewayRouteTableVpcAssociationResponse createResponse = CreateLocalGatewayRouteTableVpcAssociationResponse
            .builder()
            .localGatewayRouteTableVpcAssociation(TEST_ASSOCIATION_WITH_TAGS)
            .build();

        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION_WITH_TAGS)
            .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any()))
            .thenReturn(createResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final Set<Tag> tagSet = new HashSet<>();
        tagSet.add(Tag.builder().key("Name").value("MyAssociation").build());
        tagSet.add(Tag.builder().key("Stage").value("Prod").build());

        final ResourceModel modelWithTags = ResourceModel.builder()
            .vpcId(VPC_ID)
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .tags(tagSet)
            .build();
        final ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(modelWithTags)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, requestWithTags, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());
        verify(proxy)
            .injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any());
        final CreateTagsRequest expectedCreateTagsRequest = CreateTagsRequest
            .builder()
            .tags(TEST_ASSOCIATION_WITH_TAGS.tags())
            .resources(TEST_ASSOCIATION_WITH_TAGS.localGatewayRouteTableVpcAssociationId())
            .build();

        verify(proxy)
            .injectCredentialsAndInvokeV2(eq(expectedCreateTagsRequest), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromAssociation(TEST_ASSOCIATION_WITH_TAGS));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AssociationAlreadyExists_Fails() {
        final CreateHandler handler = new CreateHandler();

        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
            .errorCode("LocalGatewayRouteTableVpcAssociationAlreadyExists")
            .build();

        final Ec2Exception alreadyExistsException = (Ec2Exception) Ec2Exception
            .builder()
            .awsErrorDetails(errorDetails)
            .build();

        when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any()))
            .thenThrow(alreadyExistsException);

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CreateStarted_Success() {
        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION)
            .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromAssociation(TEST_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AssociationNotFound_InProgress() {
        final CreateLocalGatewayRouteTableVpcAssociationResponse createResponse = CreateLocalGatewayRouteTableVpcAssociationResponse
            .builder()
            .localGatewayRouteTableVpcAssociation(TEST_ASSOCIATION)
            .build();

        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(Collections.emptyList())
            .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any()))
            .thenReturn(createResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromAssociation(TEST_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AssociationPending_InProgress() {
        final CreateLocalGatewayRouteTableVpcAssociationResponse createResponse = CreateLocalGatewayRouteTableVpcAssociationResponse
            .builder()
            .localGatewayRouteTableVpcAssociation(TEST_ASSOCIATION)
            .build();

        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(PENDING_ASSOCIATION)
            .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any()))
            .thenReturn(createResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
            .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVpcAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromAssociation(PENDING_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
