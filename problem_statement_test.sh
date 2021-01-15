#!/usr/bin/env bash

URL="http://127.0.0.1:8080/user/bob/points"
CONTENT_TYPE="Content-Type: application/json"

printf "Adding [DANNON, 300]\n"
curl -X POST "$URL" -H "$CONTENT_TYPE" -d '{"payer": "DANNON",  "points": 300}'
printf "Adding [UNILEVER, 200]\n"
curl -X POST "$URL" -H "$CONTENT_TYPE" -d '{"payer": "UNILEVER",  "points": 200}'
printf "Adding [DANNON, -200]\n"
curl -X POST "$URL" -H "$CONTENT_TYPE" -d '{"payer": "DANNON",  "points": -200}'
printf "Adding [MILLER COORS, 10000]\n"
curl -X POST "$URL" -H "$CONTENT_TYPE" -d '{"payer": "MILLER COORS",  "points": 10000}'
printf "Adding [DANNON, 1000]\n"
curl -X POST "$URL" -H "$CONTENT_TYPE" -d '{"payer": "DANNON",  "points": 1000}'

printf "\nIntermediary Result:\n"
curl --silent -X GET "$URL" | json_pp

printf "\nDelete 5000 points:\n"
curl --silent -X DELETE "$URL" -H "$CONTENT_TYPE" -d '{"points": 5000}' | json_pp

printf "\nFinal Result:\n"
curl --silent -X GET "$URL" | json_pp

# Clean up
curl --silent -o /dev/null -X DELETE "$URL" -H "$CONTENT_TYPE" -d '{"points": 6300}'
