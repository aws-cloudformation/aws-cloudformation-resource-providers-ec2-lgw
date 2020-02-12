package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final Ec2Client client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();

        if (callbackContext == null || !callbackContext.isUpdateStarted()) {
            final ReadHandler readHandler = new ReadHandler();
            final ResourceModel existingResource = readHandler.handleRequest(proxy, request, callbackContext, logger).getResourceModel();
            if (!existingResource.getLocalGatewayRouteTableId().equals(model.getLocalGatewayRouteTableId())
                || !existingResource.getVpcId().equals(model.getVpcId())) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .message("Cannot update not updatable properties LocalGatewayRouteTableId or VpcId")
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotUpdatable)
                    .build();
            }
            if (existingResource.getTags().equals(model.getTags())) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
            }
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(CallbackContext.builder().updateStarted(true).build())
                .resourceModel(model)
                .status(OperationStatus.IN_PROGRESS)
                .build();
        }

        final DeleteTagsRequest deleteTagsRequest = DeleteTagsRequest
            .builder()
            .resources(model.getLocalGatewayRouteTableVpcAssociationId())
            .build();
        proxy.injectCredentialsAndInvokeV2(deleteTagsRequest, client::deleteTags);

        final CreateTagsRequest createTagsRequest = CreateTagsRequest
            .builder()
            .tags(model.getTags().stream().map(Translator::createSdkTagFromCfnTag).collect(Collectors.toSet()))
            .resources(model.getLocalGatewayRouteTableVpcAssociationId())
            .build();
        proxy.injectCredentialsAndInvokeV2(createTagsRequest, client::createTags);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(model)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
