package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

@LambdaHandler(lambdaName = "sns_handler",
	roleName = "sns_handler-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@SnsEventSource(
		targetTopic = "lambda_topic"
)
@DependsOn(
		name = "lambda_topic",
		resourceType = ResourceType.SNS_TOPIC
)
public class SnsHandler implements RequestHandler<SNSEvent, Boolean> {
	LambdaLogger logger;

	@Override
	public Boolean handleRequest(SNSEvent event, Context context) {
		logger = context.getLogger();
		List<SNSRecord> records = event.getRecords();
		if (!records.isEmpty()) {
			Iterator<SNSRecord> recordsIter = records.iterator();
			while (recordsIter.hasNext()) {
				processRecord(recordsIter.next());
			}
		}
		return Boolean.TRUE;
	}

	public void processRecord(SNSRecord record) {
		try {
			String message = record.getSNS().getMessage();
			logger.log(message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
