package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class TagHelper {

    /*
     * Aggregates the various tags that can come in a request:
     * 1. Tags specified on the resource itself
     * 2. Tags specified on the stack
     */
    public static Set<Tag> getAllResourceTags(ResourceHandlerRequest<ResourceModel> request) {
        Set<Tag> tags = new HashSet<>();
        if (request.getDesiredResourceState().getTags() != null) {
            tags.addAll(request.getDesiredResourceState().getTags());
        }
        if (request.getDesiredResourceTags() != null) {
            tags.addAll(tagsFromCfnRequestTags(request.getDesiredResourceTags()));
        }
        return tags;
    }

    /*
     * A few different translator methods below. There's three "tag shapes" we are working with:
     * 1. EC2 SDK Tags, used in EC2 SDK API requests and responses
     * 2. Cloudformation tags for the CFN resource
     * 3. Tags in a Cloudformation request (represented as a Map<String, String>)
     */

    public static Set<Tag> tagsFromCfnRequestTags(Map<String, String> requestTags) {
        return requestTags.entrySet().stream()
                .map(tag -> Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    public static software.amazon.awssdk.services.ec2.model.Tag createSdkTagFromCfnTag(final Tag tag) {
        return software.amazon.awssdk.services.ec2.model.Tag.builder()
                .key(tag.getKey())
                .value(tag.getValue())
                .build();
    }

    public static Set<software.amazon.awssdk.services.ec2.model.Tag> createSdkTagsFromCfnTags(final Set<Tag> tags) {
        return tags.stream().map(tag -> createSdkTagFromCfnTag(tag)).collect(Collectors.toSet());
    }

    public static Tag createCfnTagFromSdkTag(final software.amazon.awssdk.services.ec2.model.Tag tag) {
        return Tag.builder()
                .key(tag.key())
                .value(tag.value())
                .build();
    }

    public static Set<Tag> createCfnTagsFromSdkTags(final Set<software.amazon.awssdk.services.ec2.model.Tag> tags) {
        return tags.stream().map(tag -> createCfnTagFromSdkTag(tag)).collect(Collectors.toSet());
    }
}