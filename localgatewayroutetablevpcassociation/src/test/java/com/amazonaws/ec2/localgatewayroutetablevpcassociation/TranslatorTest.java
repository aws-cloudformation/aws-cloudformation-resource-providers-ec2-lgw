package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.getHandlerErrorForEc2Error;
import static org.assertj.core.api.Assertions.assertThat;

public class TranslatorTest {
    @Test
    public void testGetHandlerErrorForEc2Error() {
        assertThat(getHandlerErrorForEc2Error("UnauthorizedOperation")).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(getHandlerErrorForEc2Error("InvalidParameter")).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(getHandlerErrorForEc2Error("AnythingElse")).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }
}
