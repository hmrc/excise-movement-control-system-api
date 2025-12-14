# Concurrent Message Polling Test - CS-8172

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Solution](#solution)
3. [Test File](#test-file)
4. [Running the Test](#running-the-test)
5. [Key Timestamps Involved](#key-timestamps-involved)
6. [Implementation for Traders](#implementation-for-traders)
7. [Model Structure](#model-structure)
8. [Test Implementation Details](#test-implementation-details)
9. [Endpoint Reference](#endpoint-reference)
10. [Validation Checklist](#validation-checklist-for-implementation)
11. [Files Reference](#files-reference)
12. [Summary](#summary)

---

## Problem Statement

A timing gap between message creation (`createdOn`) and movement update (`lastUpdated`) causes messages to be filtered out when traders poll using incorrect `updatedSince` baseline parameters.

### The Bug: Real-World Timeline
```
Time            Event                                      lastUpdated    Issue
14:25:35.246Z   IE801 message created                     —              createdOn
14:25:35.251Z   Movement saved (5ms later)                ✓ 14:25:35.251Z
14:25:37.485Z   Trader polls /movements                   ✓ 14:25:35.251Z ← Uses this
14:28:42.204Z   Trader polls /messages?updatedSince=...   ✗ 0 messages   Message filtered out!
```

**Root Cause**: If using `updatedSince=14:25:35.251Z`:
- Message filter: `message.createdOn >= 14:25:35.251Z`?
- Message `createdOn`: `14:25:35.246Z`
- `14:25:35.246Z >= 14:25:35.251Z` → **FALSE** → Message **LOST**

---

## Solution

Do not use `updatedSince` on movement's first messages poll.
Use the **movement's `lastUpdated`** from the **previous poll response** as the `updatedSince` parameter for the next poll.

### Correct Polling Pattern
```
Poll 1 (Initial):     GET /movements/:id/messages (NO updatedSince)
                      → Returns all messages
                      → Save: movement.lastUpdated → t1

Poll 2 (Subsequent):  GET /movements/:id/messages?updatedSince=t1
                      → Returns only new messages (createdOn >= t1)
                      → Save: movement.lastUpdated → t2

Poll 3:               GET /movements/:id/messages?updatedSince=t2
                      ...continue this pattern
```

### Why It Works
1. **First poll**: No filtering, all messages returned
2. **Subsequent polls**: Using movement's `lastUpdated` ensures:
   - It's guaranteed to be >= latest message's `createdOn`
   - New messages inserted concurrently won't be missed
   - No gap between message creation and filter baseline

---

## Test File

**Location**: `it/test/uk/gov/hmrc/excisemovementcontrolsystemapi/ConcurrentMessagePollingItSpec.scala`

**Test Class**: `ConcurrentMessagePollingItSpec extends PlaySpec with GuiceOneServerPerSuite`

### 5 Test Scenarios

| Test | Validates |
|------|-----------|
| **Test 1**: First poll without `updatedSince` | All messages returned regardless of timing |
| **Test 2**: Subsequent poll with movement `lastUpdated` | Only new messages returned, old ones filtered |
| **Test 3**: Chained polling over multiple rounds | No message loss across multiple sequential polls |
| **Test 4**: Concurrent message insertion during polling | New messages caught even during concurrent operations |
| **Test 5**: Demonstrates the original bug | Why using movement's `lastUpdated` on first pool would fail |

### Test Results
```
✅ All 5 tests PASS
[info] Tests: succeeded 5, failed 0
```

---

## Running the Test

```bash
cd /home/hmrc/code/excise-movement-control-system-api
sbt "it/test:testOnly uk.gov.hmrc.excisemovementcontrolsystemapi.ConcurrentMessagePollingItSpec"
```

---

## Key Timestamps Involved

| Field | Description | Example | Role |
|-------|-------------|---------|------|
| `message.createdOn` | When message was created | `14:25:35.246Z` | Filter target (compared against `updatedSince`) |
| `movement.lastUpdated` | Generated in code when movement is updated with a new message | `14:25:35.251Z` | Should be used as `updatedSince` baseline |
| `updatedSince` (query param) | Filter threshold for next poll | Use `movement.lastUpdated` from previous response | Filter criterion |

---

## Implementation for Traders

### ✅ CORRECT APPROACH
```pseudocode
// First call - establish baseline
response1 = GET /movements/:movementId/messages
lastUpdatedValue = response1.movement.lastUpdated
processMessages(response1.messages)

// Subsequent calls - chain the lastUpdated values
while (shouldContinuePolling) {
    responseN = GET /movements/:movementId/messages?updatedSince=lastUpdatedValue
    lastUpdatedValue = responseN.movement.lastUpdated
    processMessages(responseN.messages)
}
```

### ❌ WRONG APPROACHES
```
❌ GET /movements/:id/messages?updatedSince={message.createdOn}
   → Messages may be lost due to timing gap

❌ GET /movements/:id/messages?updatedSince={movement.lastUpdated}  (on first poll)
   → Will miss all existing messages

❌ Using a static timestamp for all polls
   → Will miss new messages after that point
```

---

## Model Structure

### Message
```scala
case class Message(
  hash: Int,
  encodedMessage: String,
  messageType: String,
  messageId: String,
  recipient: String,
  boxesToNotify: Set[String],
  createdOn: Instant  // ← Used for filtering
)
```

### Movement
```scala
case class Movement(
  _id: String,
  boxId: Option[String],
  localReferenceNumber: String,
  consignorId: String,
  consigneeId: Option[String],
  administrativeReferenceCode: Option[String],
  lastUpdated: Instant,  // ← Use this for updatedSince
  messages: Seq[Message]
)
```

### Filter Logic
```scala
private def filterMessagesByTime(
  messages: Seq[Message], 
  updatedSince: Option[Instant]
): Seq[Message] =
  updatedSince.fold[Seq[Message]](messages)(a =>
    messages.filter(o => o.createdOn.isAfter(a) || o.createdOn.equals(a))
  )
```

**How it works**:
- If `updatedSince` is None: return all messages
- If `updatedSince` is provided: return messages where `createdOn >= updatedSince`

---

## Test Implementation Details

### Technology Stack
- **Framework**: Play Framework 2.8+
- **Language**: Scala 2.13
- **Test Framework**: ScalaTest with Mockito
- **Integration**: Full Play server per test suite

### Key Test Utilities
```scala
// Create timestamped messages
private def createMessage(messageId: String, createdOn: Instant): Message

// Make HTTP GET requests
private def getRequest(url: String, updatedSince: Option[String]): WSResponse

// Assertions
jsonMessages.length mustBe expectedCount
jsonMessages(0)("messageId").as[String] mustBe expectedId
```

### Test Timeline Example (Test 4: Concurrent Insertion)
```
t1: 14:25:35.246Z  → Message A created
t2: 14:25:35.251Z  → Movement.lastUpdated = t2 (after save)
t3: 14:25:37.100Z  → Trader polls without updatedSince, gets movement with lastUpdated=t2
                      (Message B is created concurrently at t3)
t4: 14:25:37.101Z  → Movement.lastUpdated updated to t4
t5: Trader polls with updatedSince=t2
    → Message B is returned because: createdOn(t3) >= t2 ✓
```

---

## Endpoint Reference

### GET `/movements/:movementId/messages`

**Parameters**:
- `movementId` (path): The movement ID
- `updatedSince` (query, optional): Return only messages with `createdOn >= updatedSince`

**Behavior**:
- When `updatedSince` is NOT provided: returns all messages
- When `updatedSince` IS provided: filters to messages where `message.createdOn >= updatedSince`

**Response**:
```json
[
  {
    "encodedMessage": "...",
    "messageType": "IE801",
    "recipient": "GBWK002281023",
    "messageId": "uuid",
    "createdOn": "2024-10-05T14:25:35.246Z"
  }
]
```

---

## Validation Checklist for Implementation

- [ ] First poll calls `/movements/:movementId/messages` WITHOUT `updatedSince`
- [ ] First poll response's `movement.lastUpdated` is stored
- [ ] Subsequent polls include `?updatedSince={lastUpdated}` from previous response
- [ ] After each poll, new `movement.lastUpdated` is stored for next poll
- [ ] Pattern continues indefinitely for polling loop
- [ ] No use of `message.createdOn` as baseline for filters
- [ ] No caching of `updatedSince` value across multiple poll sequences

---

## Files Reference

| File | Location |
|------|----------|
| **Integration Test** | `it/test/uk/gov/hmrc/excisemovementcontrolsystemapi/ConcurrentMessagePollingItSpec.scala` |
| **Controller** | `app/uk/gov/hmrc/excisemovementcontrolsystemapi/controllers/GetMessagesController.scala` |
| **Models** | `app/uk/gov/hmrc/excisemovementcontrolsystemapi/repository/model/Movement.scala` |
| **Routes** | `conf/app.routes` |

---

## Summary

| Aspect | Detail |
|--------|--------|
| **Problem** | 5ms timing gap causes messages to be filtered out |
| **Solution** | Use movement's `lastUpdated` from previous poll as `updatedSince` baseline |
| **First Poll** | No `updatedSince` parameter (establish baseline) |
| **Subsequent Polls** | Use `lastUpdated` from previous response |
| **Test Status** | ✅ All 5 scenarios PASSING |
| **Trader Impact** | Client-side implementation only (no API changes) |

---

**Created**: December 14, 2025  
**Ticket**: CS-8172  
**Branch**: CS-8172  
**Status**: ✅ Ready for Production
