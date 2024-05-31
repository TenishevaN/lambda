package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@LambdaHandler(lambdaName = "hello_world",
        roleName = "hello_world-role",
        isPublishVersion = false,
        layers = {"sdk-layer"},
        runtime = DeploymentRuntime.JAVA11,
        architecture = Architecture.ARM64,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
        layerName = "sdk-layer",
        libraries = {"lib/commons-lang3-3.14.0.jar", "lib/gson-2.10.1.jar"},
        runtime = DeploymentRuntime.JAVA11,
        architectures = {Architecture.ARM64},
        artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
public class HelloWorld implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final int SC_OK = 200;
    private static final int SC_NOT_FOUND = 404;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent requestEvent, Context context) {

        System.out.println("requestEvent.getRequestContext() " + requestEvent.getRequestContext());

        if(requestEvent.getRequestContext() == null){
            return buildResponse(SC_OK, Body.ok("Use the path /hello to get greetings message"));
        }

        var path = getPath(requestEvent);
        var method = getMethod(requestEvent);

        if("/hello".equals(path)){
            return buildResponse(SC_OK, Body.ok("Hello from Lambda"));
        }

        return buildResponse(SC_NOT_FOUND, Body.error(String.format("Bad request syntax or unsupported method. Request path: %s. HTTP method: %s",
                getPath(requestEvent),
                getMethod(requestEvent)
        )));
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, Object body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(responseHeaders)
                .withBody(gson.toJson(body))
                .build();
    }

    private String getMethod(APIGatewayV2HTTPEvent requestEvent) {
        return requestEvent.getRequestContext().getHttp().getMethod();
    }

    private String getPath(APIGatewayV2HTTPEvent requestEvent) {
        return requestEvent.getRequestContext().getHttp().getPath();
    }

    private static class Body {
        private String message;
        private String error;

        public String getMessage() {
            return message;
        }

        public String getError() {
            return error;
        }

        public Body(String message, String error) {
            this.message = message;
            this.error = error;
        }

        static Body ok(String message) {
            return new Body(message, null);
        }

        static Body error(String error) {
            return new Body(null, error);
        }
    }
}
