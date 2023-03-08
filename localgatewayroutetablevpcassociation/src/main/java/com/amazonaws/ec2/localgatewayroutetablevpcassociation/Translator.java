package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVpcAssociation;
import software.amazon.awssdk.services.ec2.model.TagDescription;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.HashSet;
import java.util.stream.Collectors;

public class Translator {
    private Translator() {
    }

    static ResourceModel createModelFromAssociation(final LocalGatewayRouteTableVpcAssociation association) {
        return ResourceModel.builder()
            .localGatewayId(association.localGatewayId())
            .localGatewayRouteTableId(association.localGatewayRouteTableId())
            .localGatewayRouteTableVpcAssociationId(association.localGatewayRouteTableVpcAssociationId())
            .state(association.state())
            .vpcId(association.vpcId())
            .tags(TagHelper.createCfnTagsFromSdkTags(new HashSet<>(association.tags())))
            .build();
    }

    static HandlerErrorCode getHandlerErrorForEc2Error(final String errorCode) {
        switch (errorCode) {
            case "UnauthorizedOperation":
                return HandlerErrorCode.AccessDenied;
            case "InvalidParameter":
                return HandlerErrorCode.InvalidRequest;
            default:
                return HandlerErrorCode.GeneralServiceException;
        }
    }
}
