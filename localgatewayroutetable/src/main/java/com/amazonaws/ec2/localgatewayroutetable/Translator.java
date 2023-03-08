package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTable;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class Translator {
    private Translator() {
    }

    static ResourceModel createModelFromRouteTable(LocalGatewayRouteTable routeTable) {
        return ResourceModel.builder()
                .localGatewayRouteTableId(routeTable.localGatewayRouteTableId())
                .localGatewayRouteTableArn(routeTable.localGatewayRouteTableArn())
                .localGatewayId(routeTable.localGatewayId())
                .ownerId(routeTable.ownerId())
                .outpostArn(routeTable.outpostArn())
                .mode(routeTable.modeAsString())
                .state(routeTable.state())
                .tags(TagHelper.createCfnTagsFromSdkTags(new HashSet<>(routeTable.tags())))
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
            default:
                return HandlerErrorCode.GeneralServiceException;
        }
    }
}
