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


import java.lang.reflect.Field;
import com.fasterxml.jackson.core.JsonProcessingException;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.lang.reflect.Field;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;

import com.amazonaws.regions.Regions;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1) // Set the appropriate region
            .build();


    private OpenMeteoClient openMeteoClient = new OpenMeteoClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
    private final Map<RouteKey, Function<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>> routeHandlers = Map.of(
            new RouteKey("GET", "/"), this::handleGetWeather,
            new RouteKey("GET", ""), this::handleGetWeather);

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
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType(ScalarAttributeType.S) // The primary key
                                    .build()
                    )
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH) // 'id' is the hash key
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(1L)
                            .writeCapacityUnits(1L)
                            .build())
                    .build());

            System.out.println("Table created successfully.");
        } catch (DynamoDbException e) {
            AWSXRay.getCurrentSegment().addException(e);
            System.err.println("Unable to create table: " + e.getMessage());
        }
    }

    private static void putItem(WeatherForecast forecast) {

        try {
            AmazonDynamoDBAsync client = AmazonDynamoDBAsyncClientBuilder.standard()
                    .withRegion(Regions.EU_CENTRAL_1)
                    .build();
            DynamoDB dynamoDB = new DynamoDB(client);
            Table table = dynamoDB.getTable("cmtr-85e8c71a-Weather-test");

            StringBuilder rawJsonForecast = new StringBuilder();
            rawJsonForecast.append("{");
            rawJsonForecast.append("\"latitude\":").append(forecast.getLatitude()).append(",");
            rawJsonForecast.append("\"longitude\":").append(forecast.getLongitude()).append(",");
            rawJsonForecast.append("\"generationtime_ms\":").append(forecast.getGenerationTimeMs()).append(",");
            rawJsonForecast.append("\"utc_offset_seconds\":").append(forecast.getUtcOffsetSeconds()).append(",");
            rawJsonForecast.append("\"timezone\":\"").append(forecast.getTimezone()).append("\",");
            rawJsonForecast.append("\"timezone_abbreviation\":\"").append(forecast.getTimezoneAbbreviation()).append("\",");
            rawJsonForecast.append("\"elevation\":").append(forecast.getElevation()).append(",");
            rawJsonForecast.append("\"hourly_units\":").append(mapToJson(forecast.getHourlyUnits())).append(",");
            rawJsonForecast.append("\"hourly\":").append(mapToJson(forecast.getHourly()));
            rawJsonForecast.append("}");

            ObjectMapper objectMapper = new ObjectMapper();
            HashMap<String, Object> forecastMap = objectMapper.readValue(rawJsonForecast.toString(), new TypeReference<HashMap<String, Object>>() {});

            table.putItem(new Item()
                    .withPrimaryKey("id", UUID.randomUUID().toString())
                    .withMap("forecast", forecastMap));
            System.out.println("Item inserted successfully.");
        } catch (Exception e) {
            System.err.println("Error inserting item into table: " + e.getMessage());
            Thread.currentThread().interrupt();
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

    private static String mapToJson(Map<String, String> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private static String mapToListsToJson(Map<String, List<Object>> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        boolean first = true;
        for (Map.Entry<String, List<Object>> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("[");
            boolean firstItem = true;
            for (Object item : entry.getValue()) {
                if (!firstItem) json.append(",");
                if (item instanceof String) {
                    json.append("\"").append(item).append("\"");
                } else {
                    json.append(item);
                }
                firstItem = false;
            }
            json.append("]");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private static String mapToJsonForHourly(Map<String, List<Object>> map) {
        var ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // Return empty JSON object in case of error
        }
    }


    private static Map<String, Object> forecastToMap(WeatherForecast forecast) {
        Map<String, Object> forecastMap = new HashMap<>();
        Field[] fields = forecast.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true); // You might need this if fields are private
            try {
                Object value = field.get(forecast);
                if (value != null) {
                    forecastMap.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                System.err.println("Error accessing field: " + e.getMessage());
            }
        }
        return forecastMap;
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