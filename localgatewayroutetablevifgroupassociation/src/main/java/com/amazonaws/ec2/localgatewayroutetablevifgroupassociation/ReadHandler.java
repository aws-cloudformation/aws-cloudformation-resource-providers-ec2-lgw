package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.CallbackContext;
import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.ClientBuilder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVirtualInterfaceGroupAssociation;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator.createModelFromVifGroupAssociation;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return createSuccessEvent(describeRouteTableVifGroupAssociation(model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId(), proxy, ClientBuilder.getClient(logger)));
    }

    private ResourceModel describeRouteTableVifGroupAssociation(
            String localGatewayRouteTableVirtualInterfaceGroupAssociationId,
            AmazonWebServicesClientProxy proxy,
            Ec2Client client) {

        String nextToken = null;
        LocalGatewayRouteTableVirtualInterfaceGroupAssociation vifGroupAssociation = null;
        do {
            final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest request = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest
                    .builder()
                    .localGatewayRouteTableVirtualInterfaceGroupAssociationIds(localGatewayRouteTableVirtualInterfaceGroupAssociationId)
                    .nextToken(nextToken)
                    .build();
            final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse response = proxy.injectCredentialsAndInvokeV2(request, client::describeLocalGatewayRouteTableVirtualInterfaceGroupAssociations);
            if (response.localGatewayRouteTableVirtualInterfaceGroupAssociations().size() > 1) {
                throw new CfnGeneralServiceException("Should be 1 route table virtual interface group association when reading, but was " + response.localGatewayRouteTableVirtualInterfaceGroupAssociations());
            }
            if (!response.localGatewayRouteTableVirtualInterfaceGroupAssociations().isEmpty()) {
                vifGroupAssociation = response.localGatewayRouteTableVirtualInterfaceGroupAssociations().get(0);
            }
            nextToken = response.nextToken();
        } while (vifGroupAssociation == null && nextToken != null);
        if (vifGroupAssociation == null) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, localGatewayRouteTableVirtualInterfaceGroupAssociationId);
        }
        return createModelFromVifGroupAssociation(vifGroupAssociation);
    }
}
