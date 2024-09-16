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
import org.json.JSONArray;
import java.util.LinkedHashMap;

public class GetTableByIdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String tableId = requestEvent.getPathParameters().get("tableId");

        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName("cmtr-85e8c71a-Tables")
                    .build();

            ScanResponse result = dynamoDB.scan(scanRequest);
            JSONArray tablesArray = new JSONArray();
            for (Map<String, AttributeValue> item : result.items()) {
                Map<String, Object> orderedMap = new LinkedHashMap<>();
                orderedMap.put("id", Integer.parseInt(item.get("id").n()));
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
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("There was an error in the request.".toString());

        }
    }
}