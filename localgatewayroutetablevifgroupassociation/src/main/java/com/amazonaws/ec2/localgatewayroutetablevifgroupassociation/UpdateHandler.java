package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createNotUpdatableEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEvent;

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
                return createFailedEvent(model, e);
            }

            if (model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId() != null && !model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId().equals(existingResource.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId())) {
                return createNotUpdatableEvent(model, "LocalGatewayRouteTableVirtualInterfaceGroupAssociationId");
            }

            if (model.getLocalGatewayId() != null && !model.getLocalGatewayId().equals(existingResource.getLocalGatewayId())) {
                return createNotUpdatableEvent(model, "LocalGatewayId");
            }

            if (model.getLocalGatewayRouteTableId() != null && !model.getLocalGatewayRouteTableId().equals(existingResource.getLocalGatewayRouteTableId())) {
                return createNotUpdatableEvent(model, "LocalGatewayRouteTableId");
            }

            if (model.getLocalGatewayRouteTableArn() != null && !model.getLocalGatewayRouteTableArn().equals(existingResource.getLocalGatewayRouteTableArn())) {
                return createNotUpdatableEvent(model, "LocalGatewayRouteTableArn");
            }

            if (model.getLocalGatewayVirtualInterfaceGroupId() != null && !model.getLocalGatewayVirtualInterfaceGroupId().equals(existingResource.getLocalGatewayVirtualInterfaceGroupId())) {
                return createNotUpdatableEvent(model, "LocalGatewayVirtualInterfaceGroupId");
            }

            if (model.getOwnerId() != null && !model.getOwnerId().equals(existingResource.getOwnerId())) {
                return createNotUpdatableEvent(model, "OwnerId");
            }

            if (model.getState() != null && !model.getState().equals(existingResource.getState())) {
                return createNotUpdatableEvent(model, "State");
            }

            final Set<Tag> currentTags = existingResource.getTags();
            final Set<Tag> desiredTags = TagHelper.getAllResourceTags(request);
            if (currentTags.equals(desiredTags)) {
                return createSuccessEvent(model);
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

            return createInProgressEvent(model, nextContext, 0);
        }

        // Create new tags before deleting old ones
        try {
            if (callbackContext.getTagsToCreate() != null && !callbackContext.getTagsToCreate().isEmpty()) {
                final CreateTagsRequest createTagsRequest = CreateTagsRequest
                        .builder()
                        .tags(callbackContext.getTagsToCreate().stream().map(TagHelper::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                        .resources(model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId())
                        .build();
                proxy.injectCredentialsAndInvokeV2(createTagsRequest, client::createTags);
            }
            if (callbackContext.getTagsToDelete() != null && !callbackContext.getTagsToDelete().isEmpty()) {
                final DeleteTagsRequest deleteTagsRequest = DeleteTagsRequest
                        .builder()
                        .tags(callbackContext.getTagsToDelete().stream().map(TagHelper::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                        .resources(model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId())
                        .build();
                proxy.injectCredentialsAndInvokeV2(deleteTagsRequest, client::deleteTags);
            }
        } catch (Ec2Exception e) {
            return createFailedEvent(model, e);
        }

        return createSuccessEvent(model);
    }
}

