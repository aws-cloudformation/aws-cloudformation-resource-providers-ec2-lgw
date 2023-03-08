package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.CallbackContext;
import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.ClientBuilder;
import com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.cloudformation.proxy.*;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEventForMultipleModels;
import static java.util.stream.Collectors.toList;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final List<ResourceModel> models = describeAllRouteTableVirtualInterfaceGroupAssociations(proxy, ClientBuilder.getClient(logger));

        return createSuccessEventForMultipleModels(models);
    }

    private List<ResourceModel> describeAllRouteTableVirtualInterfaceGroupAssociations(
            final AmazonWebServicesClientProxy proxy,
            final Ec2Client client) {
        List<LocalGatewayRouteTableVirtualInterfaceGroupAssociation> vifGroupAssociations = new ArrayList<>();
        String nextToken = null;

        do {
            final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest request = DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsRequest
                    .builder()
                    .nextToken(nextToken)
                    .build();
            final DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse response = proxy.injectCredentialsAndInvokeV2(request, client::describeLocalGatewayRouteTableVirtualInterfaceGroupAssociations);
            nextToken = response.nextToken();
            vifGroupAssociations.addAll(response.localGatewayRouteTableVirtualInterfaceGroupAssociations());
        } while (nextToken != null);
        return vifGroupAssociations
                .stream()
                .map(Translator::createModelFromVifGroupAssociation)
                .collect(toList());
    }
}
