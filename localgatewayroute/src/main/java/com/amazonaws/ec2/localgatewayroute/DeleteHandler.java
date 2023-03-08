package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteLocalGatewayRouteRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroute.EventGenerator.createSuccessEvent;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final Ec2Client client = ClientBuilder.getClient(logger);

        if (callbackContext == null || !callbackContext.isDeleteStarted()) {
            try {
                deleteLocalGatewayRoute(model.getDestinationCidrBlock(), model.getLocalGatewayRouteTableId(), proxy, client);
            } catch (Ec2Exception e) {
                return createFailedEvent(model, e);
            }
        }
        final ReadHandler readHandler = new ReadHandler();
        try {
            final ResourceModel readModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            return createInProgressEventForDelete(readModel);
        } catch (CfnNotFoundException expected) {
            return createSuccessEvent(null);
        } catch (Ec2Exception e) {
            return createFailedEvent(model, e);
        }
    }

    private void deleteLocalGatewayRoute(
        final String destinationCidrBlock,
        final String localGatewayRouteTableId,
        AmazonWebServicesClientProxy proxy,
        Ec2Client client) {

        final DeleteLocalGatewayRouteRequest deleteRequest = DeleteLocalGatewayRouteRequest
            .builder()
            .destinationCidrBlock(destinationCidrBlock)
            .localGatewayRouteTableId(localGatewayRouteTableId)
            .build();

        proxy.injectCredentialsAndInvokeV2(deleteRequest, client::deleteLocalGatewayRoute);
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForDelete(ResourceModel model) {
        return createInProgressEvent(model, CallbackContext.builder().deleteStarted(true).build());
    }
}
