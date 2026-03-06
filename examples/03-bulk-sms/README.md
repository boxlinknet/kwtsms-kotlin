# 03: Bulk SMS

## What this example demonstrates

Sending SMS to more than 200 numbers using `sendBulk()`, which automatically splits into batches, handles rate limits, and retries on queue full errors.

## Prerequisites

- Java 8+ installed
- kwtSMS account with sufficient credits for the send

## Environment setup

```ini
KWTSMS_USERNAME=your_api_user
KWTSMS_PASSWORD=your_api_pass
KWTSMS_SENDER_ID=YOUR-SENDERID
KWTSMS_TEST_MODE=1
```

## Walkthrough

1. **Auto-batching.** `sendBulk()` splits numbers into batches of 200 (the API maximum per request).
2. **Rate limiting.** 0.5s delay between batches keeps you under the 2 req/s API limit.
3. **ERR013 retry.** If the API queue is full (1000 messages), the client retries with exponential backoff: 30s, 60s, 120s.
4. **Deduplication.** Duplicate numbers (after normalization) are sent only once.
5. **Aggregated results.** The response includes totals across all batches and per-batch error details.

## Expected output

```
Sending to 500 numbers...
Result: OK
Batches: 3
Numbers sent: 500
Credits used: 500
Balance after: 1500.0
Message IDs: [abc123, def456, ghi789]
```

## Common mistakes

1. **Not checking the result type.** `result` can be `"OK"` (all batches succeeded), `"PARTIAL"` (some failed), or `"ERROR"` (all failed). Always check.

2. **Estimating credit cost wrong.** Multi-page messages (>160 chars English or >70 chars Arabic) cost multiple credits per number. Check message length before bulk sends.

3. **Not saving message IDs.** Each batch returns its own `msgId`. Store all of them for status and delivery report lookups.
