package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLocalGatewayRouteTableVpcAssociationRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.util.Set;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.getHandlerErrorForEc2Error;
import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.createModelFromAssociation;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        final Ec2Client client = ClientBuilder.getClient(logger);
        if (callbackContext == null || !callbackContext.isCreateStarted()) {
            // Return InvalidRequest if caller is attempting to set a read-only property
            if (model.getLocalGatewayId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayId");
            }
            if (model.getLocalGatewayRouteTableVpcAssociationId() != null) {
                return createFailedReadOnlyPropertyEvent(model, "LocalGatewayRouteTableVpcAssociationId");
            }
            if (model.getState() != null) {
                return createFailedReadOnlyPropertyEvent(model, "State");
            }
            try {
                model = createAssociation(model.getLocalGatewayRouteTableId(),
                        model.getVpcId(),
                        TagHelper.getAllResourceTags(request),
                        proxy,
                        client);
            } catch (Ec2Exception e) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                    .message(e.getMessage())
                    .build();
            }
            request.setDesiredResourceState(model);
            return createInProgressEvent(model, 0);
        }

        final ReadHandler readHandler = new ReadHandler();
        final ResourceModel resultModel;
        try {
            resultModel = readHandler.handleRequest(proxy, request, null, logger).getResourceModel();
            if (!"associated".equals(resultModel.getState())) {
                return createInProgressEvent(resultModel);
            }
        } catch (CfnNotFoundException e) {
            return createInProgressEvent(model);
        } catch (Ec2Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(getHandlerErrorForEc2Error(e.awsErrorDetails().errorCode()))
                .message(e.getMessage())
                .build();
        }
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(resultModel)
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private ResourceModel createAssociation(
        final String localGatewayRouteTableId,
        final String vpcId,
        final Set<Tag> tags,
        final AmazonWebServicesClientProxy proxy,
        final Ec2Client client) {

        final CreateLocalGatewayRouteTableVpcAssociationRequest.Builder request = CreateLocalGatewayRouteTableVpcAssociationRequest
            .builder()
            .localGatewayRouteTableId(localGatewayRouteTableId)
            .vpcId(vpcId);
        if (tags != null && !tags.isEmpty()) {
            request.tagSpecifications(TagSpecification.builder().resourceType("local-gateway-route-table-vpc-association").tags(TagHelper.createSdkTagsFromCfnTags(tags)).build());
        }
        try {
            return createModelFromAssociation(proxy.injectCredentialsAndInvokeV2(request.build(), client::createLocalGatewayRouteTableVpcAssociation)
                .localGatewayRouteTableVpcAssociation());
        } catch (Ec2Exception e) {
            if ("LocalGatewayRouteTableVpcAssociationAlreadyExists".equals(e.awsErrorDetails().errorCode())) {
                throw new CfnAlreadyExistsException("LocalGatewayRouteTableVPCAssociation", "localGatewayRouteTableId, vpcId: "
                    + localGatewayRouteTableId + ", " + vpcId);
            }
            throw e;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model) {
        return createInProgressEvent(model, POLLING_DELAY_SECONDS);
    }

    private ProgressEvent<ResourceModel, CallbackContext> createInProgressEvent(ResourceModel model, int callbackDelay) {
        CallbackContext context = CallbackContext.builder()
            .createStarted(true)
            .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(context)
            .callbackDelaySeconds(callbackDelay)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> createFailedReadOnlyPropertyEvent(ResourceModel model, String readOnlyProperty) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InvalidRequest)
                .message("Cannot set read-only property " + readOnlyProperty)
                .build();
    }
}
