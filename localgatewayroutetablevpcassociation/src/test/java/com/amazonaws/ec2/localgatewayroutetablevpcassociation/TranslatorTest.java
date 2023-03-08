package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.getHandlerErrorForEc2Error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TranslatorTest extends TestBase {
    @Test
    public void testGetHandlerErrorForEc2Error() {
        assertThat(getHandlerErrorForEc2Error("UnauthorizedOperation")).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(getHandlerErrorForEc2Error("InvalidParameter")).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(getHandlerErrorForEc2Error("AnythingElse")).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void testCreateModelFromAssociation() {
        Set<Tag> expectedTags = TagHelper.createCfnTagsFromSdkTags(new HashSet<>(TEST_ASSOCIATION_WITH_TAGS.tags()));
        ResourceModel model = Translator.createModelFromAssociation(TEST_ASSOCIATION_WITH_TAGS);
        assertEquals(expectedTags, model.getTags());
        assertEquals(ASSOCIATION_ID, model.getLocalGatewayRouteTableVpcAssociationId());
        assertEquals(LOCAL_GATEWAY_ID, model.getLocalGatewayId());
        assertEquals(ROUTE_TABLE_ID, model.getLocalGatewayRouteTableId());
        assertEquals(VPC_ID, model.getVpcId());
        assertEquals("associated", model.getState());
    }
}
