# API Endpoints

## Authentication

### Login
```http
POST /api/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "deviceID": "mobile_abc123",
  "datetime": 1732617600,
  "duration": 1732617600
}

Response 200:
{
  "token": "Bearer_token_string",
  "case": { /* CaseData object */ },
  "custominputs": [ /* InputField array */ ],
  "inputs": { /* InputsConfig object */ },
  "api_version": "v1"
}
```

## Entries (v1 and v2 use same patterns)

### Get Entries
```http
GET /api/{version}/entry/{caseId}
Authorization: Bearer {token}

Response 200:
{
  "data": [
    {
      "id": 123,
      "begin": "1732617600",
      "end": "1732621200",
      "case_id": 456,
      "entity_id": 789,
      "entity_name": "Entity Name",
      "inputs": "{\"field1\":\"value1\"}",
      "created_at": "2024-11-26 10:00:00",
      "updated_at": "2024-11-26 10:00:00"
    }
  ]
}
```

### Create Entry
```http
POST /api/{version}/cases/{caseId}/entries
Authorization: Bearer {token}
Content-Type: application/json

{
  "begin": 1732617600,
  "end": 1732621200,
  "case_id": 456,
  "entity_id": "entity_name",
  "inputs": {
    "text_field": "Some text",
    "scale_field": 3,
    "audio_field": "data:audio/mp3;base64,..."
  }
}

Response 200:
{ "id": 789 }
```

### Update Entry
```http
PATCH /api/{version}/cases/{caseId}/entries/{entryId}
Authorization: Bearer {token}
Body: Same as Create
```

### Delete Entry
```http
DELETE /api/{version}/cases/{caseId}/entries/{entryId}
Authorization: Bearer {token}

Response 200: {}
```

## Audio Files

Audio is sent inline as base64 Data URI:
```
data:audio/mp3;base64,AAAA...
```

Format: AAC-ADTS encoded, sent with `audio/mp3` MIME type for backend compatibility.

## Timestamp Format

- **Sent to API**: Unix timestamp (Long) - e.g., `1732617600`
- **Received from API**: May be Unix or datetime string
- **Display**: Converted to local format - e.g., "26 Nov 2024 10:00"

## Error Responses

| Code | Meaning |
|------|---------|
| 401 | Invalid/expired token |
| 404 | Resource not found |
| 422 | Validation error |
| 500 | Server error |
