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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


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
            DynamoDbClient dynamoDB = DynamoDbClient.create();
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName("cmtr-85e8c71a-Tables")
                    .build();

            ScanResponse result = dynamoDB.scan(scanRequest);
            ObjectMapper mapper = new ObjectMapper();

            ArrayNode tablesArray = mapper.createArrayNode();

            for (Map<String, AttributeValue> item : result.items()) {
                ObjectNode table = mapper.createObjectNode();
                table.put("id", Integer.parseInt(item.get("id").n()));
                table.put("number", Integer.parseInt(item.get("number").n()));
                table.put("places", Integer.parseInt(item.get("places").n()));
                table.put("isVip", item.get("isVip").bool());

                if (item.containsKey("minOrder")) {
                    table.put("minOrder", Integer.parseInt(item.get("minOrder").n()));
                }

                tablesArray.add(table);
            }

            ObjectNode responseBody = mapper.createObjectNode();
            responseBody.set("tables", tablesArray);
            String jsonOutput = mapper.writeValueAsString(responseBody);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(jsonOutput);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(e.getMessage().toString());

        }
    }
}