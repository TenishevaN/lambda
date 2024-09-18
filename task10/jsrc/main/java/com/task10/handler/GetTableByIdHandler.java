package com.task10.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;


public class GetTableByIdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        String tableId = requestEvent.getPathParameters().get("tableId");
        logger.log("Received request for tableId: " + tableId);

        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName("cmtr-85e8c71a-Tables")
                    .build();

            ScanResponse result = dynamoDB.scan(scanRequest);
            JsonArray tablesArray = new JsonArray();
            for (Map<String, AttributeValue> item : result.items()) {
                String id = item.get("id").n();
                if (!tableId.equals(id)) {
                    continue;
                }
                JsonObject table = new JsonObject();
                table.addProperty("id", tableId);
                table.addProperty("number", Integer.parseInt(item.get("number").n()));
                table.addProperty("places", Integer.parseInt(item.get("places").n()));
                table.addProperty("isVip", item.get("isVip").bool());
                if (item.containsKey("minOrder")) {
                    table.addProperty("minOrder", Integer.parseInt(item.get("minOrder").n()));
                }
                tablesArray.add(table);
            }

            JsonObject responseBody = new JsonObject();
            responseBody.add("tables", tablesArray);

            Gson gson = new Gson();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(responseBody));
        } catch (Exception e) {
            logger.log("Error processing request: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"There was an error in the request.\"}");
        }
    }
}