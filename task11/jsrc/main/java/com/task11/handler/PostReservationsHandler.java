package com.task11.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

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

import org.json.JSONArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;


import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class PostReservationsHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
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
        String tablesTableName = System.getenv("tables_table");

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

            if(!doesTablesTableExist(tablesTableName, tableNumber)){
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Table does not exist: " + tableName);
            }

            String id = UUID.randomUUID().toString();

            logger.info("reservation id: " + id);

            if (checkForOverlappingReservations(tableName, id, tableNumber, dateString, slotTimeStartString, slotTimeEndString)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Reservation overlaps with an existing reservation.");
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
        logger.info("if table exist: " + tableName);
        try {
            DescribeTableResponse describeTableResponse = dynamoDB.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
            logger.info("DescribeTableResponse: " + describeTableResponse);
            return describeTableResponse != null && describeTableResponse.table() != null;
        } catch (ResourceNotFoundException e) {
            logger.info("Table does not exist: " + tableName, e);
            return false;
        } catch (Exception e) {
            logger.error("Error checking if table exists: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean doesTablesTableExist(String tableName, int tableNumber) {

       boolean tableExist = false;

        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build();

            ScanResponse result = dynamoDB.scan(scanRequest);

            for (Map<String, AttributeValue> item : result.items()) {
                AttributeValue tableNumberAttr = item.get("number");
                int tableNumberN = Integer.parseInt(tableNumberAttr.n());
                if (tableNumberN == tableNumber) {
                    tableExist = true;
                }
            }
            return tableExist;
        } catch (Exception e) {
            return false;
        }

    }

    private boolean checkForOverlappingReservations(String tableName, String id, int tableNumber, String date, String startTime, String endTime) {
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build();

            logger.info("Check overloap with: id = " + id + ", tableNumber= " + tableNumber + ", date = " + date + ", startTime=" + startTime);

            ScanResponse result = dynamoDB.scan(scanRequest);
            for (Map<String, AttributeValue> item : result.items()) {
                AttributeValue tableNumberAttr = item.get("tableNumber");
                if (tableNumberAttr == null || tableNumberAttr.n() == null) {
                    continue;
                }
                int tableNumberN = Integer.parseInt(tableNumberAttr.n());

                String dateV = safeGetString(item, "date");
                String startTimeV = safeGetString(item, "startTime");
                String endTimeV = safeGetString(item, "endTime");

                logger.info("actual: tableNumber= " + tableNumberN + ", date = " + dateV + ", startTime=" + startTimeV);


                if ((tableNumber == tableNumberN) && (date.equals(dateV))) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing request: " + e.getMessage());
            return false;
        }
        return false;
    }

    private String safeGetString(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value == null ? null : value.s();
    }
}