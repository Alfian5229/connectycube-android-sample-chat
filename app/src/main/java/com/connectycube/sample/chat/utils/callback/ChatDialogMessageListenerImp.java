package com.connectycube.sample.chat.utils.callback;

import com.connectycube.chat.exception.ChatException;
import com.connectycube.chat.listeners.ChatDialogMessageListener;
import com.connectycube.chat.model.ConnectycubeChatMessage;

public class ChatDialogMessageListenerImp implements ChatDialogMessageListener{
    public ChatDialogMessageListenerImp() {
    }

    @Override
    public void processMessage(String s, ConnectycubeChatMessage chatMessage, Integer integer) {

    }

    @Override
    public void processError(String s, ChatException e, ConnectycubeChatMessage chatMessage, Integer integer) {

    }
}
