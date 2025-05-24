## Gotify Notify

![video](https://github.com/user-attachments/assets/149207b9-d332-42cb-93c9-e15cc9bff58a)

It's a simple app that receives notifications at your mobile device from [Gotify](https://gotify.net) server.
Gotify repo has its own implementation, but it's just a wrapper around WebSocket protocol.
What is shown here is the minimal example of how to receive notifications from a WebSocket server on Android side.

It consits of two parts which is a server and a client. I put docker files to run gotify which you can do from docker hub directly but for me it's more convenient to run it locally this way despite using mixed technology stack in one repo considered a bad practice.

## Gotify server setup

1. open `http://127.0.0.1:8080`
2. login with `admin` and `admin`
3. create a new application (user client is already bond to the device you logged in which is admin). Copy generated its application token (`AIM-Ptoh.ZfceYW` here) and client token (`Cl-YBBhXZH5iQn1` here).

4. send a notification from Terminal:

```sh

curl -X POST "http://127.0.0.1:8080/message?token=AIM-Ptoh.ZfceYW" \
     -F "title=Hello你好👋" \
     -F "message=This is a Gotify test👁️" \
     -F "priority=5"
```

## How to run and test the Android client

1. Make sure to setup the correct Gotify server endpoint with client token:

```ini
gotify.wss.url=ws://192.168.1.65:8080/stream?token=Cl-YBBhXZH5iQn1
```

2. Build and run the app. Make sure it has been provided with the permissions requested.

3. Open the app, press "Test notification" and wait 5 seconds if it's received.

4. Send from user's token side a message to gotify server:

```shell
curl -X POST "http://192.168.1.65:8080/message?token=AIM-Ptoh.ZfceYW" \
     -F "title=Gotify Notify test👁️" \
     -F "message=The Work Is Mysterious And Important!👋" \
     -F "priority=5"
```

5. You should receive a notification at your mobile client endpoint.

6. Check Logcat if something goes wrong.
