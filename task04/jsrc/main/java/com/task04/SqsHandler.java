package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@LambdaHandler(lambdaName = "sqs_handler",
	roleName = "sqs_handler-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@SqsTriggerEventSource(
		batchSize = 10,
		targetQueue = "async_queue"
)
@DependsOn(
		name = "async_queue",
		resourceType = ResourceType.SQS_QUEUE
)
public class SqsHandler implements RequestHandler<SQSEvent, Void> {
	@Override
	public Void handleRequest(SQSEvent sqsEvent, Context context) {
		for (SQSMessage msg : sqsEvent.getRecords()) {
			processMessage(msg, context);
		}
		context.getLogger().log("done");
		return null;
	}

	private void processMessage(SQSMessage msg, Context context) {
		try {
			context.getLogger().log(msg.getBody());

	    } catch (Exception e) {
			context.getLogger().log("An error occurred while processing SQS Queue message");
			throw e;
		}
	}
}
