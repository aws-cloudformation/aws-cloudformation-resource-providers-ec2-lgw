package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTable;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroutetable.EventGenerator.createSuccessEvent;
import static com.amazonaws.ec2.localgatewayroutetable.Translator.createModelFromRouteTable;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return createSuccessEvent(describeRouteTable(model.getLocalGatewayRouteTableId(), proxy, ClientBuilder.getClient(logger)));
    }

    private ResourceModel describeRouteTable(
        String localGatewayRouteTableId,
        AmazonWebServicesClientProxy proxy,
        Ec2Client client) {

        String nextToken = null;
        LocalGatewayRouteTable routeTable = null;
        do {
            final DescribeLocalGatewayRouteTablesRequest request = DescribeLocalGatewayRouteTablesRequest
                    .builder()
                    .localGatewayRouteTableIds(localGatewayRouteTableId)
                    .nextToken(nextToken)
                    .build();
            final DescribeLocalGatewayRouteTablesResponse response = proxy.injectCredentialsAndInvokeV2(request, client::describeLocalGatewayRouteTables);
            if (response.localGatewayRouteTables().size() > 1) {
                throw new CfnGeneralServiceException("Should be 1 route table when reading, but was " + response.localGatewayRouteTables());
            }
            if (!response.localGatewayRouteTables().isEmpty()) {
                routeTable = response.localGatewayRouteTables().get(0);
            }
            nextToken = response.nextToken();
        } while (routeTable == null && nextToken != null);
        if (routeTable == null) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, localGatewayRouteTableId);
        }
        return createModelFromRouteTable(routeTable);
    }
}
