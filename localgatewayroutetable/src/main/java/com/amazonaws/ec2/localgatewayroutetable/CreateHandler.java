package com.amazonaws.ec2.localgatewayroutetable;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetable.EventGenerator.createFailedEvent;
import static com.amazonaws.ec2.localgatewayroutetable.EventGenerator.createFailedReadOnlyPropertyEvent;
import static com.amazonaws.ec2.localgatewayroutetable.EventGenerator.createInProgressEvent;
import static com.amazonaws.ec2.localgatewayroutetable.EventGenerator.createSuccessEvent;
import static com.amazonaws.ec2.localgatewayroutetable.Translator.createModelFromRouteTable;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private static final List<String> CREATED_LGW_RTB_STATES = Arrays.asList("available", "inactive");

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel resultModel;

        final Ec2Client client = ClientBuilder.getClient(logger);

        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            // Return InvalidRequest if caller is attempting to set a read-only property
            if (model.getLocalGatewayRouteTableId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayRouteTableId");
            }
            if (model.getLocalGatewayRouteTableArn() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayRouteTableArn");
            }
            if (model.getOutpostArn() != null) {
                return createFailedReadOnlyPropertyEvent(model, "OutpostArn");
            }
            if (model.getOwnerId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "OwnerId");
            }
            if (model.getState() != null) {
                return createFailedReadOnlyPropertyEvent(model, "State");
            }
            try {
                resultModel = createLocalGatewayRouteTable(
                        model.getLocalGatewayId(),
                        model.getMode(),
                        TagHelper.getAllResourceTags(request),
                        proxy,
                        client);
            } catch (Ec2Exception e) {
                return createFailedEvent(model, e);
            }
            return createInProgressEventForCreate(resultModel, 0);
        }

        final ReadHandler readHandler = new ReadHandler();
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            if (!CREATED_LGW_RTB_STATES.contains(resultModel.getState())) {
                logger.log("LGW route table is not in one of the 'created' states, " + CREATED_LGW_RTB_STATES +
                        ". Returning InProgress event. Current state=" + resultModel.getState());
                return createInProgressEventForCreate(resultModel);
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEventForCreate(model);
        } catch (Ec2Exception e) {
            return createFailedEvent(model, e);
        }

        return createSuccessEvent(resultModel);
    }

    private ResourceModel createLocalGatewayRouteTable(
            final String localGatewayId,
            final String mode,
            final Set<Tag> tags,
            final AmazonWebServicesClientProxy proxy,
            final Ec2Client client) {

        final CreateLocalGatewayRouteTableRequest.Builder createRequest = CreateLocalGatewayRouteTableRequest
                .builder()
                .localGatewayId(localGatewayId)
                .mode(mode);

        if (tags != null && !tags.isEmpty()) {
            createRequest.tagSpecifications(TagSpecification.builder().resourceType("local-gateway-route-table").tags(TagHelper.createSdkTagsFromCfnTags(tags)).build());
        }

        try {
            return createModelFromRouteTable(proxy.injectCredentialsAndInvokeV2(createRequest.build(), client::createLocalGatewayRouteTable).localGatewayRouteTable());
        } catch (Ec2Exception e) {
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForCreate(ResourceModel model) {
        return createInProgressEvent(model, CallbackContext.builder().createStarted(true).build());
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEventForCreate(ResourceModel model, int pollingDelaySeconds) {
        return createInProgressEvent(model, CallbackContext.builder().createStarted(true).build(), pollingDelaySeconds);
    }
}
