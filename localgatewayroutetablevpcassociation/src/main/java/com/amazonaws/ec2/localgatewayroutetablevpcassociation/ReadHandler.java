package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsResponse;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVpcAssociation;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.createModelFromAssociation;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final Ec2Client client = ClientBuilder.getClient(logger);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(describeVpcAssociation(model.getLocalGatewayRouteTableVpcAssociationId(), proxy, client))
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private ResourceModel describeVpcAssociation(
        final String vpcAssociationId,
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {

        String nextToken = null;
        LocalGatewayRouteTableVpcAssociation association = null;
        do {
            final DescribeLocalGatewayRouteTableVpcAssociationsRequest request = DescribeLocalGatewayRouteTableVpcAssociationsRequest
                .builder()
                .localGatewayRouteTableVpcAssociationIds(vpcAssociationId)
                .nextToken(nextToken)
                .build();
            final DescribeLocalGatewayRouteTableVpcAssociationsResponse response = proxy.injectCredentialsAndInvokeV2(request, client::describeLocalGatewayRouteTableVpcAssociations);
            if (response.localGatewayRouteTableVpcAssociations().size() > 1) {
                throw new CfnGeneralServiceException("Should be 1 association when reading, but was " + response.localGatewayRouteTableVpcAssociations());
            }
            if (!response.localGatewayRouteTableVpcAssociations().isEmpty()) {
                association = response.localGatewayRouteTableVpcAssociations().get(0);
            }
            nextToken = response.nextToken();
        } while (association == null && nextToken != null);
        if (association == null) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, vpcAssociationId);
        }
        return createModelFromAssociation(association);
    }
}
