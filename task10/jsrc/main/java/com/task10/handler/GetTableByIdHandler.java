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
            JSONArray tablesArray = new JSONArray();
            for (Map<String, AttributeValue> item : result.items()) {
                String id = item.get("id").n();
                if(!tableId.equals(id)){
                   continue;
                }
                Map<String, Object> orderedMap = new LinkedHashMap<>();
                orderedMap.put("id", tableId);
                orderedMap.put("number", Integer.parseInt(item.get("number").n()));
                orderedMap.put("places", Integer.parseInt(item.get("places").n()));
                orderedMap.put("isVip", item.get("isVip").bool());
                if (item.containsKey("minOrder")) {
                    orderedMap.put("minOrder", Integer.parseInt(item.get("minOrder").n()));
                }
                JSONObject table = new JSONObject(orderedMap);
                tablesArray.put(table);
            }

            JSONObject responseBody = new JSONObject();
            responseBody.put("tables", tablesArray);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody.toString());
        } catch (Exception e) {
            logger.log("Error processing request: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"There was an error in the request.\"}");
        }
    }
}