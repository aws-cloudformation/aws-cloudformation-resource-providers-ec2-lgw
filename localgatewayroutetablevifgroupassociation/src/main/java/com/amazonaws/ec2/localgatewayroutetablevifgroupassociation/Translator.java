package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVirtualInterfaceGroupAssociation;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class Translator {
    private Translator() {
    }

    static ResourceModel createModelFromVifGroupAssociation(LocalGatewayRouteTableVirtualInterfaceGroupAssociation vifGroupAssociation) {
        return ResourceModel.builder()
                .localGatewayRouteTableVirtualInterfaceGroupAssociationId(vifGroupAssociation.localGatewayRouteTableVirtualInterfaceGroupAssociationId())
                .localGatewayId(vifGroupAssociation.localGatewayId())
                .localGatewayRouteTableId(vifGroupAssociation.localGatewayRouteTableId())
                .localGatewayRouteTableArn(vifGroupAssociation.localGatewayRouteTableArn())
                .localGatewayVirtualInterfaceGroupId(vifGroupAssociation.localGatewayVirtualInterfaceGroupId())
                .ownerId(vifGroupAssociation.ownerId())
                .state(vifGroupAssociation.state())
                .tags(TagHelper.createCfnTagsFromSdkTags(new HashSet<>(vifGroupAssociation.tags())))
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
            case "InvalidLocalGatewayVirtualInterfaceGroupID.NotFound":
                return HandlerErrorCode.NotFound;
            case "InvalidLocalGatewayRouteTableVirtualInterfaceGroupAssociationID.NotFound":
                return HandlerErrorCode.NotFound;
            default:
                return HandlerErrorCode.GeneralServiceException;
        }
    }
}
