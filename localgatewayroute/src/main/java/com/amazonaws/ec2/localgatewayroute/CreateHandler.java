package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesResponse;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import static com.amazonaws.ec2.localgatewayroute.CallbackContext.POLLING_DELAY_SECONDS;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        final Ec2Client client = com.amazonaws.ec2.localgatewayroute.ClientBuilder.getClient();

        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            createLocalGatewayRoute(
                model.getLocalGatewayRouteTableId(),
                model.getDestinationCidrBlock(),
                model.getLocalGatewayVirtualInterfaceGroupId(),
                proxy,
                client);
        }

        final ReadHandler readHandler = new ReadHandler();
        final ResourceModel resultModel;
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();

            switch (resultModel.getState()) {
                case "pending":
                    return createInProgressEvent(resultModel);
                case "active":
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(resultModel)
                        .status(OperationStatus.SUCCESS)
                        .build();
                default:
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .build();
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEvent(model);
        }
    }

    private void createLocalGatewayRoute(
        final String localGatewayRouteTableId,
        final String destinationCidrBlock,
        final String localGatewayVirtualInterfaceGroupId,
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {

        final SearchLocalGatewayRoutesRequest searchRequest = SearchLocalGatewayRoutesRequest.builder()
            .localGatewayRouteTableId(localGatewayRouteTableId)
            .filters(Filter.builder().name("route-search.exact-match").values(destinationCidrBlock).build())
            .build();

        final CreateLocalGatewayRouteRequest createRequest = CreateLocalGatewayRouteRequest
            .builder()
            .localGatewayRouteTableId(localGatewayRouteTableId)
            .destinationCidrBlock(destinationCidrBlock)
            .localGatewayVirtualInterfaceGroupId(localGatewayVirtualInterfaceGroupId)
            .build();

        // check if this cidr block is already in use for this route table. EC2 only throws an exception if
        // the route is pointing to a different target
        final SearchLocalGatewayRoutesResponse response = proxy.injectCredentialsAndInvokeV2(searchRequest, client::searchLocalGatewayRoutes);
        if (!response.routes().isEmpty()) {
            throw new CfnAlreadyExistsException("LocalGatewayRoute", "localGatewayRouteTableId, destinationCidrBlock: "
                + localGatewayRouteTableId + ", " + destinationCidrBlock);
        }
        proxy.injectCredentialsAndInvokeV2(createRequest, client::createLocalGatewayRoute);
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model) {
        CallbackContext context = CallbackContext.builder()
            .createStarted(true)
            .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(context)
            .callbackDelaySeconds(POLLING_DELAY_SECONDS)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
    }
}
