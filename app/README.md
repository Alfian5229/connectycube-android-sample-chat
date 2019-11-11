## ConnectyCybe Android Chat code sample

This README introduces [ConnectyCube](https://connectycube.com/) Chat code sample written in Java lang.

Project contains XMPP instant messaging implementation between multiple users.

Original integration guide and API documentation - https://developers.connectycube.com/android/messaging

<p align="center">
<img src="https://developers.connectycube.com/docs/_images/code_samples/android_codesample_chat_java_demo2.jpg" width="250" alt="Chat code sample demo image">
<img src="https://developers.connectycube.com/docs/_images/code_samples/android_codesample_chat_java_demo4.jpg" width="250" alt="Chat code sample demo image">
<img src="https://developers.connectycube.com/docs/_images/code_samples/android_codesample_chat_java_demo3.jpg" width="250" alt="Chat code sample demo image">
</p>

## Setup

1. Register new account and application at `https://admin.connectycube.com` then put Application credentials from 'Overview' page + Account key to the `App` class.

2. At `https://admin.connectycube.com`, create from 2 to 10 users in 'Users' module and put them into `user_config.json` file at `assets`, in the following format:

```
{
  "user_1_login": "user_1_password",
  "user_2_login": "user_2_password",
  "user_3_login": "user_3_password"
}
```

3. (Optional) If you are at [Enterprise plan](https://connectycube.com/pricing/) - provide your API server and Chat server endpoints at  `App` class to point the sample against your own server.

4. For offline pushes setup server key [FCM](https://developers.connectycube.com/android/push-notifications?id=configure-firebase-project-and-api-key), define **sender_id** (your sender id from google console) in string resource and put your **google-services.json** to module package, also uncomment *apply plugin: 'com.google.gms.google-services'* line in chat module **build.gradle** file. For more information look at <https://developers.connectycube.com/android/push-notifications>
