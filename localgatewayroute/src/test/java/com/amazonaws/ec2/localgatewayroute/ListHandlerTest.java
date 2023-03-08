package com.amazonaws.ec2.localgatewayroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLocalGatewayRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
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
            .routes(Collections.singletonList(PENDING_VIF_GROUP_ROUTE))
            .nextToken("token")
            .build();

        final SearchLocalGatewayRoutesResponse responseWithoutToken = SearchLocalGatewayRoutesResponse
            .builder()
            .routes(Collections.singletonList(ACTIVE_VIF_GROUP_ROUTE))
            .build();


        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponsewithToken = DescribeLocalGatewayRouteTablesResponse
            .builder()
            .localGatewayRouteTables(Collections.singletonList(VIF_GROUP_RTB))
            .nextToken("token")
            .build();

        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponsewithoutToken = DescribeLocalGatewayRouteTablesResponse
            .builder()
            .localGatewayRouteTables(Collections.singletonList(VIF_GRP_RTB_2))
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
                createModelFromRoute(PENDING_VIF_GROUP_ROUTE),
                createModelFromRoute(ACTIVE_VIF_GROUP_ROUTE),
                createModelFromRoute(PENDING_VIF_GROUP_ROUTE),
                createModelFromRoute(ACTIVE_VIF_GROUP_ROUTE)
            )
        );

        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NoSearchRoutesPermissions() {
        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final SearchLocalGatewayRoutesResponse searchRoutesResponse = SearchLocalGatewayRoutesResponse
                .builder()
                .routes(Collections.singletonList(ACTIVE_VIF_GROUP_ROUTE))
                .build();

        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponse = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(Arrays.asList(VIF_GROUP_RTB, VIF_GRP_RTB_2))
                .build();

        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(DescribeLocalGatewayRouteTablesRequest.class), any()))
                .thenReturn(describeLocalGatewayRouteTablesResponse);
        final AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidLocalGatewayRouteTableID.NotFound")
                .build();
        final Ec2Exception notFoundException = (Ec2Exception) Ec2Exception
                .builder()
                .awsErrorDetails(errorDetails)
                .build();
        // Even if can't search the first route table, should just ignore and move onto the next
        Mockito.lenient().when(proxy.injectCredentialsAndInvokeV2(any(SearchLocalGatewayRoutesRequest.class), any()))
                .thenThrow(notFoundException)
                .thenReturn(searchRoutesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(
                Arrays.asList(
                        createModelFromRoute(ACTIVE_VIF_GROUP_ROUTE)
                )
        );

        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleEniRouteRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final SearchLocalGatewayRoutesResponse responseWithToken = SearchLocalGatewayRoutesResponse
                .builder()
                .routes(Collections.singletonList(PENDING_ENI_ROUTE))
                .nextToken("token")
                .build();

        final SearchLocalGatewayRoutesResponse responseWithoutToken = SearchLocalGatewayRoutesResponse
                .builder()
                .routes(Collections.singletonList(ACTIVE_ENI_ROUTE))
                .build();


        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponsewithToken = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(Collections.singletonList(ENI_RTB))
                .nextToken("token")
                .build();

        final DescribeLocalGatewayRouteTablesResponse describeLocalGatewayRouteTablesResponsewithoutToken = DescribeLocalGatewayRouteTablesResponse
                .builder()
                .localGatewayRouteTables(Collections.singletonList(ENI_RTB_2))
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
                        createModelFromRoute(PENDING_ENI_ROUTE),
                        createModelFromRoute(ACTIVE_ENI_ROUTE),
                        createModelFromRoute(PENDING_ENI_ROUTE),
                        createModelFromRoute(ACTIVE_ENI_ROUTE)
                )
        );
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
