package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.getHandlerErrorForEc2Error;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final Ec2Client client = ClientBuilder.getClient(logger);
        final ResourceModel model = request.getDesiredResourceState();

        if (callbackContext == null || !callbackContext.isUpdateStarted()) {
            final ReadHandler readHandler = new ReadHandler();
            final ResourceModel existingResource;
            try {
                existingResource = readHandler.handleRequest(proxy, request, callbackContext, logger).getResourceModel();
            } catch (Ec2Exception e) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                    .message(e.getMessage())
                    .build();
            }
            if (!existingResource.getLocalGatewayRouteTableId().equals(model.getLocalGatewayRouteTableId())
                || !existingResource.getVpcId().equals(model.getVpcId())) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .message("Cannot update not updatable properties LocalGatewayRouteTableId or VpcId")
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotUpdatable)
                    .build();
            }
            final Set<Tag> currentTags = existingResource.getTags();
            final Set<Tag> desiredTags = TagHelper.getAllResourceTags(request);
            if (currentTags.equals(desiredTags)) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
            }

            // To make this update minimally intrusive we only change tags that are not staying the same between updates
            final Set<Tag> tagsToCreate = desiredTags
                .stream()
                .filter(tag -> !currentTags.contains(tag))
                .collect(Collectors.toSet());
            final Set<Tag> tagsToDelete = currentTags
                .stream()
                .filter(tag -> !desiredTags.contains(tag))
                .collect(Collectors.toSet());

            final CallbackContext nextContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(tagsToCreate)
                .tagsToDelete(tagsToDelete)
                .build();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(nextContext)
                .resourceModel(model)
                .status(OperationStatus.IN_PROGRESS)
                .build();
        }

        // Create new tags before deleting old ones
        try {
            if (callbackContext.getTagsToCreate() != null && !callbackContext.getTagsToCreate().isEmpty()) {
                final CreateTagsRequest createTagsRequest = CreateTagsRequest
                    .builder()
                    .tags(callbackContext.getTagsToCreate().stream().map(TagHelper::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                    .resources(model.getLocalGatewayRouteTableVpcAssociationId())
                    .build();
                proxy.injectCredentialsAndInvokeV2(createTagsRequest, client::createTags);
            }
            if (callbackContext.getTagsToDelete() != null && !callbackContext.getTagsToDelete().isEmpty()) {
                final DeleteTagsRequest deleteTagsRequest = DeleteTagsRequest
                    .builder()
                    .tags(callbackContext.getTagsToDelete().stream().map(TagHelper::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                    .resources(model.getLocalGatewayRouteTableVpcAssociationId())
                    .build();
                proxy.injectCredentialsAndInvokeV2(deleteTagsRequest, client::deleteTags);
            }
        } catch (Ec2Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                .message(e.getMessage())
                .build();
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(model)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
