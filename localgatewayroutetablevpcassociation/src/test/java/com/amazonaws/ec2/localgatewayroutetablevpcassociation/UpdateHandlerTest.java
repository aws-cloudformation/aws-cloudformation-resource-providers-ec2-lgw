package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.createModelFromAssociation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION)
            .build();
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(createModelFromAssociation(TEST_ASSOCIATION))
            .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

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
    public void handleRequest_UpdateCreateOnlyProperties_Fails() {
        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION)
            .build();
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final ResourceModel model = ResourceModel
            .builder()
            .localGatewayRouteTableId("lgw-rtb-09876543210987654")
            .vpcId("vpc-09876543210987654")
            .localGatewayRouteTableVpcAssociationId(ASSOCIATION_ID)
            .build();
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
        assertThat(response.getMessage()).isEqualTo("Cannot update not updatable properties LocalGatewayRouteTableId or VpcId");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
    }

    @Test
    public void handleRequest_TagUpdateNotStarted_InProgress() {
        final DescribeLocalGatewayRouteTableVpcAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION)
            .build();
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVpcAssociationsRequest.class), any()))
            .thenReturn(describeResponse);

        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final ResourceModel model = ResourceModel
            .builder()
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .vpcId(VPC_ID)
            .localGatewayRouteTableVpcAssociationId(ASSOCIATION_ID)
            .tags(newTags)
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CallbackContext.builder().updateStarted(true).build());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateStarted_Success() {
        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final ResourceModel model = ResourceModel
            .builder()
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .vpcId(VPC_ID)
            .localGatewayRouteTableVpcAssociationId(ASSOCIATION_ID)
            .tags(newTags)
            .build();
        final CallbackContext callbackContext = CallbackContext.builder().updateStarted(true).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, callbackContext, logger);

        final DeleteTagsRequest expectedDeleteTagsRequest = DeleteTagsRequest
            .builder()
            .resources(ASSOCIATION_ID)
            .build();
        final CreateTagsRequest expectedCreateTagsRequest = CreateTagsRequest
            .builder()
            .resources(ASSOCIATION_ID)
            .tags(software.amazon.awssdk.services.ec2.model.Tag.builder().key("ThisIsNew").value("NewValue").build())
            .build();

        verify(proxy)
            .injectCredentialsAndInvokeV2(eq(expectedDeleteTagsRequest), any());
        verify(proxy)
            .injectCredentialsAndInvokeV2(eq(expectedCreateTagsRequest), any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
