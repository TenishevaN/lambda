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
public class HelloWorld implements RequestHandler<Object, Map<String, Object>> {

    private static final Map<String, Object> RESPONSE_NOT_FOUND = Map.of(
                                                                     "statusCode", 400,
                                                                      "body", "Bad request syntax or unsupported method. Request path: null. HTTP method: null");

    private static final Map<String, Object> RESPONSE_OK = Map.of(
                                                              "statusCode", 200,
                                                               "body", "Hello from Lambda");

    public Map<String, Object> handleRequest(Object request, Context context) {


        if (request != null && request instanceof APIGatewayV2HTTPEvent) {
            return getResult(request);
        } else {
            return RESPONSE_NOT_FOUND;
        }
    }

    private  Map<String, Object> getResult(Object request) {

        APIGatewayV2HTTPEvent event = (APIGatewayV2HTTPEvent) request;
        String path = event.getRequestContext().getHttp().getPath();
        String method = event.getRequestContext().getHttp().getMethod();

        if ("/hello".equals(path)) {
            return RESPONSE_OK;
        } else {
            return Map.of(
                    "statusCode", 400,
                    "body", String.format("Bad request syntax or unsupported method. Request path: null. HTTP method: null", path, method));
        }
    }
}
