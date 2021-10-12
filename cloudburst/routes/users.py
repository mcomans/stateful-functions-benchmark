import uuid
from flask import Blueprint, request
from uuid import uuid4

users = Blueprint('users', __name__)

@users.post("/")
def create_user():
  user = request.json
  user_id = str(uuid4())

  return user_id