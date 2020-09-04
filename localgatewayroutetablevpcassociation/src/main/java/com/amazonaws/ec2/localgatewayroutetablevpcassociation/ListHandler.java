package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsResponse;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVpcAssociation;
import software.amazon.cloudformation.proxy.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final List<ResourceModel> models = describeAllVpcAssociations(proxy, ClientBuilder.getClient(logger));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private List<ResourceModel> describeAllVpcAssociations(
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {

        List<LocalGatewayRouteTableVpcAssociation> associations = new ArrayList<>();
        String nextToken = null;
        do {
            final DescribeLocalGatewayRouteTableVpcAssociationsRequest request = DescribeLocalGatewayRouteTableVpcAssociationsRequest
                .builder()
                .nextToken(nextToken)
                .build();
            final DescribeLocalGatewayRouteTableVpcAssociationsResponse response = proxy.injectCredentialsAndInvokeV2(request, client::describeLocalGatewayRouteTableVpcAssociations);
            nextToken = response.nextToken();
            associations.addAll(response.localGatewayRouteTableVpcAssociations());
        } while (nextToken != null);
        return associations
            .stream()
            .map(Translator::createModelFromAssociation)
            .collect(toList());
    }
}
