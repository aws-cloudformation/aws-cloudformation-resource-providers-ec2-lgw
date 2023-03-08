package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.model.*;

import java.util.Collections;
import java.util.List;

import static com.amazonaws.ec2.localgatewayroute.Translator.createModelFromRoute;

class TestBase {
    static String DESTINATION_CIDR = "10.0.0.0/16";
    static String LOCAL_GATEWAY_ROUTE_TABLE_ID = "lgw-rtb-12345678912345678";
    static String LOCAL_GATEWAY_ROUTE_TABLE_ID_2 = "lgw-rtb-3483499393";
    static String LOCAL_GATEWAY_ROUTE_TABLE_ID_3 = "lgw-rtb-9876543211";
    static String LOCAL_GATEWAY_ROUTE_TABLE_ID_4 = "lgw-rtb-5647382910";
    static String LOCAL_GATEWAY_VIF_GROUP_ID = "lgw-vif-grp-34875";
    static String LOCAL_GATEWAY_VIF_GROUP_ID_2 = "lgw-vif-grp-44875";
    static String NETWORK_INTERFACE_ID = "eni-348756789";
    static String NETWORK_INTERFACE_ID_2 = "eni-448756789";
    static String LOCAL_GATEWAY_ID = "lgw-id-2398235";
    static String OUTPOST_ARN = "outpostarn";

    protected final LocalGatewayRoute ACTIVE_VIF_GROUP_ROUTE = buildVifGroupRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID, LOCAL_GATEWAY_VIF_GROUP_ID, LocalGatewayRouteState.ACTIVE);
    protected final LocalGatewayRoute ACTIVE_ENI_ROUTE = buildEniRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID_3, NETWORK_INTERFACE_ID, LocalGatewayRouteState.ACTIVE);
    protected final LocalGatewayRoute BLACKHOLE_VIF_GROUP_ROUTE = buildVifGroupRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID, LOCAL_GATEWAY_VIF_GROUP_ID, LocalGatewayRouteState.BLACKHOLE);
    protected final LocalGatewayRoute BLACKHOLE_ENI_ROUTE = buildEniRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID_3, NETWORK_INTERFACE_ID, LocalGatewayRouteState.BLACKHOLE);
    protected final LocalGatewayRoute PENDING_VIF_GROUP_ROUTE = buildVifGroupRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID, LOCAL_GATEWAY_VIF_GROUP_ID, LocalGatewayRouteState.PENDING);
    protected final LocalGatewayRoute PENDING_ENI_ROUTE = buildVifGroupRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID_3, LOCAL_GATEWAY_VIF_GROUP_ID, LocalGatewayRouteState.PENDING);
    protected final LocalGatewayRoute DELETING_VIF_GROUP_ROUTE = buildVifGroupRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID, LOCAL_GATEWAY_VIF_GROUP_ID, LocalGatewayRouteState.DELETING);
    protected final LocalGatewayRoute DELETING_ENI_ROUTE = buildVifGroupRoute(LOCAL_GATEWAY_ROUTE_TABLE_ID_3, LOCAL_GATEWAY_VIF_GROUP_ID, LocalGatewayRouteState.DELETING);

    final ResourceModel ACTIVE_VIF_GROUP_MODEL = createModelFromRoute(ACTIVE_VIF_GROUP_ROUTE);
    final ResourceModel BLACKHOLE_VIF_GROUP_MODEL = createModelFromRoute(BLACKHOLE_VIF_GROUP_ROUTE);
    final ResourceModel ACTIVE_ENI_MODEL = createModelFromRoute(ACTIVE_ENI_ROUTE);
    final ResourceModel PENDING_VIF_GROUP_MODEL = createModelFromRoute(PENDING_VIF_GROUP_ROUTE);
    final ResourceModel PENDING_ENI_MODEL = createModelFromRoute(PENDING_ENI_ROUTE);

    final SearchLocalGatewayRoutesResponse PENDING_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(PENDING_VIF_GROUP_ROUTE));
    final SearchLocalGatewayRoutesResponse ACTIVE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(ACTIVE_VIF_GROUP_ROUTE));
    final SearchLocalGatewayRoutesResponse BLACKHOLE_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(BLACKHOLE_VIF_GROUP_ROUTE));
    final SearchLocalGatewayRoutesResponse DELETING_VIF_GROUP_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(DELETING_VIF_GROUP_ROUTE));
    final SearchLocalGatewayRoutesResponse EMPTY_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.emptyList());
    final SearchLocalGatewayRoutesResponse PENDING_ENI_SERACH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(PENDING_ENI_ROUTE));
    final SearchLocalGatewayRoutesResponse ACTIVE_ENI_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(ACTIVE_ENI_ROUTE));
    final SearchLocalGatewayRoutesResponse BLACKHOLE_ENI_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(BLACKHOLE_ENI_ROUTE));
    final SearchLocalGatewayRoutesResponse DELETING_ENI_SEARCH_LGW_ROUTES_RESPONSE = buildSearchLgwRoutesResponse(Collections.singletonList(DELETING_ENI_ROUTE));

    final LocalGatewayRouteTable VIF_GROUP_RTB = buildLgwRouteTable(LOCAL_GATEWAY_ROUTE_TABLE_ID);
    final LocalGatewayRouteTable VIF_GRP_RTB_2 = buildLgwRouteTable(LOCAL_GATEWAY_ROUTE_TABLE_ID_2);
    final LocalGatewayRouteTable ENI_RTB = buildLgwRouteTable(LOCAL_GATEWAY_ROUTE_TABLE_ID_3);
    final LocalGatewayRouteTable ENI_RTB_2 = buildLgwRouteTable(LOCAL_GATEWAY_ROUTE_TABLE_ID_4);

    protected LocalGatewayRoute buildVifGroupRoute(String localGatewayRouteTableId, String vifGroupId, LocalGatewayRouteState state) {
        return baseLgwRouteBuilder(localGatewayRouteTableId, state)
                .localGatewayVirtualInterfaceGroupId(vifGroupId)
                .build();
    }

    protected LocalGatewayRoute buildEniRoute(String localGatewayRouteTableId, String networkInterfaceId, LocalGatewayRouteState state) {
        return baseLgwRouteBuilder(localGatewayRouteTableId, state)
                .networkInterfaceId(networkInterfaceId)
                .build();
    }

    protected LocalGatewayRoute.Builder baseLgwRouteBuilder(String localGatewayRouteTableId, LocalGatewayRouteState state) {
        return LocalGatewayRoute.builder().destinationCidrBlock(DESTINATION_CIDR)
                .localGatewayRouteTableId(localGatewayRouteTableId)
                .state(state)
                .type(LocalGatewayRouteType.STATIC);
    }

    protected SearchLocalGatewayRoutesResponse buildSearchLgwRoutesResponse(List<LocalGatewayRoute> routes) {
        return SearchLocalGatewayRoutesResponse
                .builder()
                .routes(routes)
                .build();
    }

    protected LocalGatewayRouteTable buildLgwRouteTable(String localGatewayRouteTableId) {
        return LocalGatewayRouteTable.builder()
                .localGatewayRouteTableId(localGatewayRouteTableId)
                .localGatewayId(LOCAL_GATEWAY_ID)
                .outpostArn(OUTPOST_ARN)
                .state("active")
                .build();
    }
}
