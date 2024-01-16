
# Excise Movement Control System API

This is the public facing EMCS API to process and record Excise Movements. Further text TBD

## Nomenclature

ARC - Administrative Reference Code

EMCS - Excise Movement Control System

ERN - Excise Reference Number

LRN - Local Reference Number

## Technical documentation

TBD

### Before running the app (if applicable)

TBD

### Running the test suite

TBD


### Test push notification locally

These instructions have been taken from the [Testing Push Pull Notifications on External Test - CTC](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=~tim.squires&title=Testing+Push+Pull+Notifications+on+External+Test+-+CTC) example. 
Most of the step for testing the EMCS API are similar to that one in this page. 
Only the services loaded with some other details are different.

Notice: This may be moved to a confluence page in a later time.

#### Set up callback listener

You can use mocklab.io. See [Testing Push Pull Notifications on External Test - CTC](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=~tim.squires&title=Testing+Push+Pull+Notifications+on+External+Test+-+CTC) for an example how to do it. 

#### Start the services
* Open a terminal window, type the below command and press enter. This will load locally all the services necessary for testing :

    ```
    sm2 --start AUTH_LOGIN_API \
    AUTH_LOGIN_STUB \
    AUTH \
    USER_DETAILS \
    ASSETS_FRONTEND_2 \
    IDENTITY_VERIFICATION \
    NRS_STUBS \
    THIRD_PARTY_APPLICATION \
    API_SUBSCRIPTION_FIELDS \
    PUSH_PULL_NOTIFICATIONS_API \
    PUSH_PULL_NOTIFICATIONS_GATEWAY \
    EXCISE_MOVEMENT_CONTROL_SYSTEM_API \
    EMCS_API_EIS_STUB
    ```

#### Generate an access token
* Use the [Auth wizard](https://www.development.tax.service.gov.uk/auth-login-stub/gg-sign-in)
* Fill the following details: 
<br><br>

  **Redirect Url**: http://localhost:9949/auth-login-stub/session <br>
  **Affinity Group**: Organisation <br>
  **Enrolment Key**: HMRC-EMCS-ORG <br>
  **Identifier Name**: ExciseNumber <br>
  **Identifier Value**: GBWK002281024 (or any thing else similer)
<br><br>
* Press submit. This will redirect to a new page with an access token.
* Copy the Bearer token

####Create a client application

* In the terminal type the following command and press enter

    ```
    response=$(curl --location -g --request POST 'http://localhost:9607/application' \
    --header 'Authorization: Bearer <paste the generated access token here>' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "name": "TEST APP",
        "access": {
            "accessType": "STANDARD",
            "redirectUris": [],
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
    ```
* copy the client Id

#### Create a PPNS box

* In the terminal type the following command and press enter

    ```
    curl --location --request PUT 'http://localhost:6701/box' \
    --header 'User-Agent: api-subscription-fields' \
    --header 'Authorization: Bearer <paste the access token here>' \
    --header 'Content-Type: application/json' \
    --data '{ "boxName":"customs/excise##1.0##notificationUrl", "clientId":'\"<paste the clientId here>\"'}'
    
    ```
* This will give you a boxId. Copy the boxId

#### Add a subscription

* in the terminal type the following command and press enter

```
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
```

#### Set the callback URL

* in the terminal type the following command and press enter

    ```
    curl --location --request PUT 'http://localhost:9650/field/application/<paste the boxId here>/context/customs%2Fexcise/version/1.0' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "fields": {
            "notificationUrl": "<put here Mock Api Url that is in mocklab.io>"
        }
    }'
    ```
  In the mocklab request log, you should see the challenge
#### Create a movement

* Open Postman
* you can create your own request for the Draft Movement endpoint or you can import
the postman json file that is in the excise-movement-control-system-api repo.
* If you import the postman json file in the repo once imported
* click on the **EMCSApiPollingScenarios** workflow
* Select the **Pre-request Script** tab on the right windows
* Enter the Excise Number 
* Enter the access token
* Expand this workflow
* Click on the first **SubmitDraftOfMovement** request
* Got to the **header** tab
* Add the following header:
  ```
  X-Client-Id, <the client id generate above for your app>
  ```
  
* Send the request
* You should see a response that contain the boxId
### Further documentation

TBD

## Licence

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").