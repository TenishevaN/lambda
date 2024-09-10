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

import com.fasterxml.jackson.core.JsonProcessingException;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Field;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;


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
            .region(Region.EU_CENTRAL_1)
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
                    .tableName("cmtr-85e8c71a-Weather")
                    .build());
            System.out.println("Table already exists.");
        } catch (ResourceNotFoundException e) {

            createTable();
        }
    }

    private static void createTable() {
        try {
            dynamoDB.createTable(CreateTableRequest.builder()
                    .tableName("cmtr-85e8c71a-Weather")
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
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


        StringBuilder rawJsonForecast = new StringBuilder();
        rawJsonForecast.append("{");
        rawJsonForecast.append("\"latitude\":").append(forecast.getLatitude()).append(",");
        rawJsonForecast.append("\"longitude\":").append(forecast.getLongitude()).append(",");
        rawJsonForecast.append("\"generationtime_ms\":").append(forecast.getGenerationTimeMs()).append(",");
        rawJsonForecast.append("\"utc_offset_seconds\":").append(forecast.getUtcOffsetSeconds()).append(",");
        rawJsonForecast.append("\"timezone\":\"").append(forecast.getTimezone()).append("\",");
        rawJsonForecast.append("\"timezone_abbreviation\":\"").append(forecast.getTimezoneAbbreviation()).append("\",");
        rawJsonForecast.append("\"elevation\":").append(forecast.getElevation()).append(",");

        String temperatureUnit = forecast.getHourlyUnits().get("temperature_2m");
        String timeUnit = forecast.getHourlyUnits().get("time");
      //  rawJsonForecast.append("\"hourly_units\":{\"temperature_2m\":\"").append(temperatureUnit).append("\",\"time\":\"").append(timeUnit).append("\"},");
        rawJsonForecast.append("\"hourly_units\":{\"time\":\"").append(timeUnit).append("\",\"temperature_2m\":\"").append(temperatureUnit).append("\"},");

        rawJsonForecast.append("\"hourly\":").append(convertMapForHourly(forecast.getHourly()));
        rawJsonForecast.append("}");

        try {
            AmazonDynamoDBAsync client = AmazonDynamoDBAsyncClientBuilder.standard()
                    .withRegion(Regions.EU_CENTRAL_1)
                    .build();


            DynamoDB dynamoDB = new DynamoDB(client);


            Table table = dynamoDB.getTable("cmtr-85e8c71a-Weather");

            ObjectMapper objectMapper = new ObjectMapper();


            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            HashMap<String, Object> forecastMap = objectMapper.readValue(rawJsonForecast.toString(), typeRef);

            System.out.println("inserted forecastMap: " + forecastMap);
            var id = UUID.randomUUID().toString().replace("=", "");
            System.out.println("id: " + id);
            table.putItem(new Item()
                    .withPrimaryKey("id", id)
                    .with("forecast", forecastMap));
            System.out.println("Item inserted successfully.");
        } catch (Exception e) {


            System.err.println("Error inserting item into table: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static String mapToJson(Map<String, String> map) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }




    private static String convertMapForHourly(Map<String, List<Object>> originalMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object[]> hourlyData = new HashMap<>();

        String[] temperatureValues = convertListToStringArray(originalMap.get("temperature_2m"));
        String[] timeValues = convertListToStringArray(originalMap.get("time"));

        if (temperatureValues != null && timeValues != null) {
            hourlyData.put("temperature_2m", temperatureValues);
            hourlyData.put("time", timeValues);
        } else {
            System.out.println("Error: Temperature or Time data is missing.");
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(hourlyData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.out.println("Failed to serialize hourly data. Temperature List: " + temperatureValues + ", Time List: " + timeValues);
            return "{}";
        }
    }

    private static String[] convertListToStringArray(List<String> list) {
        if (list != null) {
            return list.stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
        }
        return null;
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
        return buildResponse(404, "The resource with method and path is not found with method:" + getMethod(requestEvent) +
                " and path:  " + getPath(requestEvent));
    }

}