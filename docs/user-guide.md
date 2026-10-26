# User guide

## First launch

LibreSpeedo supports Android 8.0 (API 26) and newer. Grant location access when prompted and turn on Android location services. Precise location generally provides better speed and trip measurements; approximate location may leave measurements unavailable or unsuitable for automatic start.

The app requests location access for the visible app. Bluetooth access is optional and used for the selected paired car. On Android 12 and newer, allow the Bluetooth/Nearby devices permission when prompted. Manual trip recording does not require Bluetooth permission. There is no background location permission or recording service.

## Dashboard

The default grid contains digital speed, compass, position, and debug widgets. Available additions include an analog speedometer, analog compass, chronometer, and Hello World demonstration widget. A separate Trip logger card provides recording controls and history access.

1. Open Settings and enable **Edit Dashboard**.
2. Return to the dashboard to add or remove widgets, change card sizes, and drag widgets into a new order.
3. Use widget settings to choose speed units and preview/save speedometer styles.
4. Disable Edit Dashboard when finished. **Reset Dashboard Layout** in Settings restores the default widgets and sizes after confirmation.

Cards support half-width (`1x1`), wide (`2x1`), and large (`2x2`) layouts. Digital styles are Standard and 7-segment. Analog styles are Classic, Sport, Minimal, and Cat. Speed units are km/h, mph, and m/s; trip summaries currently use km/h and km independently of the widget preference.

The compass uses GPS bearing above 3 km/h when available and hardware heading otherwise. Nearby magnets, device orientation, and sensor availability affect heading quality. The status bar reports location and motion availability. Speed widgets show unavailable measurements rather than treating a missing fix as a measured stop; fixes expire after five seconds.

The chronometer has its own start/pause, reset, and lap controls. Its state survives widget removal/re-addition, navigation, and rotation within the same ViewModel lifetime. It is not saved across process restarts and does not start or finish trips.

Settings also offers a pure-black OLED theme and **Keep Screen On** (enabled by default while the app is open).

Settings update without restarting the app, including after rotation. Automation, selected car, and retention changes reach the recorder immediately. Route storage and mounting confirmation are captured when each trip starts; finish the current trip and start another to use new values.

## Record a trip manually

1. Wait for the Trip logger card to finish loading saved trips.
2. Tap **Start trip**. Timing begins immediately, including time before movement.
3. Keep LibreSpeedo visible and tap **Finish** to save the trip.
4. Open **History** and select the saved trip for details.

Rotation and navigation to Settings or History preserve a recording. Leaving the app or locking the screen ends it as interrupted when the activity stops. Returning starts a new opportunity to record; it does not resume the ended trip. There is no auto-launch or recording while another app, such as Maps, is foreground. Keeping the app in Recents is insufficient.

After a crash or process restart, an unfinished trip is recovered as interrupted using its last saved measurement checkpoint. Time spent outside the recording session is not added to its duration. Checkpoints normally occur about every five seconds while samples arrive, so unsaved data can be lost.

## Automatic logging with a car

1. Pair the car using Android's Bluetooth settings.
2. In LibreSpeedo Settings, grant Bluetooth permission if requested, use **Choose paired car**, and select the device. Use **Refresh devices** if necessary.
3. Enable **Automatic trip logging** and keep LibreSpeedo visible.
4. When the selected car is connected, detection is armed. Accepted non-network speed measurements above 8 km/h for at least five seconds start a trip, backdated to the first qualifying sample. Poor accuracy or gaps in the candidate samples reset confirmation.
5. Finish manually, or disconnect the selected car. A 60-second grace period lets the same car reconnect without splitting the trip. If it expires, the saved trip ends at the original disconnect boundary and excludes the grace tail.

Paired and connected are different states. The app checks connected audio/headset profiles at startup and observes connection broadcasts for the selected address. Some devices cannot report a definite connection through these APIs; use manual recording when the connection is unknown. Turning Bluetooth off is treated as connection loss. Disabling automation ends an automatic trip; manual trips remain independent of the car.

## Read trip details

Trip details include top and average speed, total time, stopped time, distance, horizontal G summaries, directional peaks, jerk, motion distribution/exposure statistics, profiles, and detected maneuvers. Moving and unknown durations are retained in the database but do not currently have separate detail rows.

- **Speed coverage:** the fraction of trip time with usable adjacent speed samples. Average speed is calculated over covered time, including accepted stops; it may describe only part of the trip.
- **Movement coverage:** the fraction classified as moving or stopped. Missing or ambiguous measurements contribute to unknown time.
- **Motion coverage:** the fraction with usable horizontal acceleration intervals. Missing G values mean unavailable data, not zero acceleration.

Distance is estimated by integrating accepted speed. Speed gaps longer than three seconds are not bridged. Stopped classification requires speed below 1 km/h for five seconds; moving classification requires above 3 km/h for two seconds.

Use a rigid mount and enable the mounting confirmation setting before starting a trip for directional motion analysis. Orientation, reliable sensors, fresh course data, and movement are also required for directional features and event detection. A phone without suitable sensors can still record speed and time. Phone handling, bumps, magnetic interference, and uncertain GPS can affect results. Detected acceleration, braking, cornering, and jerk events are experimental evidence; no driving score is calculated.

## Storage, deletion, and export

Settings and trips are stored in the app's private local storage. Automatic logging is off by default. **Store trip route** is off by default; enabling it permits coordinates to be stored with trip samples. Route and mounting settings are captured at trip start. Turning route storage off does not erase previously saved coordinates.

The retention control cycles through 7, 30, 90 days, and indefinite storage; the default is 30 days. Cleanup runs when retention is loaded or changed, rather than through a background scheduled job. Delete a trip from History to remove its samples, motion chunks, events, and rating rows through cascading deletion.

Use **Export trip database** in Settings and select a destination in Android's document picker. The default filename is `librespeedo-trips.db`. The result is a standalone SQLite snapshot containing saved trips, samples, compressed motion chunks, events, and any stored routes. It also contains identifying metadata such as the selected car address for automatic trips. It does not export dashboard preferences. Export checkpoints an active trip without intentionally finishing it; normal visibility rules still apply if the document picker causes the activity to stop. There is no import or restore interface.

The app has no Internet permission or built-in upload service. Android backup is enabled in the manifest, and its backup rules do not explicitly exclude settings or trip data; system backup and device-transfer behavior depends on the device configuration. Exported copies are independent files and are not removed by deleting trips inside the app.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Speed unavailable | Grant location permission, enable location services, and wait for a fresh fix with speed. Try an open-sky location. |
| Automatic trip does not start | Check Bluetooth permission, selected car, connected status, app visibility, and sustained accurate movement above the start threshold. |
| No car in the picker | Pair it in Android settings first, then refresh the device list. |
| G metrics or events unavailable | Check sensor support, rigid mounting confirmation, orientation quality, course/speed coverage, and warm-up time. |
| Trip marked interrupted | Check whether the app was backgrounded, location permission was lost, or an unfinished trip was recovered. |
| Save/export error | Read the displayed error, check available storage or the export destination, and retry when possible. An error does not guarantee recent recording data was saved. |
