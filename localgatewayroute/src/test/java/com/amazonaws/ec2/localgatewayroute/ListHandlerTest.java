package com.amazonaws.ec2.localgatewayroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesRequest;
import software.amazon.awssdk.services.ec2.model.SearchLocalGatewayRoutesResponse;
import software.amazon.cloudformation.proxy.*;

import java.util.Arrays;
import java.util.Collections;

import static com.amazonaws.ec2.localgatewayroute.Translator.createModelFromRoute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

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

        final SearchLocalGatewayRoutesResponse responseWithToken = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(Collections.singletonList(pendingRoute))
            .nextToken("token")
            .build();

        final SearchLocalGatewayRoutesResponse responseWithoutToken = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(Collections.singletonList(activeRoute))
            .build();


        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponsewithToken = DescribeLocalGatewayRouteTablesResponse
            .builder()
            .localGatewayRouteTables(Collections.singletonList(routeTableOne))
            .nextToken("token")
            .build();

        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponsewithoutToken = DescribeLocalGatewayRouteTablesResponse
            .builder()
            .localGatewayRouteTables(Collections.singletonList(routeTableTwo))
            .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTablesRequest.class), any()))
            .thenReturn(describeLocalGatewayRouteTablesResponsewithToken, describeLocalGatewayRouteTablesResponsewithoutToken);
        // first two for routeTableOne, last two for routeTableTwo:
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
            .thenReturn(responseWithToken, responseWithoutToken, responseWithToken, responseWithoutToken);

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(
            Arrays.asList(
                createModelFromRoute(pendingRoute),
                createModelFromRoute(activeRoute),
                createModelFromRoute(pendingRoute),
                createModelFromRoute(activeRoute)
            )
        );

        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
