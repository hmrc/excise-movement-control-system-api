#!/bin/sh


ACCESS_TOKEN='BXQ3/Treo4kQCZvVcCqKPhdoYmU4n3QnQyKG3HcqnY5oYNJG5l1rpghdf+WHw+RPNPyWVw+TLShcNdYuSn8JZ6NE9VOA7ZuEH8FAASdIinnxryWRVHwbBfcsB5cltx8tqmnjunRIyIUYCVHEmqa9tJ0L4IqxgZGDC6TOkk6dEocswHpJl11LmBXyj7ESiXc8/SsCJHiDyv5jJQREo7nuFQ=='



echo "##################################################################"
echo "Deploying services..."
echo "##################################################################"

./run_dependencies.sh

echo "Make sure you have a valid access token. You can use "
echo "the Auth-wizard to generate one at http://localhost:9949/auth-login-stub/gg-sign-in"
echo
echo "For a complete example see the CTC reference at https://confluence.tools.tax.service.gov.uk/display/~tim.squires/Testing+Push+Pull+Notifications+on+External+Test+-+CTC"

if [ -z "$ACCESS_TOKEN" ] 
then
  echo 
  echo "ACCESS_TOKEN variable is not set. Modify this shell script to set the ACCESS_TOKEN variable with a valid access token."
  exit
fi


echo "##################################################################"
echo "creating a client Application..."
echo "##################################################################"

response=$(curl --location -g --request POST 'http://localhost:9607/application' \
--header 'Authorization: Bearer $ACCESS_TOKEN' \
--header 'Content-Type: application/json' \
--data-raw '{
    "name": "TEST APP",
    "access": {
        "accessType": "STANDARD",
         "redirectUris": [],
         "postLogoutRedirectUris": [],
         "overrides": []
     },
    "environment": "SANDBOX",
    "collaborators": [
        {
            "emailAddress": "test@test.com",
            "role": "ADMINISTRATOR",
            "userId": "61e93581-5028-4475-912b-e8df6f406e2f"
        }
    ]
}')


clientId=$(echo $response | egrep -o '"clientId":\s?.*?".*?"' | egrep -o '[^"clientId":][^"].*[^"]')

echo "Client Id is: $clientId"
echo "copy this client Id as you will need it to make request."
read -p "press any key when you have done" any_input
export CLIENT_ID=$clientId

echo "##################################################################"
echo "Creating PPNS box..."
echo "##################################################################"

boxIdResponse=$(curl --location --request PUT 'http://localhost:6701/box' \
--header 'User-Agent: api-subscription-fields' \
--header 'Authorization: Bearer $ACCESS_TOKEN' \
--header 'Content-Type: application/json' \
--data '{ "boxName":"customs/excise##1.0##notificationUrl", "clientId":'\"$clientId\"'}')

echo "BoxId: $boxIdResponse"

echo "##################################################################"
echo "Adding subscription..."
echo "##################################################################"

curl --location --request PUT 'http://localhost:9650/definition/context/customs%2Fexcise/version/1.0' \
--header 'Content-Type: application/json' \
--data-raw '{
    "fieldDefinitions": [
        {
            "name": "notificationUrl",
            "shortDescription": "Notification URL",
            "description": "What is your notification web address for us to send push notifications to?",
            "type": "PPNSField",
            "hint": "You must only give us a web address that you own. Your application will use this address to listen to notifications from HMRC.",
            "validation": {
                "errorMessage": "notificationUrl must be a valid https URL",
                "rules": [
                    {
                        "UrlValidationRule": {}
                    }
                ]
            }
        }
    ]
}'

sbt "run -Drun.mode=Dev -Dhttp.port=10250 $*"