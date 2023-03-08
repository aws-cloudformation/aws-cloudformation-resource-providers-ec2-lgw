package com.amazonaws.ec2.localgatewayroutetable;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetable.Translator.getHandlerErrorForEc2Error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagHelperTest {
    @Test
    public void testGetAllResourceTags() {
        Map<String, String> stackTags = Collections.singletonMap("stackTagKey", "stackTagValue");
        Set<Tag> resourceLevelTags = Collections.singleton(Tag.builder().key("resourceTagKey").value("resourceTagValue").build());
        ResourceModel modelWithTags = ResourceModel.builder()
                .tags(resourceLevelTags)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelWithTags)
                .desiredResourceTags(stackTags)
                .build();
        Set<Tag> expectedTags = new HashSet<>();
        expectedTags.addAll(TagHelper.tagsFromCfnRequestTags(stackTags));
        expectedTags.addAll(resourceLevelTags);

        assertEquals(expectedTags, TagHelper.getAllResourceTags(request));
    }

    @Test
    public void testCreateSdkTagFromCfnTagAndReverse() {
        Set<Tag> cfnTags = Collections.singleton(Tag.builder().key("resourceTagKey").value("resourceTagValue").build());
        assertEquals(TagHelper.createCfnTagsFromSdkTags(TagHelper.createSdkTagsFromCfnTags(cfnTags)), cfnTags);
        Set<software.amazon.awssdk.services.ec2.model.Tag> sdkTags = Collections.singleton(software.amazon.awssdk.services.ec2.model.Tag.builder().key("resourceTagKey").value("resourceTagValue").build());
        assertEquals(TagHelper.createSdkTagsFromCfnTags(TagHelper.createCfnTagsFromSdkTags(sdkTags)), sdkTags);
    }
}