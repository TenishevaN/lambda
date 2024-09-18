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

        try {

            DynamoDbClient dynamoDB = DynamoDbClient.create();
            if (!doesTableExist(dynamoDB, "cmtr-85e8c71a-Reservations-test")) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("DynamoDB table does not exist.");
            }



            JSONObject requestBody = new JSONObject(requestEvent.getBody());

            String clientName = requestBody.getString("clientName");
            String phoneNumber = requestBody.getString("phoneNumber");
            String dateString = requestBody.getString("date");
            String slotTimeStartString = requestBody.getString("slotTimeStart");
            String slotTimeEndString = requestBody.getString("slotTimeEnd");
            int tableNumber = requestBody.getInt("tableNumber");

            String id = UUID.randomUUID().toString();

            if (hasOverlappingReservation(dynamoDB, tableNumber, dateString)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("here is an overlapping reservation.");
            }

            logger.info("passed validation ");


            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(String.valueOf(id)).build());
            item.put("tableNumber", AttributeValue.builder().n(String.valueOf(tableNumber)).build());
            item.put("clientName", AttributeValue.builder().s(clientName).build());
            item.put("phoneNumber", AttributeValue.builder().s(phoneNumber).build());
            item.put("date", AttributeValue.builder().s(String.valueOf(dateString)).build());
            item.put("slotTimeStart", AttributeValue.builder().s(String.valueOf(slotTimeStartString)).build());
            item.put("slotTimeEnd", AttributeValue.builder().s(String.valueOf(slotTimeEndString)).build());


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

    private boolean doesTableExist(DynamoDbClient dynamoDB, String tableName) {
        try {
            dynamoDB.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private boolean hasOverlappingReservation(DynamoDbClient dynamoDB, int tableNumber, String date) {
        String tableName = "cmtr-85e8c71a-Reservations-test";

        // Construct the condition to find reservations for the same table on the same date that overlap in time
        String keyConditionExpression = "tableId = :tableId and date = :date";
        String filterExpression = "slotTimeStart < :endTime and slotTimeEnd > :startTime";

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":tableNumber", AttributeValue.builder().n(String.valueOf(tableNumber)).build());
        expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression(keyConditionExpression)
                .filterExpression(filterExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        QueryResponse queryResponse = dynamoDB.query(queryRequest);

        // If the query returns any items, there is an overlapping reservation
        return !queryResponse.items().isEmpty();
    }

}