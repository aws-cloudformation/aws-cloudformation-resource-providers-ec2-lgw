package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createFailedReadOnlyPropertyEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator.createModelFromVifGroupAssociation;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel resultModel;

        final Ec2Client client = ClientBuilder.getClient(logger);

        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            // Return InvalidRequest if caller is attempting to set a read-only property
            if (model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayRouteTableVirtualInterfaceGroupAssociationId");
            }
            if (model.getLocalGatewayRouteTableArn() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayRouteTableArn");
            }
            if (model.getLocalGatewayId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayId");
            }
            if (model.getOwnerId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "OwnerId");
            }
            if (model.getState() != null) {
                return createFailedReadOnlyPropertyEvent(model, "State");
            }
            try {
                resultModel = createLocalGatewayRouteTableVifGroupAssociation(
                        model.getLocalGatewayRouteTableId(),
                        model.getLocalGatewayVirtualInterfaceGroupId(),
                        TagHelper.getAllResourceTags(request),
                        proxy,
                        client);
            } catch (Ec2Exception e) {
                return createFailedEvent(model, e);
            }
            return createInProgressEventForCreate(resultModel, 0);
        }

        final ReadHandler readHandler = new ReadHandler();
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            if (!"associated".equals(resultModel.getState())) {
                return createInProgressEventForCreate(resultModel);
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEventForCreate(model);
        } catch (Ec2Exception e) {
            return createFailedEvent(model, e);
        }

        return createSuccessEvent(resultModel);
    }

    private ResourceModel createLocalGatewayRouteTableVifGroupAssociation(
            final String localGatewayRouteTableId,
            final String localGatewayVirtualInterfaceGroupId,
            final Set<Tag> tags,
            final AmazonWebServicesClientProxy proxy,
            final Ec2Client client) {

        final CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest.Builder createRequest = CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest
                .builder()
                .localGatewayRouteTableId(localGatewayRouteTableId)
                .localGatewayVirtualInterfaceGroupId(localGatewayVirtualInterfaceGroupId);

        if (tags != null && !tags.isEmpty()) {
            createRequest.tagSpecifications(TagSpecification.builder().resourceType("local-gateway-route-table-virtual-interface-group-association").tags(TagHelper.createSdkTagsFromCfnTags(tags)).build());
        }

        try {
            return createModelFromVifGroupAssociation(proxy.injectCredentialsAndInvokeV2(createRequest.build(), client::createLocalGatewayRouteTableVirtualInterfaceGroupAssociation).localGatewayRouteTableVirtualInterfaceGroupAssociation());
        } catch (Ec2Exception e) {
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForCreate(ResourceModel model) {
        return createInProgressEvent(model, CallbackContext.builder().createStarted(true).build());
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForCreate(ResourceModel model, int pollingDelaySeconds) {
        return createInProgressEvent(model, CallbackContext.builder().createStarted(true).build(), pollingDelaySeconds);
    }
}

