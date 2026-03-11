# Mizan — Free Deployment Guide

## Stack (100% Free)

| Service | Provider | Free Tier |
|---------|----------|-----------|
| Spring Boot API | **Render.com** | Free web service (sleeps after 15 min idle) |
| PostgreSQL | **Neon.dev** | Free forever (0.5 GB storage, autoscaling compute) |
| Redis | **Upstash.com** | Free (10,000 commands/day, 256 MB) |
| Market Data | **Twelve Data** | Free per user (800 calls/day BYOK) |
| Mobile Testing | **Expo Go** | Free (test on phone without Xcode) |

Total cost: **$0/month**

---

## Step 1: Create accounts (5 minutes)

Create free accounts on these 3 sites:
1. **https://neon.tech** → Sign up with GitHub
2. **https://upstash.com** → Sign up with GitHub
3. **https://render.com** → Sign up with GitHub

---

## Step 2: Set up Neon PostgreSQL (3 minutes)

1. Log in to **neon.tech**
2. Click **"New Project"**
3. Name: `mizan` → Region: `aws-eu-central-1` (Frankfurt, closest to Egypt)
4. Click **Create Project**
5. You'll see a connection string like:
   ```
   postgresql://mizan_owner:abc123@ep-cool-river-123456.eu-central-1.aws.neon.tech/mizan?sslmode=require
   ```
6. **Save these values** (you'll need them for Render):
   - Host: `ep-cool-river-123456.eu-central-1.aws.neon.tech`
   - Database: `mizan`
   - Username: `mizan_owner`
   - Password: `abc123` (your actual password)

---

## Step 3: Set up Upstash Redis (2 minutes)

1. Log in to **upstash.com**
2. Click **"Create Database"**
3. Name: `mizan-cache` → Region: `eu-central-1` (Frankfurt)
4. Type: **Regional** → Click **Create**
5. You'll see connection details:
   - Endpoint: `eu1-abc-12345.upstash.io`
   - Port: `6379`
   - Password: `AXxxYYzz...`
6. **Save these values**

---

## Step 4: Push code to GitHub (3 minutes)

1. Create a new GitHub repository called `mizan`
2. Extract the project tarball and push:
   ```bash
   tar -xzf mizan-project.tar.gz
   cd mizan-project
   git init
   git add .
   git commit -m "Initial commit — Mizan EGX Portfolio Tracker"
   git remote add origin https://github.com/YOUR_USERNAME/mizan.git
   git push -u origin main
   ```

---

## Step 5: Deploy on Render (5 minutes)

1. Log in to **render.com**
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub → Select the `mizan` repository
4. Configure:
   - **Name**: `mizan-api`
   - **Region**: Frankfurt (EU Central)
   - **Root Directory**: `backend`
   - **Runtime**: Docker
   - **Instance Type**: **Free**
5. Add **Environment Variables** (click "Advanced" → "Add Environment Variable"):

   | Key | Value |
   |-----|-------|
   | `SPRING_PROFILES_ACTIVE` | `render` |
   | `DB_HOST` | *(from Neon step 2)* |
   | `DB_PORT` | `5432` |
   | `DB_NAME` | *(from Neon)* |
   | `DB_USER` | *(from Neon)* |
   | `DB_PASS` | *(from Neon)* |
   | `REDIS_HOST` | *(from Upstash step 3)* |
   | `REDIS_PORT` | `6379` |
   | `REDIS_PASSWORD` | *(from Upstash)* |
   | `JWT_SECRET` | *(paste any 64+ character random string)* |
   | `GOOGLE_CLIENT_ID` | *(from Google Cloud Console, or `skip` for now)* |

6. Click **"Create Web Service"**
7. Wait for build (~3-5 minutes for first deploy)
8. Your API will be live at: `https://mizan-api.onrender.com`

---

## Step 6: Verify it works

Open in your browser:
```
https://mizan-api.onrender.com/api/actuator/health
```

You should see:
```json
{"status": "UP"}
```

Test the market data endpoint (once you have a Twelve Data API key):
```bash
curl https://mizan-api.onrender.com/api/market/validate-key \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "your-twelve-data-key"}'
```

---

## Step 7: Test the mobile app (5 minutes)

The fastest way to test on your phone WITHOUT Xcode or Android Studio:

1. Install **Expo Go** app on your phone (App Store / Play Store)
2. In the mobile project:
   ```bash
   cd mobile
   cp .env.example .env
   ```
3. Edit `.env`:
   ```
   API_BASE_URL=https://mizan-api.onrender.com/api
   ```
4. Run:
   ```bash
   npx expo start
   ```
5. Scan the QR code with your phone → app opens in Expo Go

---

## Important: Free Tier Limitations

### Render
- Service **sleeps after 15 minutes** of no traffic
- First request after sleep takes **~30-50 seconds** (cold start)
- Solution: Use a free uptime monitor like **UptimeRobot** to ping `/api/actuator/health` every 14 minutes — this keeps the service awake

### Neon
- **0.5 GB** storage (plenty for a portfolio tracker)
- Compute auto-suspends after 5 min idle, resumes in ~1 second
- No issue for this app — Neon cold starts are fast

### Upstash
- **10,000 commands/day** on free tier
- Each price check = ~5-10 Redis commands
- That's ~1,000-2,000 price refreshes per day — enough for many users

---

## Optional: Keep Render awake for free

Sign up at **https://uptimerobot.com** (free):
1. Add a new monitor
2. Type: HTTP(s)
3. URL: `https://mizan-api.onrender.com/api/actuator/health`
4. Interval: 5 minutes
5. This pings your API every 5 minutes, preventing it from sleeping

---

## Architecture on free tier

```
Phone (Expo Go)
   │
   ▼ HTTPS
Render.com (Spring Boot — free)
   │              │
   ▼              ▼
Neon.tech       Upstash.com
(PostgreSQL)    (Redis)
   free           free
                   │
              ┌────┴────┐
              │  Cache   │
              │ 5-min TTL│
              └────┬────┘
                   │ cache miss
                   ▼
            Twelve Data API
            (user's own key)
```

---

## What to do next

1. **Get Google OAuth credentials**: Go to https://console.cloud.google.com → Create project → OAuth consent screen → Create OAuth 2.0 Client ID → Add to Render env vars
2. **Get a Twelve Data API key**: https://twelvedata.com → Sign up free → Copy API key → Use in the app's Settings screen
3. **Port the React artifact UI to React Native**: The dashboard, portfolio, and risk screens from the browser prototype need to be converted to React Native components
