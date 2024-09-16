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

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class PostTablesdHandler  extends CognitoSupport  implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DynamoDbClient dynamoDB = DynamoDbClient.builder()
            .region(Region.EU_CENTRAL_1)
            .build();

    public PostTablesdHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context)  {
        try {
            JSONObject requestBody = new JSONObject(requestEvent.getBody());
            int id = requestBody.getInt("id");
            int number = requestBody.getInt("number");
            int places = requestBody.getInt("places");
            boolean isVip = requestBody.getBoolean("isVip");
            Integer minOrder = requestBody.has("minOrder") ? requestBody.getInt("minOrder") : null;

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().n(String.valueOf(id)).build());
            item.put("number", AttributeValue.builder().n(String.valueOf(number)).build());
            item.put("places", AttributeValue.builder().n(String.valueOf(places)).build());
            item.put("isVip", AttributeValue.builder().bool(isVip).build());
            if (minOrder != null) {
                item.put("minOrder", AttributeValue.builder().n(String.valueOf(minOrder)).build());
            }

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName("cmtr-85e8c71a-Tables")
                    .item(item)
                    .build();
            PutItemResponse putItemResponse = dynamoDB.putItem(putItemRequest);

            JSONObject responseBody = new JSONObject()
                    .put("id", id);

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
