package com.task10.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.regions.Region;


public class RouteNotImplementedHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(RouteNotImplementedHandler.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        logger.info("Received request with HTTP method: {}", requestEvent.getHttpMethod());
        logger.info("Request path: {}", requestEvent.getPath());
        logger.info("Request body: {}", requestEvent.getBody());


        logger.info("Headers: {}", requestEvent.getHeaders());
        logger.info("Query parameters: {}", requestEvent.getQueryStringParameters());

        String tableName = System.getenv("reservations_table");
        if (!doesTableExist(tableName)) {
            logger.error("Table does not exist: " + tableName);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("doesn't exist");
        }


        return new APIGatewayProxyResponseEvent()
                .withStatusCode(501)
                .withBody(
                        new JSONObject().put(
                                "message",
                                "Handler for the %s method on the %s path is not implemented."
                                        .formatted(requestEvent.getHttpMethod(), requestEvent.getPath())
                        ).toString()
                );
    }

    private boolean doesTableExist(String tableName) {
        logger.info("if table exist: " + tableName);
        try {
            DynamoDbClient dynamoDB = DynamoDbClient.builder()
                    .region(Region.EU_CENTRAL_1)
                    .build();

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

}
