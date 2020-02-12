package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRoute;

class Translator {
    private Translator() {
    }

    static ResourceModel createModelFromRoute(LocalGatewayRoute route) {
        return ResourceModel.builder()
            .destinationCidrBlock(route.destinationCidrBlock())
            .localGatewayRouteTableId(route.localGatewayRouteTableId())
            .localGatewayVirtualInterfaceGroupId(route.localGatewayVirtualInterfaceGroupId())
            .state(route.state() == null ? null : route.state().toString())
            .type(route.type() == null ? null : route.type().toString())
            .build();
    }
}
