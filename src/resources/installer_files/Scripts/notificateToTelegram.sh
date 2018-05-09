#!/bin/bash

KEY="$1"

URL="https://api.telegram.org/bot$KEY/sendMessage"
# URL="https://api.telegram.org/bot549785187:AAHt6qfL8-DEy-qHQLgaDhw0cHUFd6rNWTg/getUpdate"

TARGET="$2" # Telegram ID of the conversation with the bot, get it from /getUpdates API

TEXT="Hi!, review the server: $3"

PAYLOAD="chat_id=$TARGET&text=$TEXT&parse_mode=Markdown&disable_web_page_preview=true"

# Run in background so the script could return immediately without blocking PAM
curl -s --max-time 10 --retry 5 --retry-delay 2 --retry-max-time 10 -d "$PAYLOAD" $URL > /dev/null 2>&1 &
