package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.model.*;

import java.util.Collections;

class TestBase {
    static String DESTINATION_CIDR = "10.0.0.0/16";
    static String LOCAL_GATEWAY_ROUTE_TABLE_ID = "lgw-rtb-12345678912345678";
    static String LOCAL_GATEWAY_ROUTE_TABLE_ID_2 = "lgw-rtb-3483499393";
    static String LOCAL_GATEWAY_VIF_GROUP_ID = "lgw-vif-grp-34875";
    static String LOCAL_GATEWAY_ID = "lgw-id-2398235";
    static String OUTPOST_ARN = "outpostarn";
    static final LocalGatewayRoute TEST_ROUTE = LocalGatewayRoute.builder()
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .destinationCidrBlock(DESTINATION_CIDR)
        .build();

    final LocalGatewayRoute activeRoute = LocalGatewayRoute.builder().destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
        .state(LocalGatewayRouteState.ACTIVE.toString())
        .type(LocalGatewayRouteType.STATIC)
        .build();

    final LocalGatewayRoute pendingRoute = LocalGatewayRoute.builder().destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
        .state(LocalGatewayRouteState.PENDING.toString())
        .type(LocalGatewayRouteType.STATIC)
        .build();

    final ResourceModel activeModel = ResourceModel.builder()
        .destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
        .state(LocalGatewayRouteState.ACTIVE.toString())
        .type(LocalGatewayRouteType.STATIC.toString())
        .build();

    final ResourceModel pendingModel = ResourceModel.builder()
        .destinationCidrBlock(DESTINATION_CIDR)
        .localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayVirtualInterfaceGroupId(LOCAL_GATEWAY_VIF_GROUP_ID)
        .state(LocalGatewayRouteState.PENDING.toString())
        .type(LocalGatewayRouteType.STATIC.toString())
        .build();

    final SearchLocalGatewayRoutesResponse pendingSearchLgwRoutesResponse = SearchLocalGatewayRoutesResponse
        .builder()
        .routes(pendingRoute)
        .build();

    final SearchLocalGatewayRoutesResponse activeSearchLgwRoutesResponse = SearchLocalGatewayRoutesResponse
        .builder()
        .routes(activeRoute)
        .build();

    final SearchLocalGatewayRoutesResponse emptyLocalGatewayRoutesResponse = SearchLocalGatewayRoutesResponse
        .builder()
        .routes(Collections.emptyList())
        .build();

    final LocalGatewayRouteTable routeTableOne = LocalGatewayRouteTable.
        builder().
        localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID)
        .localGatewayId(LOCAL_GATEWAY_ID)
        .outpostArn(OUTPOST_ARN)
        .state("active")
        .build();

    final LocalGatewayRouteTable routeTableTwo = LocalGatewayRouteTable.
        builder().
        localGatewayRouteTableId(LOCAL_GATEWAY_ROUTE_TABLE_ID_2)
        .localGatewayId(LOCAL_GATEWAY_ID)
        .outpostArn(OUTPOST_ARN)
        .state("active")
        .build();
}
