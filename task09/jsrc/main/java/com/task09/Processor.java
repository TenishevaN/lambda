package com.task09;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import software.amazon.awssdk.core.client.builder.SdkClientBuilder;

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
import java.util.*;
import com.amazonaws.xray.AWSXRay;

import com.syndicate.deployment.model.TracingMode;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



@LambdaHandler(
        lambdaName = "processor",
        roleName = "processor-role",
        layers = {"sdk-layer"},
        runtime = DeploymentRuntime.JAVA11,
        isPublishVersion = false,
        architecture = Architecture.ARM64,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
        tracingMode = TracingMode.Active
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
public class Processor implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {


    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1) // Set the appropriate region
            .build();


    private OpenMeteoClient openMeteoClient = new OpenMeteoClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
    private final Map<RouteKey, Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>> routeHandlers = Map.of(
            new RouteKey("GET", "/"), this::handleGetWeather);

    public Processor() {
        ensureTableExists();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent requestEvent, Context context) {
        RouteKey routeKey = new RouteKey(getMethod(requestEvent), getPath(requestEvent));
        return routeHandlers.getOrDefault(routeKey, this::notFoundResponse).apply(requestEvent);
    }

    private static void ensureTableExists() {

        try {
            dynamoDB.describeTable(DescribeTableRequest.builder()
                    .tableName("cmtr-85e8c71a-Weather-test")
                    .build());
            System.out.println("Table already exists.");
        } catch (ResourceNotFoundException e) {

            createTable();
        }
    }

    private static void createTable() {

        try {
            dynamoDB.createTable(CreateTableRequest.builder()
                    .tableName("cmtr-85e8c71a-Weather-test")
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(1L)
                            .writeCapacityUnits(1L)
                            .build())
                    .build());

        } catch (DynamoDbException e) {
            AWSXRay.getCurrentSegment().addException(e);
            System.err.println("Unable to create table: " + e.getMessage());
        }
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
        putItem(forecast);
        return buildResponse(200, forecast);
    }



    private static void putItem(WeatherForecast forecast) {
        String uniqueID = UUID.randomUUID().toString();
        Gson gson = new Gson();

        // Convert the forecast object to a JSON string, then to a Map
        String jsonForecast = gson.toJson(forecast);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(uniqueID).build());
        item.put("forecast", AttributeValue.builder().also(jsonForecast).build());

        try {
            dynamoDB.putItem(PutItemRequest.builder()
                    .tableName("cmtr-85e8c71a-Weather-test")
                    .item(item)
                    .build());
            System.out.println("Item inserted successfully.");
        } catch (DynamoDbException e) {
            System.err.println("Error inserting item into table: " + e.getMessage());
        }
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