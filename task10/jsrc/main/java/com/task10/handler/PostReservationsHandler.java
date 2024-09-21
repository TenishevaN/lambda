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
       String tableName = System.getenv("reservations_table");

        if (!doesTableExist(tableName)) {
            logger.error("Table does not exist: " + tableName);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"Reservation table doesn't exist.\"}");
        }

        try {
            logger.info("passed validation ");

            DynamoDbClient dynamoDB = DynamoDbClient.create();

            JSONObject requestBody = new JSONObject(requestEvent.getBody());

            String clientName = requestBody.getString("clientName");
            String phoneNumber = requestBody.getString("phoneNumber");
            String dateString = requestBody.getString("date");
            String slotTimeStartString = requestBody.getString("slotTimeStart");
            String slotTimeEndString = requestBody.getString("slotTimeEnd");
            int tableNumber = requestBody.getInt("tableNumber");

            String id = UUID.randomUUID().toString();

            logger.info("reservation id: " + id);

            if (isOverlappingReservation(tableName, dateString, slotTimeStartString, slotTimeEndString, tableNumber)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Reservation overlaps with an existing reservation.\"}");
            }


            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(String.valueOf(id)).build());
            item.put("tableNumber", AttributeValue.builder().n(String.valueOf(tableNumber)).build());
            item.put("clientName", AttributeValue.builder().s(clientName).build());
            item.put("phoneNumber", AttributeValue.builder().s(phoneNumber).build());
            item.put("date", AttributeValue.builder().s(String.valueOf(dateString)).build());
            item.put("slotTimeStart", AttributeValue.builder().s(String.valueOf(slotTimeStartString)).build());
            item.put("slotTimeEnd", AttributeValue.builder().s(String.valueOf(slotTimeEndString)).build());


            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
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

    private boolean doesTableExist(String tableName) {
        try {
            logger.info("Checking if table exists: " + tableName);
            dynamoDB.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
            return true;
        } catch (Exception e) {
            logger.error("Table does not exist: " + e.getMessage());
            return false;
        }
    }

    private boolean isOverlappingReservation(String tableName, String date, String startTime, String endTime, int tableNumber) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("tableNumber = :tableNumber AND date = :date")
                .expressionAttributeValues(Map.of(
                        ":tableNumber", AttributeValue.builder().n(String.valueOf(tableNumber)).build(),
                        ":date", AttributeValue.builder().s(date).build()
                ))
                .build();

        QueryResponse queryResponse = dynamoDB.query(queryRequest);
        for (Map<String, AttributeValue> item : queryResponse.items()) {
            String existingStartTime = item.get("slotTimeStart").s();
            String existingEndTime = item.get("slotTimeEnd").s();
            if (timeOverlap(startTime, endTime, existingStartTime, existingEndTime)) {
                return true;
            }
        }
        return false;
    }

    private boolean timeOverlap(String start1, String end1, String start2, String end2) {
        return (start1.compareTo(end2) < 0 && end1.compareTo(start2) > 0);
    }

}