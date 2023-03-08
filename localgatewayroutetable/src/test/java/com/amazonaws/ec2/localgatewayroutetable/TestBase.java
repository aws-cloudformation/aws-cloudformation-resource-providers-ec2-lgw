package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTable;
import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableMode;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TestBase {
    final String ROUTE_TABLE_ID = "lgw-rtb-12345678912345678";
    final String ROUTE_TABLE_ARN = "lgw-rtb-12345678912345678"+ ROUTE_TABLE_ID;
    final String LOCAL_GATEWAY_ID = "lgw-12345678912345678";
    final String OWNER_ID = "123456789012";
    final String OUTPOST_ARN = "arn:aws:outposts:us-west-2:" + OWNER_ID + ":outpost/op-12345678912345678";
    final String MODE = LocalGatewayRouteTableMode.COIP.toString();
    final LocalGatewayRouteTable TEST_ROUTE_TABLE = buildLgwRouteTable("available", Collections.emptyList());

    final LocalGatewayRouteTable TEST_ROUTE_TABLE_WITH_TAGS = buildLgwRouteTable("available", Arrays.asList(
            Tag.builder().key("Name").value("MyRouteTable").build(),
            Tag.builder().key("Stage").value("Prod").build()));
    final LocalGatewayRouteTable PENDING_ROUTE_TABLE = buildLgwRouteTable("pending", Collections.emptyList());
    final LocalGatewayRouteTable INACTIVE_ROUTE_TABLE = buildLgwRouteTable("inactive", Collections.emptyList());

    private LocalGatewayRouteTable buildLgwRouteTable(String state, Collection<Tag> tags) {
        return LocalGatewayRouteTable
                .builder()
                .localGatewayRouteTableId(ROUTE_TABLE_ID)
                .localGatewayRouteTableArn(ROUTE_TABLE_ARN)
                .outpostArn(OUTPOST_ARN)
                .localGatewayId(LOCAL_GATEWAY_ID)
                .ownerId(OWNER_ID)
                .mode(MODE)
                .state(state)
                .tags(tags)
                .build();
    }
}
