from commons.log_helper import get_logger
from commons.abstract_lambda import AbstractLambda
import boto3
import json
import uuid
import os
from datetime import datetime, timezone

_LOG = get_logger('AuditProducer-handler')

class AuditProducer(AbstractLambda):
    dynamodb = boto3.resource('dynamodb')
    lambda_client = boto3.client('lambda')

    def handle_request(self, event, context):
        records_processed = 0

        if 'Records' not in event:
            _LOG.error("No 'Records' in the event")
            _LOG.info(f"Received event: {event}")
        else:
            for record in event['Records']:
                current_time = datetime.now(timezone.utc)
                iso_time = current_time.isoformat()

                _LOG.info(f"record content: {record['dynamodb']}")

                change_type = record['eventName']  # INSERT, MODIFY, REMOVE
                new_image = record['dynamodb'].get('NewImage')
                item_key = new_image.get('key', {}).get('S') if new_image else None

                if change_type in ['INSERT', 'MODIFY']:
                    new_value = int(new_image.get('value', {}).get('N', 0)) if new_image else None

                if change_type == 'MODIFY':
                    old_image = record['dynamodb'].get('OldImage')
                    old_value = int(old_image.get('value', {}).get('N', 0)) if old_image else None
                else:
                    old_value, updated_attribute = None, None

                obj = {
                    'id':  str(uuid.uuid4()),
                    'itemKey': item_key,
                    'modificationTime': iso_time,
                }

                if change_type == 'INSERT':
                    obj['newValue'] = {
                        'key': item_key,
                        'value': int(new_value)
                    }
                elif change_type == 'MODIFY':
                    obj['updatedAttribute'] = 'value'
                    obj['oldValue'] = old_value
                    obj['newValue'] = new_value

                _LOG.info(f"object to write to db: {obj}")

                dynamodb = boto3.resource('dynamodb', region_name=os.environ['region'])
                table_name = os.environ['table_name']
                table = dynamodb.Table(table_name)

                try:
                    response = table.put_item(Item=obj)
                    records_processed += 1
                except Exception as e:
                    _LOG.error(f"Error putting item into DynamoDB: {e}")
                    return {
                        "statusCode": 500,
                        "errorMessage": f"Error putting item into DynamoDB: {e}"
                    }

        return {
            "statusCode": 201,
            "event": f"Successfully processed {records_processed} records."
        }

def audit_producer(event, context):
    producer = AuditProducer()
    return producer.handle_request(event, context)