package com.amazonaws.ec2.localgatewayroute;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRoute;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

class Translator {
    private Translator() {
    }

    static ResourceModel createModelFromRoute(LocalGatewayRoute route) {
        // One of LGW VIF Group ID and ENI ID will be null
        return ResourceModel.builder()
                .destinationCidrBlock(route.destinationCidrBlock())
                .localGatewayRouteTableId(route.localGatewayRouteTableId())
                .localGatewayVirtualInterfaceGroupId(route.localGatewayVirtualInterfaceGroupId())
                .networkInterfaceId(route.networkInterfaceId())
                .state(route.state() == null ? null : route.state().toString())
                .type(route.type() == null ? null : route.type().toString())
                .build();
    }

    static HandlerErrorCode getHandlerErrorForEc2Error(final String errorCode) {
        switch (errorCode) {
            case "UnauthorizedOperation":
                return HandlerErrorCode.AccessDenied;
            case "InvalidParameter":
                return HandlerErrorCode.InvalidRequest;
            case "InvalidLocalGatewayRouteTableID.NotFound":
                return HandlerErrorCode.NotFound;
            case "InvalidRoute.NotFound":
                return HandlerErrorCode.NotFound;
            default:
                return HandlerErrorCode.GeneralServiceException;
        }
    }
}
