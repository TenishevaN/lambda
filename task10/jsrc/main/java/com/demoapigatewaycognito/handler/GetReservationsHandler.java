package com.task10.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.json.JSONObject;
import org.json.JSONArray;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.LinkedHashMap;


public class GetReservationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName("cmtr-85e8c71a-Reservations-test")
                    .build();

            ScanResponse result = dynamoDB.scan(scanRequest);
            JSONArray reservationsArray = new JSONArray();
            for (Map<String, AttributeValue> item : result.items()) {
                Map<String, Object> orderedMap = new LinkedHashMap<>();
                orderedMap.put("tableNumber", Integer.parseInt(item.get("tableNumber").n()));
                orderedMap.put("clientName", item.get("clientName").s());
                orderedMap.put("phoneNumber", item.get("phoneNumber").s());
                orderedMap.put("date", item.get("date").s());
                orderedMap.put("slotTimeStart", item.get("slotTimeStart").s());
                orderedMap.put("slotTimeEnd", item.get("slotTimeEnd").s());

                JSONObject reservation = new JSONObject(orderedMap);
                reservationsArray.put(reservation);
            }

            JSONObject responseBody = new JSONObject();
            responseBody.put("reservations", reservationsArray);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody.toString());
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.".toString());

        }
    }
}