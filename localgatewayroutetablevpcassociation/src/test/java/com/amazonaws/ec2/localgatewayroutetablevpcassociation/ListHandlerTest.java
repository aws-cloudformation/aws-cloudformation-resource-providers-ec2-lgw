package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTableVpcAssociationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static com.amazonaws.ec2.localgatewayroutetablevpcassociation.Translator.createModelFromAssociation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final DescribeLocalGatewayRouteTableVpcAssociationsResponse responseWithToken = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(PENDING_ASSOCIATION)
            .nextToken("token")
            .build();

        final DescribeLocalGatewayRouteTableVpcAssociationsResponse responseWithAssociation = DescribeLocalGatewayRouteTableVpcAssociationsResponse
            .builder()
            .localGatewayRouteTableVpcAssociations(TEST_ASSOCIATION)
            .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
            .thenReturn(responseWithToken)
            .thenReturn(responseWithAssociation);

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(
            Arrays.asList(
                createModelFromAssociation(PENDING_ASSOCIATION),
                createModelFromAssociation(TEST_ASSOCIATION)
            )
        );
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
