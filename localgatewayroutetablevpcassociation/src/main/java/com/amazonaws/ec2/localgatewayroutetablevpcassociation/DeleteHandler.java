package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteLocalGatewayRouteTableVpcAssociationRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.getHandlerErrorForEc2Error;

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
                deleteAssociation(model.getLocalGatewayRouteTableVpcAssociationId(), proxy, client);
            } catch (Ec2Exception e) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                    .message(e.getMessage())
                    .build();
            }
        }
        final ReadHandler readHandler = new ReadHandler();
        try {
            final ResourceModel readModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            return createInProgressEvent(readModel);
        } catch (CfnNotFoundException expected) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .build();
        } catch (Ec2Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                .message(e.getMessage())
                .build();
        }
    }

    private void deleteAssociation(
        final String associationId,
        AmazonWebServicesClientProxy proxy,
        Ec2Client client) {

        final DeleteLocalGatewayRouteTableVpcAssociationRequest deleteRequest = DeleteLocalGatewayRouteTableVpcAssociationRequest
            .builder()
            .localGatewayRouteTableVpcAssociationId(associationId)
            .build();

        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, client::deleteLocalGatewayRouteTableVpcAssociation);
        } catch (Ec2Exception e) {
            if ("InvalidLocalGatewayRouteTableVpcAssociationID.NotFound".equals(e.awsErrorDetails().errorCode())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, associationId);
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
            .callbackDelaySeconds(POLLING_DELAY_SECONDS)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
    }
}
