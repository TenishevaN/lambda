import boto3
import uuid
import json

from commons.log_helper import get_logger
from commons.abstract_lambda import AbstractLambda
from datetime import datetime

_LOG = get_logger('UuidGenerator-handler')


class UuidGenerator(AbstractLambda):

    s3 = boto3.resource('s3')
    bucket = s3.Bucket('cmtr-85e8c71a-uuid-storage-test')
        
    def handle_request(self, event, context):
      uuids = [str(uuid.uuid4()) for _ in range(10)]
      file_name = datetime.now().isoformat() + ".json"
      data = {"ids": uuids}
      self.bucket.put_object(Key=file_name, Body=json.dumps(data))
      return 200
    

HANDLER = UuidGenerator()


def uuid_generator(event, context):
    producer = UuidGenerator()
    return producer.handle_request(event, context)
