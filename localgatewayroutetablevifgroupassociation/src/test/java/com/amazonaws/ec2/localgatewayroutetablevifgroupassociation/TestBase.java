package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVirtualInterfaceGroupAssociation;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Collections;

public class TestBase {
    final String ROUTE_TABLE_ID = "lgw-rtb-12345678912345678";
    final String ROUTE_TABLE_ARN = "lgw-rtb-12345678912345678"+ ROUTE_TABLE_ID;
    final String LOCAL_GATEWAY_ID = "lgw-12345678912345678";
    final String OWNER_ID = "123456789012";
    final String VIF_GROUP_ID = "lgw-vif-grp-12345678912345678";
    final String VIF_GROUP_ASSOCIATION_ID = "lgw-vif-grp-assoc-12345678912345678";

    final LocalGatewayRouteTableVirtualInterfaceGroupAssociation TEST_ASSOCIATION = LocalGatewayRouteTableVirtualInterfaceGroupAssociation.builder()
            .localGatewayRouteTableVirtualInterfaceGroupAssociationId(VIF_GROUP_ASSOCIATION_ID)
            .localGatewayId(LOCAL_GATEWAY_ID)
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .localGatewayRouteTableArn(ROUTE_TABLE_ARN)
            .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
            .ownerId(OWNER_ID)
            .state("associated")
            .tags(Collections.emptyList())
            .build();

    final LocalGatewayRouteTableVirtualInterfaceGroupAssociation TEST_ASSOCIATION_WITH_TAGS = LocalGatewayRouteTableVirtualInterfaceGroupAssociation.builder()
            .localGatewayRouteTableVirtualInterfaceGroupAssociationId(VIF_GROUP_ASSOCIATION_ID)
            .localGatewayId(LOCAL_GATEWAY_ID)
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .localGatewayRouteTableArn(ROUTE_TABLE_ARN)
            .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
            .ownerId(OWNER_ID)
            .state("associated")
            .tags(
                    Tag.builder().key("Name").value("MyAssociation").build(),
                    Tag.builder().key("Stage").value("Prod").build()
            )
            .build();

    final LocalGatewayRouteTableVirtualInterfaceGroupAssociation TEST_ASSOCIATION_PENDING = LocalGatewayRouteTableVirtualInterfaceGroupAssociation.builder()
            .localGatewayRouteTableVirtualInterfaceGroupAssociationId(VIF_GROUP_ASSOCIATION_ID)
            .localGatewayId(LOCAL_GATEWAY_ID)
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .localGatewayRouteTableArn(ROUTE_TABLE_ARN)
            .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
            .ownerId(OWNER_ID)
            .state("pending")
            .tags(Collections.emptyList())
            .build();
}
