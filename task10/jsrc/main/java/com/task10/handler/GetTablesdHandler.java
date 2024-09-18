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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetTablesdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(GetTablesdHandler.class);

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {


        logger.info("Received request with HTTP method: {}", requestEvent.getHttpMethod());
        logger.info("Request path: {}", requestEvent.getPath());
        logger.info("Request body: {}", requestEvent.getBody());


        logger.info("Headers: {}", requestEvent.getHeaders());
        logger.info("Query parameters: {}", requestEvent.getQueryStringParameters());

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
                    .withBody(e.getMessage().toString());

        }
    }
}