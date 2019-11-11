package com.connectycube.sample.chat.utils.holder;

import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeChatMessage;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DialogHolder {

    private static DialogHolder instance;
    private Map<String, ConnectycubeChatDialog> dialogsMap;

    public static synchronized DialogHolder getInstance() {
        if (instance == null) {
            instance = new DialogHolder();
        }
        return instance;
    }

    private DialogHolder() {
        dialogsMap = new TreeMap<>();
    }

    public Map<String, ConnectycubeChatDialog> getDialogs() {
        return getSortedMap(dialogsMap);
    }

    public ConnectycubeChatDialog getChatDialogById(String dialogId) {
        return dialogsMap.get(dialogId);
    }

    public void clear() {
        dialogsMap.clear();
    }

    public void addDialog(ConnectycubeChatDialog dialog) {
        if (dialog != null) {
            dialogsMap.put(dialog.getDialogId(), dialog);
        }
    }

    public void addDialogs(List<ConnectycubeChatDialog> dialogs) {
        for (ConnectycubeChatDialog dialog : dialogs) {
            addDialog(dialog);
        }
    }

    public void deleteDialogs(Collection<ConnectycubeChatDialog> dialogs) {
        for (ConnectycubeChatDialog dialog : dialogs) {
            deleteDialog(dialog);
        }
    }

    public void deleteDialogs(ArrayList<String> dialogsIds) {
        for (String dialogId : dialogsIds) {
            deleteDialog(dialogId);
        }
    }

    public void deleteDialog(ConnectycubeChatDialog chatDialog) {
        dialogsMap.remove(chatDialog.getDialogId());
    }

    public void deleteDialog(String dialogId) {
        dialogsMap.remove(dialogId);
    }

    public boolean hasDialogWithId(String dialogId) {
        return dialogsMap.containsKey(dialogId);
    }

    public boolean hasPrivateDialogWithUser(ConnectycubeUser user) {
        return getPrivateDialogWithUser(user) != null;
    }

    public ConnectycubeChatDialog getPrivateDialogWithUser(ConnectycubeUser user) {
        for (ConnectycubeChatDialog chatDialog : dialogsMap.values()) {
            if (ConnectycubeDialogType.PRIVATE.equals(chatDialog.getType())
                    && chatDialog.getOccupants().contains(user.getId())) {
                return chatDialog;
            }
        }

        return null;
    }

    private Map<String, ConnectycubeChatDialog> getSortedMap(Map<String, ConnectycubeChatDialog> unsortedMap) {
        Map<String, ConnectycubeChatDialog> sortedMap = new TreeMap<>(new LastMessageDateSentComparator(unsortedMap));
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

    public void updateDialog(String dialogId, ConnectycubeChatMessage chatMessage) {
        ConnectycubeChatDialog updatedDialog = getChatDialogById(dialogId);
        updatedDialog.setLastMessage(chatMessage.getBody());
        updatedDialog.setLastMessageDateSent(chatMessage.getDateSent());
        updatedDialog.setUnreadMessageCount(updatedDialog.getUnreadMessageCount() != null
                ? updatedDialog.getUnreadMessageCount() + 1 : 1);
        updatedDialog.setLastMessageUserId(chatMessage.getSenderId());

        dialogsMap.put(updatedDialog.getDialogId(), updatedDialog);
    }

    public void updateDialog(ConnectycubeChatDialog dialog) {
        ConnectycubeChatDialog updatedDialog = getChatDialogById(dialog.getDialogId());
        updatedDialog.setOccupantsIds(dialog.getOccupants());
        updatedDialog.setName(dialog.getName());
        dialogsMap.put(updatedDialog.getDialogId(), updatedDialog);
    }

    static class LastMessageDateSentComparator implements Comparator<String> {
        Map<String, ConnectycubeChatDialog> map;

        LastMessageDateSentComparator(Map<String, ConnectycubeChatDialog> map) {

            this.map = map;
        }

        public int compare(String keyA, String keyB) {
            ConnectycubeChatDialog dialogA = map.get(keyA);
            ConnectycubeChatDialog dialogB = map.get(keyB);

            long valueA = dialogA.getLastMessageDateSent() == 0 ? (dialogA.getCreatedAt() != null ?
                    dialogA.getCreatedAt().getTime() / 1000 : System.currentTimeMillis()) : dialogA.getLastMessageDateSent();
            long valueB = dialogB.getLastMessageDateSent() == 0 ? (dialogB.getCreatedAt() != null ?
                    dialogB.getCreatedAt().getTime() / 1000 : System.currentTimeMillis()) : dialogB.getLastMessageDateSent();

            return (valueB < valueA) ? -1 : 1;
        }
    }
}
