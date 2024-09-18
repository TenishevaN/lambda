package com.task10.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.json.JSONArray;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class PostReservationsHandler  extends CognitoSupport  implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(GetTablesdHandler.class);

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();

    public PostReservationsHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        logger.info("Received request with HTTP method: {}", requestEvent.getHttpMethod());
        logger.info("Request path: {}", requestEvent.getPath());
        logger.info("Request body: {}", requestEvent.getBody());


        logger.info("Headers: {}", requestEvent.getHeaders());


        try {
            JSONObject requestBody = new JSONObject(requestEvent.getBody());

            String clientName = requestBody.getString("clientName");
            String phoneNumber = requestBody.getString("phoneNumber");
            String dateString = requestBody.getString("date");
            String slotTimeStartString = requestBody.getString("slotTimeStart");
            String slotTimeEndString = requestBody.getString("slotTimeEnd");
            int tableNumber = requestBody.getInt("tableNumber");

            // Check if the table exists
            if (!doesTableExist(tableNumber)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Table does not exist.\"}");
            }

            // Check for overlapping reservations
            if (hasOverlappingReservations(String.valueOf(tableNumber), dateString, slotTimeStartString, slotTimeEndString)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Reservation overlaps with an existing reservation.\"}");
            }

            logger.info("passed validation ");

            String id = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(String.valueOf(id)).build());
            item.put("tableNumber", AttributeValue.builder().n(String.valueOf(tableNumber)).build());
            item.put("clientName", AttributeValue.builder().s(clientName).build());
            item.put("phoneNumber", AttributeValue.builder().s(phoneNumber).build());
            item.put("date", AttributeValue.builder().s(String.valueOf(dateString)).build());
            item.put("slotTimeStart", AttributeValue.builder().s(String.valueOf(slotTimeStartString)).build());
            item.put("slotTimeEnd", AttributeValue.builder().s(String.valueOf(slotTimeEndString)).build());

            DynamoDbClient dynamoDB = DynamoDbClient.create();
            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName("cmtr-85e8c71a-Reservations-test")
                    .item(item)
                    .build();
            logger.info("defined data: " + item);

            PutItemResponse putItemResponse = dynamoDB.putItem(putItemRequest);

            JSONObject responseBody = new JSONObject()
                    .put("reservationId", id);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody.toString());
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(e.getMessage().toString());
        }
    }

    private boolean doesTableExist(int tableNumber) {
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName("cmtr-85e8c71a-Tables")
                .key(Map.of("tableNumber", AttributeValue.builder().n(String.valueOf(tableNumber)).build()))
                .build();

        GetItemResponse getItemResponse = dynamoDB.getItem(getItemRequest);
        return getItemResponse.hasItem();
    }

    private boolean hasOverlappingReservations(String tableNumber, String date, String slotTimeStart, String slotTimeEnd) {
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":tableNumber", AttributeValue.builder().n(tableNumber).build());
        expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());
        expressionAttributeValues.put(":slotTimeStart", AttributeValue.builder().s(slotTimeStart).build());
        expressionAttributeValues.put(":slotTimeEnd", AttributeValue.builder().s(slotTimeEnd).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName("cmtr-85e8c71a-Reservations")
                .keyConditionExpression("tableNumber = :tableNumber AND date = :date")
                .filterExpression("slotTimeStart < :slotTimeEnd AND slotTimeEnd > :slotTimeStart")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        QueryResponse queryResponse = dynamoDB.query(queryRequest);
        return !queryResponse.items().isEmpty();
    }

}