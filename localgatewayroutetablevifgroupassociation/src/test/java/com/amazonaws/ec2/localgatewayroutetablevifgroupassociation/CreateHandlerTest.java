package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVpcAssociationRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.CallbackContext.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator.createModelFromVifGroupAssociation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final ResourceModel model = ResourceModel.builder()
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
            .build();

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

    private final ResourceHandlerRequest<ResourceModel> requestAfterRouteTableCreated = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(createModelFromVifGroupAssociation(TEST_ASSOCIATION))
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
    public void handleRequest_CreateNotStarted_InProgress() {
        final CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse createResponse = CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociation(TEST_ASSOCIATION)
                .build();

        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(TEST_ASSOCIATION)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any()))
                .thenReturn(createResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromVifGroupAssociation(TEST_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateWithTagsNotStarted_InProgress() {
        final CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse createResponse = CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociation(TEST_ASSOCIATION_WITH_TAGS)
                .build();

        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(TEST_ASSOCIATION_WITH_TAGS)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any()))
                .thenReturn(createResponse);
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        // Test that both types of tags get added
        ResourceModel modelWithTags = ResourceModel.builder()
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
                .tags(TagHelper.createCfnTagsFromSdkTags(new HashSet<>(TEST_ASSOCIATION_WITH_TAGS.tags())))
                .build();
        Map<String, String> stackTags = Collections.singletonMap("stackTagKey", "stackTagValue");

        ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelWithTags)
                .desiredResourceTags(stackTags)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestWithTags, null, logger);

        Set<software.amazon.awssdk.services.ec2.model.Tag> expectedTags = TagHelper.createSdkTagsFromCfnTags(modelWithTags.getTags());
        expectedTags.addAll(TagHelper.createSdkTagsFromCfnTags(TagHelper.tagsFromCfnRequestTags(stackTags)));

        CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest expectedRequest = CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.builder()
                .localGatewayRouteTableId(TEST_ASSOCIATION_WITH_TAGS.localGatewayRouteTableId())
                .localGatewayVirtualInterfaceGroupId(TEST_ASSOCIATION_WITH_TAGS.localGatewayVirtualInterfaceGroupId())
                .tagSpecifications(TagSpecification.builder()
                        .resourceType("local-gateway-route-table-virtual-interface-group-association")
                        .tags(expectedTags)
                        .build())
                .build();

        verify(proxy)
                .injectCredentialsAndInvokeV2(eq(expectedRequest), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromVifGroupAssociation(TEST_ASSOCIATION_WITH_TAGS));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateNotStarted_Failed() {
        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnexpectedError")
                .build();

        final Ec2Exception unexpectedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any()))
                .thenThrow(unexpectedException);

        final CreateHandler handler = new CreateHandler();

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
    public void handleRequest_CreateWithTagsStarted_Success() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(TEST_ASSOCIATION_WITH_TAGS)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final Set<Tag> tagSet = new HashSet<>();
        tagSet.add(Tag.builder().key("Name").value("MyAssociation").build());
        tagSet.add(Tag.builder().key("Stage").value("Prod").build());

        final ResourceModel modelWithTags = ResourceModel.builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociationId(VIF_GROUP_ASSOCIATION_ID)
                .tags(tagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelWithTags)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestWithTags, inProgressContext, logger);

        verify(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromVifGroupAssociation(TEST_ASSOCIATION_WITH_TAGS));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStarted_Success() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(TEST_ASSOCIATION)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromVifGroupAssociation(TEST_ASSOCIATION));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateStarted_Failed() {
        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnexpectedError")
                .build();

        final Ec2Exception unexpectedException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenThrow(unexpectedException);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
                .injectCredentialsAndInvokeV2(any(CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.class), any());

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
    public void handleRequest_VifGroupAssociationNotFound_InProgress() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(Collections.emptyList())
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestAfterRouteTableCreated, inProgressContext, logger);

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
    public void handleRequest_VifGroupAssociationPending_InProgress() {
        final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse describeResponse = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociations(TEST_ASSOCIATION_PENDING)
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest.class), any()))
                .thenReturn(describeResponse);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestAfterRouteTableCreated, inProgressContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromVifGroupAssociation(TEST_ASSOCIATION_PENDING));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InvalidRequest_Failed() {
        ResourceModel invalidModel = ResourceModel.builder()
                .state("invalid")
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
                .build();
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
        assertThat(response.getMessage()).isEqualTo("Cannot set read-only property State");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
