package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyLocalGatewayRouteRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createNotUpdatableEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createSuccessEvent;

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

            if (model.getLocalGatewayRouteTableId() != null && !model.getLocalGatewayRouteTableId().equals(existingResource.getLocalGatewayRouteTableId())) {
                return createNotUpdatableEvent(model, "LocalGatewayRouteTableId");
            }

            if (model.getDestinationCidrBlock() != null && !model.getDestinationCidrBlock().equals(existingResource.getDestinationCidrBlock())) {
                return createNotUpdatableEvent(model, "DestinationCidrBlock");
            }

            if (model.getType() != null && !model.getType().equals(existingResource.getType())) {
                return createNotUpdatableEvent(model, "Type");
            }

            if (model.getState() != null && !model.getState().equals(existingResource.getState())) {
                return createNotUpdatableEvent(model, "State");
            }

            String routeTarget = null;
            LocalGatewayRouteTargetType targetType = null;
            Boolean updateRequired = false;
            if (model.getLocalGatewayVirtualInterfaceGroupId() != null && model.getNetworkInterfaceId() != null) {
                throw new CfnInvalidRequestException("Request should have either LgwVifGroupId or NetworkInterfaceId as target");
            } else if (model.getLocalGatewayVirtualInterfaceGroupId() != null) {
                // Only attempt to update VIF group route target if it is not already equal to the current target
                if (!model.getLocalGatewayVirtualInterfaceGroupId().equals(existingResource.getLocalGatewayVirtualInterfaceGroupId())) {
                    routeTarget = model.getLocalGatewayVirtualInterfaceGroupId();
                    targetType = LocalGatewayRouteTargetType.LGW_VIF_GROUP;
                    updateRequired = true;
                }
            } else if (model.getNetworkInterfaceId() != null) {
                // Only attempt to update ENI route target if it is not already equal to the current target
                if (!model.getNetworkInterfaceId().equals(existingResource.getNetworkInterfaceId())) {
                    routeTarget = model.getNetworkInterfaceId();
                    targetType = LocalGatewayRouteTargetType.ENI;
                    updateRequired = true;
                }
            }

            if (updateRequired) {
                try {
                    modifyLocalGatewayRoute(
                            model.getLocalGatewayRouteTableId(),
                            model.getDestinationCidrBlock(),
                            routeTarget,
                            targetType,
                            proxy,
                            client);
                } catch (Ec2Exception e) {
                    return createFailedEvent(model, e);
                }
            }
        }

        return createSuccessEvent(model);
    }

    private void modifyLocalGatewayRoute(
            final String localGatewayRouteTableId,
            final String destinationCidrBlock,
            final String targetId,
            final LocalGatewayRouteTargetType targetType,
            final AmazonWebServicesClientProxy proxy,
            final Ec2Client client) {

        final ModifyLocalGatewayRouteRequest.Builder modifyRequestBuilder = ModifyLocalGatewayRouteRequest
                .builder()
                .localGatewayRouteTableId(localGatewayRouteTableId)
                .destinationCidrBlock(destinationCidrBlock);

        switch (targetType) {
            case LGW_VIF_GROUP:
                modifyRequestBuilder.localGatewayVirtualInterfaceGroupId(targetId);
                break;
            case ENI:
                modifyRequestBuilder.networkInterfaceId(targetId);
                break;
        }

        proxy.injectCredentialsAndInvokeV2(modifyRequestBuilder.build(), client::modifyLocalGatewayRoute);
    }
}
