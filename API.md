# TR Tier Tagger - API Documentation

This document explains how TR Tier Tagger fetches and processes player tier data from the backend API.

## API Overview

**Base URL:** `https://loratech.dev/api`

The mod uses Java's built-in `HttpClient` to make asynchronous HTTP requests to the API.

---

## Endpoints

### 1. Overall Ranking Endpoint

**Endpoint:** `GET /overall-ranking`

**Full URL:** `https://loratech.dev/api/overall-ranking`

**Description:** Fetches all players and their tier rankings across all game modes (kits).

### 2. Mojang UUID Endpoint

**Endpoint:** `GET /users/profiles/minecraft/{username}`

**Full URL:** `https://api.mojang.com/users/profiles/minecraft/{username}`

**Description:** Fetches player UUID from Mojang API for skin loading.

**Response Format:**
```json
{
  "name": "PlayerName",
  "id": "069a79f444e94726a5befca90e38aaf5"
}
```

The `id` field is the UUID without dashes, which is then formatted to standard UUID format.

### 3. MC-Heads Skin API

**Endpoint:** `GET /body/{uuid}/right`

**Full URL:** `https://mc-heads.net/body/{uuid}/right`

**Description:** Fetches player skin render (full body, right-facing view) as PNG image.

#### Response Format

The API returns a JSON array of player objects:

```json
[
  {
    "name": "PlayerName",
    "totalPoints": 350,
    "kitTiers": {
      "sword": "HT1",
      "pot": "LT2",
      "axe": "HT3",
      "uhc": "LT1"
    },
    "region": "TR",
    "position": 5,
    "title": {
      "name": "Combat Master",
      "points": 250
    }
  },
  {
    "name": "AnotherPlayer",
    "totalPoints": 120,
    "kitTiers": {
      "sword": "LT3",
      "gapple": "HT4"
    },
    "region": "EU",
    "position": 42,
    "title": {
      "name": "Combat Ace",
      "points": 100
    }
  }
]
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Player's Minecraft username |
| `totalPoints` | Integer | Total points earned across all kits |
| `kitTiers` | Object | Map of kit IDs to tier strings (e.g., "HT1", "LT2") |
| `region` | String | Player's region code (TR, EU, NA, AS, etc.) |
| `position` | Integer | Overall ranking position |
| `title` | Object | Player's title information |
| `title.name` | String | Title name (e.g., "Combat Master") |
| `title.points` | Integer | Points required for this title |

#### Tier String Format

Tier strings follow the format: `[H/L]T[1-5]`

- **First character:** `H` (High) or `L` (Low)
- **Second character:** Always `T` (Tier)
- **Third character:** Tier number (1-5, where 1 is best)

**Examples:**
- `HT1` = High Tier 1 (best possible rank)
- `LT1` = Low Tier 1
- `HT2` = High Tier 2
- `LT5` = Low Tier 5 (lowest rank)

---

## Data Processing

### 1. Initial Data Fetch

When the mod starts, it calls `TRTierCache.init()` which:

1. Sends GET request to `/overall-ranking`
2. Validates HTTP 200 response
3. Parses JSON array into `List<TRTierPlayer>` objects
4. Stores players in a concurrent hash map (indexed by lowercase username)
5. Extracts all unique kit IDs from player data
6. Creates `GameMode` objects for each kit

### 2. Player Data Model

The mod uses several model classes to represent data:

#### TRTierPlayer (Raw API Response)
```java
record TRTierPlayer(
    String name,
    int totalPoints,
    Map<String, String> kitTiers,  // kitId -> tierString (e.g., "HT1")
    String region,
    int position,
    TitleInfo title
)
```

#### PlayerInfo.Ranking (Processed Tier Data)
```java
record Ranking(
    int tier,        // 1-5 (extracted from "HT1" -> 1)
    int pos,         // 0=High, 1=Low (extracted from "HT1" -> 0)
    Integer peakTier,
    Integer peakPos,
    long attained,
    boolean retired
)
```

### 3. Tier String Parsing

The mod parses tier strings like "HT1" into structured data:

```java
// "HT1" becomes:
// tier = 1 (from "T1")
// pos = 0 (from "H", where H=0, L=1)

// "LT3" becomes:
// tier = 3 (from "T3")
// pos = 1 (from "L")
```

### 4. Kit/Game Mode Mapping

The mod recognizes these kit IDs and displays them with custom icons:

| Kit ID | Display Name | Icon |
|--------|--------------|------|
| `sword` | Sword | ⚔️ |
| `pot` | Pot | 🧪 |
| `axe` | Axe | 🪓 |
| `uhc` | UHC | ❤️ |
| `mace` | Mace | 🔨 |
| `nethpot` | Neth Pot | 🔥 |
| `gapple` | Gapple | 🍎 |
| `crystal` | Crystal | 💎 |
| `smp` | SMP | 🌍 |
| `diasmp` | Dia SMP | 💠 |

### 5. Region Colors

Players are color-coded by region:

| Region | Color (Hex) | Description |
|--------|-------------|-------------|
| TR | `#ff0000` | Turkey (Red) |
| EU | `#6aff6e` | Europe (Green) |
| NA | `#ff6a6e` | North America (Pink) |
| AS | `#c27ba0` | Asia (Purple) |
| SA | `#ff9900` | South America (Orange) |
| AU | `#f6b26b` | Australia (Tan) |
| ME | `#ffd966` | Middle East (Yellow) |
| AF | `#674ea7` | Africa (Dark Purple) |

### 6. Point-Based Titles

Players receive titles based on total points:

| Points | Title | Color |
|--------|-------|-------|
| 400+ | Combat Grandmaster | Gold |
| 250+ | Combat Master | Orange |
| 100+ | Combat Ace | Pink |
| 50+ | Combat Specialist | Purple |
| 20+ | Combat Cadet | Blue |
| 10+ | Combat Novice | Light Blue |
| 1+ | Rookie | Gray |
| 0 | Unranked | White |

---

## Caching Strategy

### In-Memory Cache

The mod maintains several caches:

1. **Players by Name:** `Map<String, TRTierPlayer>`
   - Key: Lowercase username
   - Value: Full player data

2. **Player Rankings by UUID:** `Map<UUID, Map<String, Ranking>>`
   - Key: Player UUID
   - Value: Map of kit IDs to rankings

3. **Game Modes:** `List<GameMode>`
   - All available kits/game modes

4. **UUID Cache:** `Map<String, String>` (in UUIDFetcher)
   - Key: Lowercase username
   - Value: Formatted UUID string
   - Prevents repeated Mojang API calls

5. **Skin Texture Cache:** `Map<String, Identifier>` (in SkinTextureLoader)
   - Key: Lowercase UUID
   - Value: Minecraft texture identifier
   - Prevents re-downloading skin images

### Cache Lifecycle

- **Initialization:** On mod startup
- **Refresh:** Can be triggered manually (not automatic)
- **Lookup:** O(1) username lookups via hash map
- **Thread-Safe:** Uses `ConcurrentHashMap` for concurrent access

---

## Error Handling

The mod handles various error scenarios:

1. **HTTP Errors:** Logs warning if status code != 200
2. **Invalid JSON:** Validates response starts with `[`
3. **Empty Response:** Checks for null/empty body
4. **Network Failures:** Catches exceptions and logs errors
5. **Missing Data:** Returns empty optionals for unknown players

---

## Usage in Game

### Player List Display

When viewing the player list (Tab menu):
1. Mod gets player's username
2. Looks up player in cache by name
3. Retrieves tier for currently selected game mode
4. Displays tier prefix before username (e.g., "HT1 | PlayerName")

### Player Search

Players can search for specific users:
1. Enter username in search screen
2. Mod looks up player in local cache
3. Displays all tiers, region, points, and title
4. Shows tier icons and colors

---

## Configuration

Users can configure:

- **API URL:** Custom API endpoint (default: `https://loratech.dev/api`)
- **Game Mode:** Which kit to display in player list
- **Show Retired:** Whether to show retired tiers
- **Tier Colors:** Custom colors for each tier level
- **Show Icons:** Toggle kit icons on/off

---

## Example Flow

### Basic Tier Display Flow

1. **Mod Starts**
   ```
   TRTier.init() → TRTierCache.init() → fetchAllPlayers()
   ```

2. **API Request**
   ```
   GET https://loratech.dev/api/overall-ranking
   ```

3. **Response Received**
   ```json
   [{"name": "Kynux", "totalPoints": 350, "kitTiers": {"sword": "HT1"}, ...}]
   ```

4. **Data Parsed**
   ```
   TRTierPlayer(name="Kynux", totalPoints=350, kitTiers={sword=HT1})
   ```

5. **Stored in Cache**
   ```
   PLAYERS_BY_NAME.put("kynux", playerData)
   ```

6. **Player Joins Server**
   ```
   getPlayerByName("Kynux") → Returns cached data
   ```

7. **Display in Tab List**
   ```
   "HT1 | Kynux" (with colored tier and icon)
   ```

### Player Info Screen with Skin Flow

1. **User Opens Player Info Screen**
   ```
   searchPlayer("Kynux") called
   ```

2. **Fetch UUID from Mojang**
   ```
   GET https://api.mojang.com/users/profiles/minecraft/Kynux
   Response: {"name": "Kynux", "id": "069a79f444e94726a5befca90e38aaf5"}
   ```

3. **Format UUID**
   ```
   "069a79f444e94726a5befca90e38aaf5" → "069a79f4-44e9-4726-a5be-fca90e38aaf5"
   ```

4. **Create PlayerInfo with UUID**
   ```
   PlayerInfo(uuid="069a79f4-44e9-4726-a5be-fca90e38aaf5", name="Kynux", ...)
   ```

5. **Load Skin Texture**
   ```
   GET https://mc-heads.net/body/069a79f4-44e9-4726-a5be-fca90e38aaf5/right
   Response: PNG image data
   ```

6. **Register Texture**
   ```
   NativeImage.read(inputStream) → NativeImageBackedTexture
   TextureManager.registerTexture(identifier, texture)
   ```

7. **Display in Screen**
   ```
   Render player info with skin texture on left side
   ```

---

## Technical Details

- **HTTP Client:** Java 11+ `java.net.http.HttpClient`
- **JSON Parser:** Google Gson
- **Async Processing:** `CompletableFuture` for non-blocking requests
- **Thread Safety:** `ConcurrentHashMap` for concurrent access
- **Locale Handling:** All username lookups use `toLowerCase(Locale.ROOT)`

---

## Future API Considerations

If the API structure changes, update these files:
- `TRTierCache.java` - Main fetching logic
- `TRTierPlayer.java` - API response model
- `PlayerInfo.java` - Processed data model
- `GameMode.java` - Kit/mode definitions
- `UUIDFetcher.java` - UUID fetching from Mojang
- `SkinTextureLoader.java` - Skin texture loading
