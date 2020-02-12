package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVpcAssociation;

import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Collections;

public class TestBase {
    final String ASSOCIATION_ID = "lgw-vpc-assoc-12345678912345678";
    final String VPC_ID = "vpc-12345678912345678";
    final String ROUTE_TABLE_ID = "lgw-rtb-12345678912345678";
    final String LOCAL_GATEWAY_ID = "lgw-12345678912345678";
    final LocalGatewayRouteTableVpcAssociation TEST_ASSOCIATION = LocalGatewayRouteTableVpcAssociation
        .builder()
        .localGatewayRouteTableVpcAssociationId(ASSOCIATION_ID)
        .vpcId(VPC_ID)
        .localGatewayRouteTableId(ROUTE_TABLE_ID)
        .localGatewayId(LOCAL_GATEWAY_ID)
        .state("associated")
        .tags(Collections.emptyList())
        .build();
    final LocalGatewayRouteTableVpcAssociation TEST_ASSOCIATION_WITH_TAGS = LocalGatewayRouteTableVpcAssociation
        .builder()
        .localGatewayRouteTableVpcAssociationId(ASSOCIATION_ID)
        .vpcId(VPC_ID)
        .localGatewayRouteTableId(ROUTE_TABLE_ID)
        .localGatewayId(LOCAL_GATEWAY_ID)
        .state("associated")
        .tags(
            Tag.builder().key("Name").value("MyAssociation").build(),
            Tag.builder().key("Stage").value("Prod").build()
            )
        .build();
    final LocalGatewayRouteTableVpcAssociation PENDING_ASSOCIATION = LocalGatewayRouteTableVpcAssociation
        .builder()
        .localGatewayRouteTableVpcAssociationId(ASSOCIATION_ID)
        .vpcId(VPC_ID)
        .localGatewayRouteTableId(ROUTE_TABLE_ID)
        .localGatewayId(LOCAL_GATEWAY_ID)
        .state("pending")
        .tags(Collections.emptyList())
        .build();
}
