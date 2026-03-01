# Bay Area Concert API

An Akka HTTP backend that proxies the Ticketmaster Discovery API, paired with a SwiftUI iOS app that lists upcoming Bay Area music concerts.

## Architecture

```
iOS App (SwiftUI)
  └── GET http://localhost:8080/api/concerts
        └── Akka HTTP server (this repo)
              └── GET https://app.ticketmaster.com/discovery/v2/events.json
```

---

## Prerequisites

| Tool | Purpose |
|---|---|
| JDK 8+ & sbt | Run the Akka HTTP backend |
| Ticketmaster API key | Free at [developer.ticketmaster.com](https://developer.ticketmaster.com) |
| Xcode 15+ (macOS) | Build and run the iOS app |

---

## 1 — Run the Backend

### Set your API key

```bash
export TICKETMASTER_API_KEY=your_key_here
```

Or edit `src/main/resources/application.conf` directly:

```
ticketmaster {
  api-key = "your_key_here"
}
```

### Start the server

```bash
sbt "runMain part5_concert.BayAreaConcertApi"
```

You should see:

```
Bay Area Concert API running → http://localhost:8080/api/concerts
```

---

## 2 — Test the Backend

### Smoke test — check the server responds

```bash
curl -i http://localhost:8080/api/concerts
```

Expected: `HTTP/1.1 200 OK` with a JSON array.

### Pretty-print the response

```bash
curl -s http://localhost:8080/api/concerts | python3 -m json.tool
```

### Inspect a single concert field

```bash
curl -s http://localhost:8080/api/concerts | python3 -c "
import sys, json
concerts = json.load(sys.stdin)
print(f'Found {len(concerts)} concerts')
for c in concerts[:3]:
    print(f\"  {c['date']}  {c['name']}  @  {c['venue']}\")
"
```

### Expected response shape

```json
[
  {
    "name":     "Artist Name",
    "date":     "2026-03-15",
    "time":     "20:00:00",
    "venue":    "Chase Center",
    "url":      "https://www.ticketmaster.com/event/...",
    "imageUrl": "https://s1.ticketimg.com/..."
  }
]
```

### Test error handling — stop the server and confirm 503 behaviour, or pass a bad key

```bash
TICKETMASTER_API_KEY=bad_key sbt "runMain part5_concert.BayAreaConcertApi"
curl -i http://localhost:8080/api/concerts
# → HTTP/1.1 500 Internal Server Error
```

---

## 3 — Set Up the iOS App in Xcode

1. Open Xcode → **File › New › Project**
2. Choose **iOS › App**, click Next
3. Set:
   - **Product Name**: `BayAreaConcerts`
   - **Interface**: SwiftUI
   - **Language**: Swift
4. Save the project anywhere on your Mac
5. Replace the generated source files with the ones in `ios/BayAreaConcerts/`:

```
ios/BayAreaConcerts/
├── BayAreaConcertsApp.swift   → replace generated App file
├── Info.plist                 → merge NSAppTransportSecurity key (see below)
├── Models/
│   └── Concert.swift          → add to project
├── Services/
│   └── ConcertService.swift   → add to project
└── Views/
    ├── ContentView.swift      → replace generated ContentView
    └── ConcertRow.swift       → add to project
```

### Merge the Info.plist ATS entry

In your Xcode project's `Info.plist`, add the following key to allow plain HTTP to localhost (development only):

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>
```

> **Note:** Remove this before submitting to the App Store. In production, serve the API over HTTPS.

---

## 4 — Run and Test the iOS App

### In the simulator

1. Make sure the backend is running (step 1)
2. Select any iPhone simulator in Xcode's device picker
3. Press **Cmd+R** to build and run

The app fetches concerts on launch and displays them in a scrollable list.

### On a real device

`localhost` only works in the simulator. For a physical device, find your Mac's local IP:

```bash
ipconfig getifaddr en0   # Wi-Fi
```

Then update `ConcertService.swift`:

```swift
private let endpoint = URL(string: "http://192.168.1.x:8080/api/concerts")!
```

### Manual test checklist

- [ ] App launches and shows a loading spinner
- [ ] Concert list appears within a few seconds
- [ ] Each row shows the concert name, formatted date, and venue
- [ ] Tapping "Get Tickets →" opens the Ticketmaster page in Safari
- [ ] Pull-to-refresh (or the toolbar button) re-fetches the list
- [ ] Stopping the backend and refreshing shows the error state with a "Try Again" button
- [ ] Tapping "Try Again" retries the request
