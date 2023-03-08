package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.Arrays;

import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.CallbackContext.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createFailedReadOnlyPropertyEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createNotUpdatableEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEvent;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.EventGenerator.createSuccessEventForMultipleModels;
import static com.amazonaws.ec2.localgatewayroutetablevifgroupassociation.Translator.getHandlerErrorForEc2Error;
import static org.assertj.core.api.Assertions.assertThat;

public class EventGeneratorTest extends TestBase {

    private ResourceModel model = ResourceModel.builder()
            .localGatewayVirtualInterfaceGroupId(VIF_GROUP_ID)
            .localGatewayRouteTableId(ROUTE_TABLE_ID)
            .build();

    @Test
    public void testCreateSuccessEvent() {
        ProgressEvent<ResourceModel, CallbackContext> expectedEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
        assertThat(createSuccessEvent(model)).isEqualTo(expectedEvent);
    }

    @Test
    public void testCreateSuccessEventForMultipleModels() {
        ProgressEvent<ResourceModel, CallbackContext> expectedEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Arrays.asList(model, model))
                .status(OperationStatus.SUCCESS)
                .build();
        assertThat(createSuccessEventForMultipleModels(Arrays.asList(model, model))).isEqualTo(expectedEvent);
    }

    @Test
    public void testCreateInProgressEvent() {
        CallbackContext context = new CallbackContext();
        ProgressEvent<ResourceModel, CallbackContext> expectedEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(context)
                .callbackDelaySeconds(POLLING_DELAY_SECONDS)
                .status(OperationStatus.IN_PROGRESS)
                .resourceModel(model)
                .build();
        assertThat(createInProgressEvent(model, context)).isEqualTo(expectedEvent);
    }

    @Test
    public void testCreateFailedEvent() {
        String errorCode = "error code";
        String errorMessage = "error message";
        AwsErrorDetails errorDetails = AwsErrorDetails.builder().errorCode(errorCode).errorMessage(errorMessage).build();
        Ec2Exception exception = (Ec2Exception) Ec2Exception.builder().awsErrorDetails(errorDetails).build();
        ProgressEvent<ResourceModel, CallbackContext> expectedEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(getHandlerErrorForEc2Error(exception.awsErrorDetails().errorCode()))
                .message(exception.getMessage())
                .build();
        assertThat(createFailedEvent(model, exception)).isEqualTo(expectedEvent);
    }

    @Test
    public void testCreateFailedEventReadOnlyProperty() {
        String readOnlyProperty = "State";
        ProgressEvent<ResourceModel, CallbackContext> expectedEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InvalidRequest)
                .message("Cannot set read-only property " + readOnlyProperty)
                .build();
        assertThat(createFailedReadOnlyPropertyEvent(model, readOnlyProperty)).isEqualTo(expectedEvent);
    }

    @Test
    public void testCreateNotUpdatableEvent() {
        String nonUpdatableProperty = "State";
        ProgressEvent<ResourceModel, CallbackContext> expectedEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .message("Cannot update not updatable property " + nonUpdatableProperty)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.NotUpdatable)
                .build();
        assertThat(createNotUpdatableEvent(model, nonUpdatableProperty)).isEqualTo(expectedEvent);
    }
}
