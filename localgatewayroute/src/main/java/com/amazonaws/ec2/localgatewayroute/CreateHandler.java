package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createFailedReadOnlyPropertyEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createSuccessEvent;
import static com.amazonaws.ec2.localgatewayroute.Translator.createModelFromRoute;
import static com.amazonaws.ec2.localgatewayroute.Translator.getHandlerErrorForEc2Error;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        final Ec2Client client = com.amazonaws.ec2.localgatewayroute.ClientBuilder.getClient(logger);

        final ReadHandler readHandler = new ReadHandler();

        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            // Return InvalidRequest if caller is attempting to set a read-only property
            if (model.getType() != null) {
                return createFailedReadOnlyPropertyEvent(model, "Type");
            }
            if (model.getState() != null) {
                return createFailedReadOnlyPropertyEvent(model, "State");
            }
            try {
                String routeTarget = null;
                LocalGatewayRouteTargetType targetType = null;
                if ((model.getLocalGatewayVirtualInterfaceGroupId() != null && model.getNetworkInterfaceId() != null) ||
                        (model.getLocalGatewayVirtualInterfaceGroupId() == null && model.getNetworkInterfaceId() == null)) {
                    throw new CfnInvalidRequestException("Request should have either LgwVifGroupId or NetworkInterfaceId as target");
                } else if (model.getLocalGatewayVirtualInterfaceGroupId() != null) {
                    routeTarget = model.getLocalGatewayVirtualInterfaceGroupId();
                    targetType = LocalGatewayRouteTargetType.LGW_VIF_GROUP;
                } else if (model.getNetworkInterfaceId() != null) {
                    routeTarget = model.getNetworkInterfaceId();
                    targetType = LocalGatewayRouteTargetType.ENI;
                }

                // Check if a route with primary identifier (lgw-rtb-id, destination-cidr-block) exists already
                try {
                    readHandler.handleRequest(proxy, request, null, logger);
                    throw new CfnAlreadyExistsException("LocalGatewayRoute", "localGatewayRouteTableId, destinationCidrBlock: "
                            + model.getLocalGatewayRouteTableId() + ", " + model.getDestinationCidrBlock());
                } catch (CfnNotFoundException e) {
                    // Route not found, proceed with creating it
                }

                createLocalGatewayRoute(
                        model.getLocalGatewayRouteTableId(),
                        model.getDestinationCidrBlock(),
                        routeTarget,
                        targetType,
                        proxy,
                        client);
            } catch (Ec2Exception e) {
                return ProgressEvent.defaultFailureHandler(e, getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()));
            }
        }

        final ResourceModel resultModel;
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();

            switch (resultModel.getState()) {
                case "pending":
                    return createInProgressEventForCreate(resultModel);
                case "active":
                case "blackhole":
                    return createSuccessEvent(resultModel);
                default:
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .build();
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEventForCreate(model);
        } catch (Ec2Exception e) {
            return createFailedEvent(model, e);
        }
    }

    private ResourceModel createLocalGatewayRoute(
        final String localGatewayRouteTableId,
        final String destinationCidrBlock,
        final String targetId,
        final LocalGatewayRouteTargetType targetType,
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {

        final CreateLocalGatewayRouteRequest.Builder createRequestBuilder = CreateLocalGatewayRouteRequest
                .builder()
                .localGatewayRouteTableId(localGatewayRouteTableId)
                .destinationCidrBlock(destinationCidrBlock);

        switch (targetType) {
            case LGW_VIF_GROUP:
                createRequestBuilder.localGatewayVirtualInterfaceGroupId(targetId);
                break;
            case ENI:
                createRequestBuilder.networkInterfaceId(targetId);
                break;
        }

        return createModelFromRoute(proxy.injectCredentialsAndInvokeV2(createRequestBuilder.build(), client::createLocalGatewayRoute).route());
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForCreate(ResourceModel model) {
        return createInProgressEvent(model, CallbackContext.builder().createStarted(true).build());
    }
}
