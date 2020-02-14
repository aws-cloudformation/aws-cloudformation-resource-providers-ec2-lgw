package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.model.LocalGatewayRouteTableVpcAssociation;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

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
            .tags(association.tags()
                .stream()
                .map(Translator::createCfnTagFromSdkTag)
                .collect(Collectors.toSet()))
            .build();
    }

    static software.amazon.awssdk.services.ec2.model.Tag createSdkTagFromCfnTag(final Tag tag) {
        return software.amazon.awssdk.services.ec2.model.Tag.builder()
            .key(tag.getKey())
            .value(tag.getValue())
            .build();
    }

    private static Tag createCfnTagFromSdkTag(final software.amazon.awssdk.services.ec2.model.Tag tag) {
        return Tag.builder()
            .key(tag.key())
            .value(tag.value())
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
