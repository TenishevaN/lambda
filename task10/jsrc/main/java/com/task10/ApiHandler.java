package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.task10.dto.RouteKey;
import com.task10.handler.GetRootHandler;
import com.task10.handler.GetSecuredHandler;
import com.task10.handler.GetTablesdHandler;
import com.task10.handler.GetTableByIdHandler;
import com.task10.handler.PostTablesdHandler;
import com.task10.handler.GetReservationsHandler;
import com.task10.handler.PostReservationsHandler;
import com.task10.handler.RouteNotImplementedHandler;
import com.task10.handler.PostSignInHandler;
import com.task10.handler.PostSignUpHandler;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID;

import com.amazonaws.xray.AWSXRay;

import com.syndicate.deployment.model.TracingMode;

@DependsOn(resourceType = ResourceType.COGNITO_USER_POOL, name = "${booking_userpool}")
@LambdaHandler(
    lambdaName = "api_handler",
	roleName = "api-handler-role",
	isPublishVersion = false,
	runtime = DeploymentRuntime.JAVA17,
	tracingMode = TracingMode.Active
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "REGION", value = "${region}"),
		@EnvironmentVariable(key = "COGNITO_ID", value = "${booking_userpool}", valueTransformer = USER_POOL_NAME_TO_USER_POOL_ID),
		@EnvironmentVariable(key = "CLIENT_ID", value = "${booking_userpool}", valueTransformer = USER_POOL_NAME_TO_CLIENT_ID),
		@EnvironmentVariable(key = "reservations_table", value = "${reservations_table}"),
		@EnvironmentVariable(key = "tables_table", value = "${tables_table}")
})
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final CognitoIdentityProviderClient cognitoClient;
	private final Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlersByRouteKey;
	private final Map<String, String> headersForCORS;
	private final RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> routeNotImplementedHandler;

	private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);

	public ApiHandler() {
		this.cognitoClient = initCognitoClient();
		this.handlersByRouteKey = initHandlers();
		this.headersForCORS = initHeadersForCORS();
		this.routeNotImplementedHandler = new RouteNotImplementedHandler();
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
		return getHandler(requestEvent)
				.handleRequest(requestEvent, context)
				.withHeaders(headersForCORS);
	}

	private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler(APIGatewayProxyRequestEvent requestEvent) {
		return handlersByRouteKey.getOrDefault(getRouteKey(requestEvent), routeNotImplementedHandler);
	}

	private RouteKey getRouteKey(APIGatewayProxyRequestEvent requestEvent) {
		String path = requestEvent.getPath();
		logger.info("path: " + path);
		if (path.matches("/tables/\\d+")) {
			logger.info("path matches \\d: " + path);
			return new RouteKey(requestEvent.getHttpMethod(), "/tables/{tableId}");
		}
		return new RouteKey(requestEvent.getHttpMethod(), requestEvent.getPath());
	}

	private CognitoIdentityProviderClient initCognitoClient() {
		return CognitoIdentityProviderClient.builder()
				.region(Region.of(System.getenv("REGION")))
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}

	private Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> initHandlers() {

		return Map.of(
				new RouteKey("GET", "/"), new GetRootHandler(),
				new RouteKey("POST", "/signup"), new PostSignUpHandler(cognitoClient),
				new RouteKey("POST", "/signin"), new PostSignInHandler(cognitoClient),
				new RouteKey("POST", "/tables"), new PostTablesdHandler(cognitoClient),
				new RouteKey("GET", "/tables/{tableId}"), new GetTableByIdHandler(),
				new RouteKey("GET", "/tables"), new GetTablesdHandler(),
			    new RouteKey("POST", "/reservations"), new PostReservationsHandler(cognitoClient),
				new RouteKey("GET", "/reservations"), new GetReservationsHandler()
		);
	}

	private Map<String, String> initHeadersForCORS() {
		return Map.of(
				"Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
				"Access-Control-Allow-Origin", "*",
				"Access-Control-Allow-Methods", "*",
				"Accept-Version", "*"
		);
	}
}