package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteLocalGatewayRouteRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final Ec2Client client = ClientBuilder.getClient();

        if (callbackContext == null || !callbackContext.isDeleteStarted()) {
            deleteLocalGatewayRoute(model.getDestinationCidrBlock(), model.getLocalGatewayRouteTableId(), proxy, client);
        }
        final ReadHandler readHandler = new ReadHandler();
        try {
            final ResourceModel readModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            return createInProgressEvent(readModel);
        } catch (CfnNotFoundException expected) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .build();
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

        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, client::deleteLocalGatewayRoute);
        } catch (Ec2Exception e) {
            // no matching route table found
            if ("InvalidLocalGatewayRouteTableID.NotFound".equals(e.awsErrorDetails().errorCode())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, localGatewayRouteTableId);
            }
            // no matching cidr found
            if ("InvalidRoute.NotFound".equals(e.awsErrorDetails().errorCode())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, destinationCidrBlock);
            }

            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model) {
        CallbackContext context = CallbackContext.builder()
            .deleteStarted(true)
            .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(context)
            .callbackDelaySeconds(CallbackContext.POLLING_DELAY_SECONDS)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
    }
}
