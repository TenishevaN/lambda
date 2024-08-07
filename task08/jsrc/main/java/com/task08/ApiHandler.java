package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.openmeteo.sdk.OpenMeteoClient;
import com.openmeteo.sdk.WeatherForecast;
import java.util.function.Function;

import java.util.Map;

@LambdaHandler(
        lambdaName = "api_handler",
        roleName = "api_handler-role",
        layers = {"sdk-layer"},
        runtime = DeploymentRuntime.JAVA11,
        isPublishVersion = false,
        architecture = Architecture.ARM64,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
        layerName = "sdk-layer",
        libraries = {"lib/open-meteo-sdk-1.0-SNAPSHOT.jar"},
        runtime = DeploymentRuntime.JAVA11,
        architectures = {Architecture.ARM64},
        artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private OpenMeteoClient openMeteoClient = new OpenMeteoClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
    private final Map<RouteKey, Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>> routeHandlers = Map.of(
            new RouteKey("GET", "/weather"), this::handleGetWeather);

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent requestEvent, Context context) {
        RouteKey routeKey = new RouteKey(getMethod(requestEvent), getPath(requestEvent));
        return routeHandlers.getOrDefault(routeKey, this::notFoundResponse).apply(requestEvent);
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, Object body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(responseHeaders)
                .withBody(gson.toJson(body))
                .build();
    }

    private APIGatewayV2HTTPResponse handleGetWeather(APIGatewayV2HTTPEvent requestEvent) {
        WeatherForecast forecast = openMeteoClient.getLatestForecast();
        return buildResponse(200, forecast);
    }

    private String getMethod(APIGatewayV2HTTPEvent requestEvent) {
        if (requestEvent != null && requestEvent.getRequestContext() != null && requestEvent.getRequestContext().getHttp() != null) {
            return requestEvent.getRequestContext().getHttp().getMethod();
        }
        return null;
    }

    private String getPath(APIGatewayV2HTTPEvent requestEvent) {
        if (requestEvent != null && requestEvent.getRequestContext() != null && requestEvent.getRequestContext().getHttp() != null) {
            return requestEvent.getRequestContext().getHttp().getPath();
        }
        return null;
    }

    private APIGatewayV2HTTPResponse notFoundResponse(APIGatewayV2HTTPEvent requestEvent) {
        return buildResponse(404, "The resource with method and path is not found with method:"+  getMethod(requestEvent) +
                " and path:  " + getPath(requestEvent));
    }

}