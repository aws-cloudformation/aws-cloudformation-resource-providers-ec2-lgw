package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.List;

import static com.amazonaws.ec2.localgatewayroutetable.CallbackContext.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetable.Translator.getHandlerErrorForEc2Error;

class EventGenerator {
    private EventGenerator() {
    }

    static ProgressEvent<ResourceModel, CallbackContext> createSuccessEvent(ResourceModel model) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    static ProgressEvent<ResourceModel, CallbackContext> createSuccessEventForMultipleModels(List<ResourceModel> models) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    static ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model, CallbackContext context) {
        return createInProgressEvent(model, context, POLLING_DELAY_SECONDS);
    }

    static ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model, CallbackContext context, int pollingDelaySeconds) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(context)
                .callbackDelaySeconds(pollingDelaySeconds)
                .status(OperationStatus.IN_PROGRESS)
                .resourceModel(model)
                .build();
    }

    static ProgressEvent<ResourceModel, CallbackContext> createFailedEvent(ResourceModel model, Ec2Exception e) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                .message(e.getMessage())
                .build();
    }

    static ProgressEvent<ResourceModel, CallbackContext> createFailedReadOnlyPropertyEvent(ResourceModel model, String readOnlyProperty) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InvalidRequest)
                .message("Cannot set read-only property " + readOnlyProperty)
                .build();
    }

    static ProgressEvent<ResourceModel, CallbackContext> createNotUpdatableEvent(ResourceModel model, String nonUpdatableProperty) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .message("Cannot update not updatable property " + nonUpdatableProperty)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.NotUpdatable)
                .build();
    }
}
