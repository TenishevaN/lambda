package com.task10.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteNotImplementedHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(RouteNotImplementedHandler.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        logger.info("Received request with HTTP method: {}", requestEvent.getHttpMethod());
        logger.info("Request path: {}", requestEvent.getPath());
        logger.info("Request body: {}", requestEvent.getBody());


        logger.info("Headers: {}", requestEvent.getHeaders());
        logger.info("Query parameters: {}", requestEvent.getQueryStringParameters());


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

}
