# AI Bio Generator — Setup

## 1. Database migration
Run this once on your MySQL database:
```sql
ALTER TABLE users ADD COLUMN bio TEXT NULL;
ALTER TABLE users ADD COLUMN skills TEXT NULL;
ALTER TABLE users ADD COLUMN experience TEXT NULL;
```

## 2. Gemini API key (free)
1. Go to https://aistudio.google.com/app/apikey
2. Click **Create API key** — no credit card needed
3. Copy the key

**Option A — Environment variable (recommended):**

Windows (PowerShell):
```powershell
$env:GEMINI_API_KEY = "AIza..."
```

**Option B — Hardcode (dev only):**
Edit `src/main/java/org/example/util/AIService.java` and replace:
```java
: "YOUR_GEMINI_API_KEY";
```
with your actual key.

## Free tier limits
- 15 requests / minute
- 1,000,000 tokens / day
- Model: gemini-2.0-flash
