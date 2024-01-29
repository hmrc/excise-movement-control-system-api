#!/bin/sh

sm2 --start AUTH_LOGIN_API \
 AUTH_LOGIN_STUB \
 AUTH \
 USER_DETAILS \
 ASSETS_FRONTEND_2 \
 IDENTITY_VERIFICATION \
 NRS_STUBS \
 THIRD_PARTY_APPLICATION \
 API_SUBSCRIPTION_FIELDS \
 PUSH_PULL_NOTIFICATIONS_API --appendArgs '{"PUSH_PULL_NOTIFICATIONS_API":["-Dallowlisted.useragents.0=api-subscription-fields","-Dallowlisted.useragents.1=excise-movement-control-system-api"]}' \
 PUSH_PULL_NOTIFICATIONS_GATEWAY --appendArgs '{"PUSH_PULL_NOTIFICATIONS_GATEWAY":["-DvalidateHttpsCallbackUrl=false"]}'
