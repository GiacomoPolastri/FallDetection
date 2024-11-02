# Fall Detection App

## Description
This Android application is designed to detect falls using the device's sensors, such as the accelerometer. In the event of a fall, the app sends a notification to alert the user or other people. It is particularly useful for monitoring the safety of elderly individuals or those who need assistance in case of accidents.

## Key Features
- **Fall Detection**: Utilizes the device's sensors, such as the accelerometer, to detect sudden movements that indicate a fall.
- **Notifications**: Sends push notifications when a fall is detected.
- **Alert System**: Potential feature to send alerts to emergency contacts (planned for future implementation).
- **Emergency Calls, SMS, and Emails**: Includes functionality to initiate emergency calls, send SMS messages, and send email notifications when a fall is detected.

## Technical Overview
### Fall Detection Algorithm
The fall detection algorithm in this app uses data from the device's accelerometer to detect rapid changes in acceleration, which are indicative of a fall. The algorithm monitors the sensor data continuously, looking for a sudden drop followed by a lack of movement, which typically indicates that the user has fallen and is immobile. The sensitivity of the fall detection can be adjusted within the code to cater to different user needs, minimizing false positives or enhancing sensitivity for frail users.

### Sensor Usage
- **Accelerometer**: The app makes use of the accelerometer to capture changes in movement and orientation. It monitors the acceleration along different axes (X, Y, Z) to determine if the detected movement matches the pattern of a fall.
- **Sensor Event Listener**: A `SensorEventListener` is implemented to handle changes in sensor data in real-time. The app processes these changes to determine whether a fall has occurred.

### Notification and Alert Functions
- **Local Notifications**: When a potential fall is detected, the app creates a notification using the Android `NotificationManager`. The notification is designed to alert both the user and potentially nearby individuals.
- **Emergency Calls**: The app has the capability to initiate an emergency call when a fall is confirmed. This is achieved by using an `Intent` with the `ACTION_CALL` action, which starts a phone call to a predefined emergency contact number.
- **SMS and Email Alerts**: In addition to notifications, the app can send an SMS to a predefined contact number using the `SmsManager` API. Similarly, email notifications can be generated using an `Intent` with `ACTION_SENDTO`, allowing for quick alerts to multiple contacts.

## Requirements
- **Android SDK**: Minimum compatible version specified in `build.gradle`.
- **Required Permissions**:
  - `Manifest.permission.ACCESS_FINE_LOCATION`
  - `Manifest.permission.SEND_SMS`
  - `Manifest.permission.CALL_PHONE`
  - `Manifest.permission.INTERNET`
  - `Manifest.permission.RECEIVE_BOOT_COMPLETED`

## Installation
1. Clone the repository:
   ```sh
   git clone <repository_url>
   ```
2. Open the project in Android Studio.
3. Build and install the app on your Android device.

## Usage
- The app will automatically start monitoring for falls once launched.
- In the event of a detected fall, a notification will be sent, and the configured emergency procedures (such as SMS, email, or calls) will be triggered.
- Further settings and configuration can be adjusted directly in the code (future in-app settings UI planned).

## Customization
You can adjust the sensitivity of fall detection by modifying the parameters in the `MainActivity.kt` file. The specific values used for fall detection can be calibrated based on the target user to improve accuracy and reduce false alarms.

## Contributions
Contributions, bug reports, and improvements are welcome! Feel free to open an issue or submit a pull request.

## License
This project is licensed under the MIT License. See the `LICENSE` file for more details.

## Contact
For further information or questions, contact us at: [giacomo.polastri@gmail.com](mailto:giacomo.polastri@gmail.com).
