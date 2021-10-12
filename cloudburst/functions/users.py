def add_user_credits(cb, user_id, credits):
  user = cb.get(user_id)

  if user is None:
    user = {"credits": credits}
  else:
    user["credits"] += credits
  
  cb.put(user_id, user)
  return user

def retract_user_credits(cb, user_id, credits):
  user = cb.get(user_id)

  if user is None:
    return None
  else:
    user["credits"] -= credits
  
  cb.put(user_id, user)
  return user


def register_users_functions(cloud):
  cloud.register(add_user_credits, "add_user_credits")
  cloud.register(retract_user_credits, "retract_user_credits")