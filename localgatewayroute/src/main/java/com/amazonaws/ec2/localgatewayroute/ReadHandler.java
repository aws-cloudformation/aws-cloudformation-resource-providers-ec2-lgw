package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRoute;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroute.Translator.createModelFromRoute;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(searchForRoute(model.getLocalGatewayRouteTableId(), model.getDestinationCidrBlock(), proxy, ClientBuilder.getClient()))
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private ResourceModel searchForRoute(
        String localGatewayRouteTableId,
        String destinationCidr,
        AmazonWebServicesClientProxy proxy,
        Ec2Client client) {

        String nextToken = null;
        LocalGatewayRoute route = null;

        do {
            SearchLocalGatewayRoutesRequest request = SearchLocalGatewayRoutesRequest.builder()
                .localGatewayRouteTableId(localGatewayRouteTableId)
                .filters(Filter.builder().name("route-search.exact-match").values(destinationCidr).build())
                .nextToken(nextToken)
                .build();
            final SearchLocalGatewayRoutesResponse response = proxy.injectCredentialsAndInvokeV2(request, client::searchLocalGatewayRoutes);
            if (response.routes().size() > 1) {
                throw new CfnGeneralServiceException("Should be 1 route when reading, but was " + response.routes());
            }
            if (!response.routes().isEmpty()) {
                route = response.routes().get(0);
            }
            nextToken = response.nextToken();
        } while (route == null && nextToken != null);
        if (route == null) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, destinationCidr);
        }
        return createModelFromRoute(route);
    }
}
