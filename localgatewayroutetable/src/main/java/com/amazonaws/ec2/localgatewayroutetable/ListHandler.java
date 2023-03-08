package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.cloudformation.proxy.*;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.ec2.localgatewayroutetable.EventGenerator.createSuccessEventForMultipleModels;
import static java.util.stream.Collectors.toList;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final List<ResourceModel> models = describeAllRouteTables(proxy, ClientBuilder.getClient(logger));

        return createSuccessEventForMultipleModels(models);
    }

    private List<ResourceModel> describeAllRouteTables(
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {
        List<LocalGatewayRouteTable> routeTables = new ArrayList<>();
        String nextToken = null;

        do {
            final DescribeLocalGatewayRouteTablesRequest request = DescribeLocalGatewayRouteTablesRequest
                    .builder()
                    .nextToken(nextToken)
                    .build();
            final DescribeLocalGatewayRouteTablesResponse response = proxy.injectCredentialsAndInvokeV2(request, client::describeLocalGatewayRouteTables);
            nextToken = response.nextToken();
            routeTables.addAll(response.localGatewayRouteTables());
        } while (nextToken != null);
        return routeTables
                .stream()
                .map(Translator::createModelFromRouteTable)
                .collect(toList());
    }
}
