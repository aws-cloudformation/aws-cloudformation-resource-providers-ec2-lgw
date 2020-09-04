package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
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

        final List<ResourceModel> models = describeAllRoutes(proxy, ClientBuilder.getClient(logger));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .status(OperationStatus.SUCCESS)
            .nextToken(null)
            .build();
    }

    private List<ResourceModel> describeAllRoutes(
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {
        List<LocalGatewayRoute> routes = new ArrayList<>();
        String nextToken = null;

        List<String> localGatewayRouteTableIds = getAllRouteTableIds(proxy, client);

        for (String localGatewayRouteTableId : localGatewayRouteTableIds) {
            do {
                final SearchLocalGatewayRoutesRequest request = SearchLocalGatewayRoutesRequest
                    .builder()
                    .localGatewayRouteTableId(localGatewayRouteTableId)
                    .filters(Filter.builder().name("state").values("active").build())
                    .nextToken(nextToken)
                    .build();
                final SearchLocalGatewayRoutesResponse response = proxy.injectCredentialsAndInvokeV2(request, client::searchLocalGatewayRoutes);
                nextToken = response.nextToken();
                routes.addAll(response.routes());
            } while (nextToken != null);
        }

        return routes
            .stream()
            .map(Translator::createModelFromRoute)
            .collect(toList());
    }

    private List<String> getAllRouteTableIds(AmazonWebServicesClientProxy proxy, Ec2Client client) {

        List<LocalGatewayRouteTable> localGatewayRouteTables = new ArrayList<>();
        String nextToken = null;

        do {
            final DescribeLocalGatewayRouteTablesRequest describeLocalGatewayRouteTablesRequest = DescribeLocalGatewayRouteTablesRequest
                .builder()
                .build();
            final DescribeLocalGatewayRouteTablesResponse response = proxy.injectCredentialsAndInvokeV2(
                describeLocalGatewayRouteTablesRequest,
                client::describeLocalGatewayRouteTables);
            nextToken = response.nextToken();
            localGatewayRouteTables.addAll(response.localGatewayRouteTables());
        } while (nextToken != null);

        return localGatewayRouteTables
            .stream()
            .map(LocalGatewayRouteTable::localGatewayRouteTableId)
            .collect(toList());
    }
}
