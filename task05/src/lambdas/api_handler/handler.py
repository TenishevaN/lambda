import json
import os
import uuid

import boto3

from datetime import datetime

current_time = datetime.now()
iso_time = current_time.isoformat()

def lambda_handler(event, context):
    obj = {
        'id': str(uuid.uuid4()),
        'principalId': str(uuid.uuid1()),
        'createdAt': iso_time,
        'body': str(event)
    }

    dynamodb = boto3.resource('dynamodb', region_name=os.environ['region'])
    table_name = os.environ['table_name']

    table = dynamodb.Table(table_name)

    response = table.put_item(Item=obj)

    return {
        "statusCode": 201,
        "event": json.dumps(response, indent=4)
    }
