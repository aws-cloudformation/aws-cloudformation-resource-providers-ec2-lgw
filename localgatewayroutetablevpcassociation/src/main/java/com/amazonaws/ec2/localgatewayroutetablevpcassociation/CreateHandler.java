package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVpcAssociationRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.createModelFromAssociation;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        Set<Tag> tags = model.getTags();
        final Ec2Client client = ClientBuilder.getClient();
        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            model = createAssociation(model.getLocalGatewayRouteTableId(), model.getVpcId(), proxy, client);
            if (tags != null) {
                model.setTags(tags);
            }
            request.setDesiredResourceState(model);
        }

        final ReadHandler readHandler = new ReadHandler();
        final ResourceModel resultModel;
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            if (!"associated".equals(resultModel.getState())) {
                return createInProgressEvent(resultModel);
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEvent(model);
        }
        if (tags != null && !tags.isEmpty()) {
            final CreateTagsRequest createTagsRequest = CreateTagsRequest
                .builder()
                .tags(tags.stream().map(Translator::createSdkTagFromCfnTag).collect(Collectors.toSet()))
                .resources(model.getLocalGatewayRouteTableVpcAssociationId())
                .build();
            proxy.injectCredentialsAndInvokeV2(createTagsRequest, client::createTags);
        }
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(resultModel)
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private ResourceModel createAssociation(
        final String localGatewayRouteTableId,
        final String vpcId,
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {

        final CreateLocalGatewayRouteTableVpcAssociationRequest request = CreateLocalGatewayRouteTableVpcAssociationRequest
            .builder()
            .localGatewayRouteTableId(localGatewayRouteTableId)
            .vpcId(vpcId)
            .build();

        try {
            return createModelFromAssociation(proxy.injectCredentialsAndInvokeV2(request, client::createLocalGatewayRouteTableVpcAssociation)
                .localGatewayRouteTableVpcAssociation());
        } catch (Ec2Exception e) {
            if ("LocalGatewayRouteTableVpcAssociationAlreadyExists".equals(e.awsErrorDetails().errorCode())) {
                throw new CfnAlreadyExistsException("LocalGatewayRouteTableVPCAssociation", "localGatewayRouteTableId, vpcId: "
                    + localGatewayRouteTableId + ", " + vpcId);
            }
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model) {
        CallbackContext context = CallbackContext.builder()
            .createStarted(true)
            .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(context)
            .callbackDelaySeconds(POLLING_DELAY_SECONDS)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
    }
}
