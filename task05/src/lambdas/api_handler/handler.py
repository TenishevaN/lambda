import json
import os
import uuid
import boto3
from datetime import datetime

def lambda_handler(event, context):

      if 'principalId' not in event or 'content' not in event:
            return {
                "statusCode": 400,
                "errorMessage": "Missing 'principalId' or 'content' in event"
            }

       if 'content' not in event or not isinstance(event['content'], dict):
              return {
                  "statusCode": 400,
                  "errorMessage": "Missing 'content' or it's not a dictionary"
              }

    current_time = datetime.now()
    iso_time = current_time.isoformat()

    obj = {
        'id':  str(uuid.uuid4()),
        'principalId': event['principalId'],
        'createdAt': iso_time,
        'body': event['content']
    }

    dynamodb = boto3.resource('dynamodb', region_name=os.environ['region'])
    table_name = os.environ['table_name']

    table = dynamodb.Table(table_name)

    response = table.put_item(Item=obj)

    return {
        "statusCode": 201,
        "event": json.dumps(response, indent=4)
    }