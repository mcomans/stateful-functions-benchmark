from flask import Blueprint, request
from uuid import uuid4
from cloud import cloud

users = Blueprint('users', __name__)

add_user_credits = cloud.get_function("add_user_credits")
retract_user_credits = cloud.get_function("retract_user_credits")

@users.post("/")
def create_user():
  request_id = str(uuid4())
  user = request.json
  user_id = str(uuid4())

  if user is not None and "credits" in user:
    add_user_credits(user_id, user["credits"], request_id).get()

  return user_id

@users.patch("/<user_id>/credits/add")
def add_credits_to_user(user_id):
  request_id = str(uuid4())
  user_body = request.json

  if user_body is not None and "credits" in user_body:
    user = add_user_credits(user_id, user_body["credits"], request_id).get()
  
  return user_id

@users.patch("/<user_id>/credits/retract")
def retract_credits_from_user(user_id):
  request_id = str(uuid4())
  user_body = request.json

  if user_body is not None and "credits" in user_body:
    user = retract_user_credits(user_id, user_body["credits"], request_id).get()
  
  return user_id
