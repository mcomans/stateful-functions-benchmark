FROM alpine

RUN apk add postgresql-client
COPY . .
ENTRYPOINT ["sh", "create_db.sh"]
