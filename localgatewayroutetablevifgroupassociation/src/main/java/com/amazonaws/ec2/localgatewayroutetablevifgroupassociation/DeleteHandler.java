package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.CallbackContext;
import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.ClientBuilder;
import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.ReadHandler;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEvent;

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
                deleteLocalGatewayRouteTableVifGroupAssociation(model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId(), proxy, client);
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

    private void deleteLocalGatewayRouteTableVifGroupAssociation(
            final String localGatewayRouteTableVirtualInterfaceGroupAssociationId,
            AmazonWebServicesClientProxy proxy,
            Ec2Client client) {

        final DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest deleteRequest = DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociationRequest
                .builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociationId(localGatewayRouteTableVirtualInterfaceGroupAssociationId)
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, client::deleteLocalGatewayRouteTableVirtualInterfaceGroupAssociation);
        } catch (Ec2Exception e) {
            if ("InvalidLocalGatewayRouteTableVirtualInterfaceGroupAssociationId.NotFound".equals(e.awsErrorDetails().errorCode())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, localGatewayRouteTableVirtualInterfaceGroupAssociationId);
            }
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForDelete(ResourceModel model) {
        return createInProgressEvent(model, CallbackContext.builder().deleteStarted(true).build());
    }
}
