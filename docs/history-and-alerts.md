# Sensor history and mobile alerts

This document describes the history and alert features added in commit `114883d`
(branch `feat/history-and-alerts`) and how to test them.

## What was added

| Feature | Summary |
| --- | --- |
| History logging | The ESP32 saves a sensor snapshot to Firebase every 5 minutes. |
| Alert events | The ESP32 saves an alert entry when a reading goes out of range. |
| History screen | A per-device screen in the app lists past readings and alerts. |
| Mobile alerts | A foreground service turns new alert entries into phone notifications. |
| Settings toggle | A "Mobile alerts" switch in Settings turns notifications on or off. |
| Unpair cleanup | Unpairing a device also deletes its reading history. |

## How it works

### Firebase data

Both logs are written by the device and read by the app.

```
/devices/{uid}/history/{macKey}/{pushId}    one sensor snapshot
    temperatureCelsius, humidityPercent, airQualityAdc, trayStatus, timestamp, deviceOnline

/devices/{uid}/alerts/{pushId}              one alert, shared by all of the user's devices
    macKey, type, value, timestamp
```

`macKey` is the device MAC address with `:` replaced by `-`. `timestamp` is the
Firebase server time in milliseconds.

### Firmware (`smartnest-firmware/smartnest-firmware.ino`)

- **History:** the first snapshot is logged as soon as Firebase is ready and the
  DHT22 has a valid reading. After that, one snapshot is logged every 5 minutes
  (`HISTORY_INTERVAL_MS`). A failed write is retried after 30 seconds.
- **Alerts:** an entry is written when a condition turns on, not while it stays
  on. Each alert type waits at least 5 minutes (`ALERT_COOLDOWN_MS`) before it
  can be written again, so a value hovering at the limit does not spam.

| Alert type | Condition |
| --- | --- |
| `HIGH_TEMP` | temperature ≥ 32.0 °C |
| `LOW_HUMIDITY` | humidity ≤ 40 % |
| `HIGH_HUMIDITY` | humidity ≥ 70 % |
| `POOR_AIR` | MQ135 reading ≥ 1000 ADC |
| `TRAY_DIRTY` | TCRT5000 pin reads `LOW` |

### Android app

- **`alerts/AlertMonitorService`** is a foreground service that listens to
  `/devices/{uid}/alerts` and posts a notification for each new entry. It
  starts when the dashboard opens, if alerts are enabled and a user is signed
  in. It stops on logout or when the Settings switch is turned off.
- **`alerts/AlertNotifier`** creates two notification channels: "Alert
  monitoring" (the persistent low-priority notification) and "Cage alerts"
  (the high-priority alerts). There is one notification per device and alert
  type, so a repeat replaces the previous one. Tapping an alert opens the
  History screen on the Alerts tab.
- **`alerts/AlertPrefs`** stores the on/off switch (on by default), whether the
  permission prompt was already shown, and the timestamp of the last alert
  notified.
- **`ui/history/`** holds the History screen. It has a Readings tab and an
  Alerts tab, and a filter for "Last 24 hours" or "Last 7 days". Entries are
  listed newest first under day headers.
- **`data/repository/HistoryRepository`** loads readings and alerts. Each time
  the History screen opens, it deletes readings older than 7 days and alerts
  older than 30 days, because there is no server doing this.
- **Dashboard:** the device dialog has a new "History" button. On Android 13
  and later, the dashboard asks for the notification permission once.
- **Settings:** a new NOTIFICATIONS section holds the "Mobile alerts" switch.
- **Manifest:** adds `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` and
  `FOREGROUND_SERVICE_DATA_SYNC`, and registers the service and the History
  activity.

When the service starts, it resumes from the last alert it notified, but never
from more than 1 hour back. On the very first run it looks back 2 minutes.

## Before you test

1. Check out the branch: `git switch feat/history-and-alerts`.
2. Build and install the app from Android Studio, or run `./gradlew installDebug`.
3. Flash the updated firmware to the ESP32 with the Arduino IDE. Keep
   `secrets.h` next to the sketch; it is ignored by git.
4. Open the Arduino Serial Monitor so you can see the firmware logs.
5. Open the Firebase console at Realtime Database, and find your user node at
   `/devices/{uid}`.
6. In the Realtime Database rules, add a `timestamp` index for both logs. The
   app queries work without it, but they download every entry and Logcat warns
   about an unspecified index.

   ```json
   "history": { "$macKey": { ".indexOn": ["timestamp"] } },
   "alerts": { ".indexOn": ["timestamp"] }
   ```

To avoid waiting 5 minutes between checks, you can temporarily lower
`HISTORY_INTERVAL_MS` and `ALERT_COOLDOWN_MS` in the sketch, for example to
`30UL * 1000UL`. To trigger an alert without heating the sensor, temporarily
set a limit such as `TEMP_LIMIT` below room temperature. Restore the values
when you are done.

## Test steps

### 1. History logging (firmware)

1. Power the device and wait for it to connect to Wi-Fi.
2. Watch the Serial Monitor.
   - Expected: `History reading logged.` appears shortly after the device connects.
3. Open `/devices/{uid}/history/{macKey}` in the Firebase console.
   - Expected: one entry with temperature, humidity, air quality, tray status
     and a timestamp.
4. Wait 5 minutes.
   - Expected: a second entry appears, and the log line is printed again.

### 2. Alert events (firmware)

1. Push one reading out of range. For example, warm the DHT22 above 32 °C or
   lower `TEMP_LIMIT`.
   - Expected: the Serial Monitor prints `Alert logged: HIGH_TEMP`.
2. Open `/devices/{uid}/alerts` in the Firebase console.
   - Expected: one entry with `macKey`, `type`, `value` and `timestamp`.
3. Keep the reading out of range for a minute.
   - Expected: no further entries for that type.
4. Bring the reading back in range, then out of range again within 5 minutes.
   - Expected: no new entry, because of the cooldown.
5. Repeat step 4 after 5 minutes have passed.
   - Expected: a new entry is written.
6. Repeat for the other types if you can trigger them: humidity, air quality
   and the tray sensor.

### 3. History screen (app)

1. Sign in and open the dashboard.
2. Tap a device card to open its dialog, then tap **History**.
   - Expected: the History screen opens with the device name as the subtitle
     and the Readings tab selected.
3. Check the Readings tab.
   - Expected: the logged snapshots, newest first, under a day header.
4. Tap the **Alerts** tab.
   - Expected: the alerts from test 2, each with its icon, title and message.
5. Switch between **Last 24 hours** and **Last 7 days**.
   - Expected: the list reloads for the chosen period.
6. Open History for a device that has no data yet.
   - Expected: "No readings yet" on the Readings tab and "No alerts" on the
     Alerts tab.
7. Turn on airplane mode and open History.
   - Expected: the list shows cached data or the "Couldn't load history" state,
     and the app does not crash.

### 4. Mobile alert notifications (app)

1. On Android 13 or later, install the app fresh and sign in.
   - Expected: the dashboard asks for the notification permission once. Allow it.
2. Pull down the notification shade.
   - Expected: a persistent "SmartNest alerts are on" notification.
3. Trigger an alert on the device, as in test 2.
   - Expected: a notification titled like "High temperature • {device name}"
     arrives within a few seconds.
4. Tap the notification.
   - Expected: the History screen opens on the Alerts tab for that device.
5. Press Home so the app is in the background, then trigger a different alert type.
   - Expected: the notification still arrives.
6. Trigger the same alert type again after the cooldown.
   - Expected: the new notification replaces the old one for that type.

You can also test notifications without the hardware. In the Firebase console,
add a child under `/devices/{uid}/alerts` with these fields:

```json
{
  "macKey": "AA-BB-CC-DD-EE-FF",
  "type": "HIGH_TEMP",
  "value": 35.2,
  "timestamp": 1790000000000
}
```

Use the `macKey` of a paired device and the current time in milliseconds for
`timestamp`. An entry with an older timestamp than the last notified alert is
ignored. Valid types are `HIGH_TEMP`, `LOW_HUMIDITY`, `HIGH_HUMIDITY`,
`POOR_AIR` and `TRAY_DIRTY`.

### 5. Settings toggle

1. Open **Settings** and find the NOTIFICATIONS section.
   - Expected: the "Mobile alerts" switch is on.
2. Turn the switch off.
   - Expected: the "SmartNest alerts are on" notification disappears.
3. Trigger an alert.
   - Expected: no notification. The alert still appears in the History screen.
4. Close and reopen the app.
   - Expected: the switch is still off and monitoring does not restart.
5. Turn the switch back on.
   - Expected: the persistent notification returns and new alerts notify again.
6. Deny the notification permission in system settings, then turn the switch
   off and on.
   - Expected: the app asks for the permission again, or shows "Notifications
     are blocked. Allow them in system settings to receive alerts."

### 6. Logout and unpair

1. Log out from the dashboard.
   - Expected: the persistent notification disappears and no alerts notify.
2. Sign in again, then unpair a device that has history.
   - Expected: `/devices/{uid}/history/{macKey}` is removed in the Firebase console.

### 7. Retention

1. In the Firebase console, add a history entry with a `timestamp` more than
   7 days old, and an alert entry more than 30 days old.
2. Open the History screen for that device.
   - Expected: both old entries are deleted from the database shortly after.

## Known limitations

- Alerts are kept for 30 days, but the Alerts tab only shows up to the last
  7 days.
- Unpairing deletes a device's readings but not its entries in the shared
  `/alerts` node. Those are removed by the 30-day cleanup.
- The cleanup runs only when the History screen is opened, and its alert pass
  covers all of the user's devices.
- If Android stops the service while the app is in the background, monitoring
  resumes the next time the app is opened. Alerts older than 1 hour at that
  point are not notified, but they still appear in History.
- No history is logged while the DHT22 reading is invalid.
