package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator.getHandlerErrorForEc2Error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TranslatorTest extends TestBase {
    @Test
    public void testGetHandlerErrorForEc2Error() {
        assertThat(getHandlerErrorForEc2Error("UnauthorizedOperation")).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(getHandlerErrorForEc2Error("InvalidParameter")).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(getHandlerErrorForEc2Error("InvalidLocalGatewayRouteTableID.NotFound")).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(getHandlerErrorForEc2Error("InvalidLocalGatewayVirtualInterfaceGroupID.NotFound")).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(getHandlerErrorForEc2Error("InvalidLocalGatewayRouteTableVirtualInterfaceGroupAssociationID.NotFound")).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(getHandlerErrorForEc2Error("AnythingElse")).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void testCreateModelFromVifGroupAssociation() {
        Set<Tag> expectedTags = TagHelper.createCfnTagsFromSdkTags(new HashSet<>(TEST_ASSOCIATION_WITH_TAGS.tags()));
        ResourceModel model = Translator.createModelFromVifGroupAssociation(TEST_ASSOCIATION_WITH_TAGS);
        assertEquals(expectedTags, model.getTags());
        assertEquals(VIF_GROUP_ASSOCIATION_ID, model.getLocalGatewayRouteTableVirtualInterfaceGroupAssociationId());
        assertEquals(LOCAL_GATEWAY_ID, model.getLocalGatewayId());
        assertEquals(ROUTE_TABLE_ID, model.getLocalGatewayRouteTableId());
        assertEquals(ROUTE_TABLE_ARN, model.getLocalGatewayRouteTableArn());
        assertEquals(VIF_GROUP_ID, model.getLocalGatewayVirtualInterfaceGroupId());
        assertEquals(OWNER_ID, model.getOwnerId());
        assertEquals("associated", model.getState());
    }
}
