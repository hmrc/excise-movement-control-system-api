
# Excise Movement Control System API

This is the public facing EMCS API to process and record Excise Movements.

## Nomenclature

ARC - Administrative Reference Code

EMCS - Excise Movement Control System

ERN - Excise Reference Number

LRN - Local Reference Number

## Technical documentation

See the [Service Guide](https://developer.service.hmrc.gov.uk/guides/emcs-api-service-guide/)

### Before running the app (if applicable)

Ensure you have MongoDb running and dependencies, via `sm2` below. If you want to run the API against a local version of the stub, you will not need to include `EMCS_API_EIS_STUB` in the `sm2` run.

### Running the test suite

To run unit tests you can run (either in a terminal or in a sbt shell):
`sbt test`

To run integration tests, run:
`sbt it/test`

### Test push notification locally

Notice: You can use the run_local_with_push_notification.sh script file to create an Client application locally.
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
    EMCS_API_EIS_STUB
    ```

#### Generate an access token
* Use the [Auth wizard](http://localhost:9949/auth-login-stub/gg-sign-in)
* Fill the following details: 
<br><br>

  **Redirect Url**: http://localhost:9949/auth-login-stub/session <br>
  **Affinity Group**: Organisation <br>
  **Enrolment Key**: HMRC-EMCS-ORG <br>
  **Identifier Name**: ExciseNumber <br>
  **Identifier Value**: GBWK002281024 (or any thing else similer)
<br><br>
* Press submit. This will redirect to a new page showing an access token.
* Copy the Bearer token

#### Create a client application

* In the terminal type the following command and press enter

    ```
    curl --location -g --request POST 'http://localhost:9607/application' \
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
    }'
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

#### Create a movement

1. Open Postman 
2. You can create your own request for the Draft Movement endpoint, or you can import.
the postman json file that is in the emcs-api-eis-stub repo. 
3. If you import the postman json file in the repo once imported.
4. Click on the **EMCSApiPollingScenarios** workflow.
5. Select the **Variables** tab on the right windows. 
6. Enter the Excise Number. 
7. Enter the access token. 
8. Enter the ClientId that was generated previously in the steps above.
9. Right-click the **EMCSApiPollingScenarios** workflow. 
10. Select **Run Collection**.
11. Press the **Run EMCSApiPollingScenarios**" button.
12. You should see a response that contains the boxId for each IE815 submission.

Remember if you do not use the **SubmitDraftOfMovement** scenario you need to
add the ClientId to the header when you submit an IE815.

  ```
  X-Client-Id, <the client id generated above for your app>
  ```


#### Verify push notification was sent

When an IE815 message (Draft Movement) is sent, a movement object will be created
and saved in the movements collection in a MongoDb database. At the same a workitem for that Excise Number is added to the excise-number-work-item collection in MongoDb.
This collection contains Excise Number for which a movement was created and represent 
a slow and a fast queue which will be processed by a thread
at every interval (which is configurable). So this thread will pick up the first processable
item/excise number and will request all message for that excise number, save that message in the
movement and send a notification to the push-pull-notification service. To see if that notification
has been received do the following:

**Notice: Before sending the IE815 following the steps above make sure you temporarily
change some configuration setting as below:**

* in the **application.conf** file change the following variable with the following settings:
  * initialDelay = 20 seconds 
  * interval = 20 seconds
  * fastInterval = 10 seconds

  This will start the scheduler after 20 seconds and run every 20 seconds


* if you start the service from comannd line not using service manager use the following command:


  ````
  sbt start
  ````

1. to see if a push notification was sent we can check if the push-pull-notification service has received the request.
2. create an access token using the [Auth Wizard](http://localhost:9949/auth-login-stub/application-login)
3. in the **Redirect Url** enter the following URL:http://localhost:9949/auth-login-stub/session
4. for the **Client Id** enter the client Id that was used in the header of the IE815
5. Select **PrivilegedApplication**
6. Press Submit
7. This will create a Bearer token and show it in a page
8. In postman create a GET request.
9. As URL enter http://localhost:6701/box/:boxId/notifications. The boxId is the boxId that was returned by the IE815 response
10. Paste the token in the Authorization
11. Send the request. If everything is ok you should see the following response:

```aidl
[
    {
        "notificationId": "6d984807-edcd-4e20-b13a-d61195529363",
        "boxId": "4cf8aa04-7215-45c6-a53a-181e6d6ce7e4",
        "messageContentType": "application/json",
        "message": "{\"movementId\":\"c79da99b-4a7e-4dfa-b79c-3dbd6d280279\", \"messageUri\":\"/customs/excise/movements/<movementid>/messages/<messageId>\",\"messageId\":\"XI000001\",\"consignor\":\"GBWK002281024\",\"consignee\":\"GBWKQOZ8OVLYR\",\"arc\":\"23XI00000000000000012\",\"ern\":\"GBWK002281024\"}",
        "status": "PENDING",
        "createdDateTime": "2024-01-27T21:11:28.453+0000"
    }
]
```

### Create Client Application locally

the **run_local_with_push_notification.sh** script file offers a quick way to create a Client application.
You should acquire an access token and modify the file by overriding the ACCEES_TOKEN variable by assigning its value to 
the newly acquired access token. Then from command line type the following command and press enter:

  ```aidl
  ./run_local_with_push_notification.sh
  ```

This will create a client application and give you back a clientId. This will also start the excise-movement-control-system-api
 service locally.

## Licence

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").