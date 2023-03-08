package com.amazonaws.ec2.localgatewayroutetable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableMode;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetable.Translator.createModelFromRouteTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        final DescribeLocalGatewayRouteTablesResponse describeResponse = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(TEST_ROUTE_TABLE)
                .build();
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTablesRequest.class), any()))
                .thenReturn(describeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(createModelFromRouteTable(TEST_ROUTE_TABLE))
                .build();

        final UpdateHandler handler = new UpdateHandler();
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
    public void handleRequest_UpdateCreateOnlyProperties_Fails() {
        final DescribeLocalGatewayRouteTablesResponse describeResponse = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(TEST_ROUTE_TABLE)
                .build();
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTablesRequest.class), any()))
                .thenReturn(describeResponse);

        Map<ResourceModel, String> testCases = new HashMap<ResourceModel, String>() {{
            put(ResourceModel.builder().localGatewayRouteTableId("invalid").build(), "LocalGatewayRouteTableId");
            put(ResourceModel.builder().localGatewayId("invalid").build(), "LocalGatewayId");
            put(ResourceModel.builder().localGatewayRouteTableArn("invalid").build(), "LocalGatewayRouteTableArn");
            put(ResourceModel.builder().outpostArn("invalid").build(), "OutpostArn");
            put(ResourceModel.builder().mode("invalid").build(), "Mode");
            put(ResourceModel.builder().ownerId("invalid").build(), "OwnerId");
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
    public void handleRequest_TagUpdateNotStarted_InProgress() {
        final DescribeLocalGatewayRouteTablesResponse describeResponse = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(TEST_ROUTE_TABLE_WITH_TAGS)
                .build();
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTablesRequest.class), any()))
                .thenReturn(describeResponse);

        // Set a single tag on each of the places tags can be applied: resource-level and stack-level
        Map<String, String> stackTags = Collections.singletonMap("Stage", "Test");
        Tag resourceLevelTag1 = Tag.builder().key("Name").value("MyRouteTable").build();
        Tag resourceLevelTag2 = Tag.builder().key("NewKey").value("NewValue").build();

        final ResourceModel model = ResourceModel
                .builder()
                .localGatewayId(LOCAL_GATEWAY_ID)
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .mode(LocalGatewayRouteTableMode.COIP.toString())
                .tags(new HashSet<>(Arrays.asList(resourceLevelTag1, resourceLevelTag2)))
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(stackTags)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final Set<Tag> expectedTagsToCreate = new HashSet<>();
        expectedTagsToCreate.add(Tag.builder().key("Stage").value("Test").build());
        expectedTagsToCreate.add(Tag.builder().key("NewKey").value("NewValue").build());
        final Set<Tag> expectedTagsToDelete = new HashSet<>();
        expectedTagsToDelete.add(Tag.builder().key("Stage").value("Prod").build());


        final CallbackContext expectedContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(expectedTagsToCreate)
                .tagsToDelete(expectedTagsToDelete)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(expectedContext);
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
        final Set<Tag> oldTags = new HashSet<>();
        oldTags.add(Tag.builder().key("ThisIsOld").value("OldValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .localGatewayId(LOCAL_GATEWAY_ID)
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .mode(LocalGatewayRouteTableMode.COIP.toString())
                .tags(newTags)
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(newTags)
                .tagsToDelete(oldTags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        final CreateTagsRequest expectedCreateTagsRequest = CreateTagsRequest
                .builder()
                .resources(ROUTE_TABLE_ID)
                .tags(software.amazon.awssdk.services.ec2.model.Tag.builder().key("ThisIsNew").value("NewValue").build())
                .build();

        final DeleteTagsRequest expectedDeleteTagsRequest = DeleteTagsRequest
                .builder()
                .resources(ROUTE_TABLE_ID)
                .tags(software.amazon.awssdk.services.ec2.model.Tag.builder().key("ThisIsOld").value("OldValue").build())
                .build();

        verify(proxy)
                .injectCredentialsAndInvokeV2(eq(expectedCreateTagsRequest), any());
        verify(proxy)
                .injectCredentialsAndInvokeV2(eq(expectedDeleteTagsRequest), any());
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
    public void handleRequest_UpdateStarted_NoTagsToDelete_Success() {
        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .localGatewayId(LOCAL_GATEWAY_ID)
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .mode(LocalGatewayRouteTableMode.COIP.toString())
                .tags(newTags)
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(newTags)
                .tagsToDelete(Collections.emptySet())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        final CreateTagsRequest expectedCreateTagsRequest = CreateTagsRequest
                .builder()
                .resources(ROUTE_TABLE_ID)
                .tags(software.amazon.awssdk.services.ec2.model.Tag.builder().key("ThisIsNew").value("NewValue").build())
                .build();

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

    @Test
    public void handleRequest_UpdateStarted_NoTagsToCreate_Success() {
        final Set<Tag> oldTags = new HashSet<>();
        oldTags.add(Tag.builder().key("ThisIsOld").value("OldValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .localGatewayId(LOCAL_GATEWAY_ID)
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .mode(LocalGatewayRouteTableMode.COIP.toString())
                .tags(Collections.emptySet())
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(Collections.emptySet())
                .tagsToDelete(oldTags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        final DeleteTagsRequest expectedDeleteTagsRequest = DeleteTagsRequest
                .builder()
                .resources(ROUTE_TABLE_ID)
                .tags(software.amazon.awssdk.services.ec2.model.Tag.builder().key("ThisIsOld").value("OldValue").build())
                .build();

        verify(proxy)
                .injectCredentialsAndInvokeV2(eq(expectedDeleteTagsRequest), any());
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
    public void handleRequest_ErrorWhileCreatingTags_Fails() {
        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .localGatewayId(LOCAL_GATEWAY_ID)
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .mode(LocalGatewayRouteTableMode.COIP.toString())
                .tags(newTags)
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(newTags)
                .tagsToDelete(Collections.emptySet())
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
        when(proxy.injectCredentialsAndInvokeV2(any(CreateTagsRequest.class), any())).thenThrow(unauthorizedException);

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

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
