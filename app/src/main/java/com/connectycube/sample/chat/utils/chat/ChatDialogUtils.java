
package com.connectycube.sample.chat.utils.chat;

import android.text.TextUtils;
import android.util.Log;

import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.chat.model.ConnectycubeDialogType;
import com.connectycube.chat.utils.DialogUtils;
import com.connectycube.sample.chat.utils.holder.UsersHolder;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChatDialogUtils {
    private static final String TAG = ChatDialogUtils.class.getSimpleName();


    public static ConnectycubeChatDialog createDialog(List<ConnectycubeUser> users) {
        if (isPrivateChat(users)) {
            ConnectycubeUser currentUser = ChatHelper.getCurrentUser();
            users.remove(currentUser);
        }
        return DialogUtils.buildDialog(users.toArray(new ConnectycubeUser[users.size()]));
    }

    private static boolean isPrivateChat(List<ConnectycubeUser> users) {
        return users.size() == 2;
    }

    public static List<ConnectycubeUser> getAddedUsers(ConnectycubeChatDialog dialog, List<ConnectycubeUser> currentUsers) {
        return getAddedUsers(getUsersFromDialog(dialog), currentUsers);
    }

    public static List<ConnectycubeUser> getAddedUsers(List<ConnectycubeUser> previousUsers, List<ConnectycubeUser> currentUsers) {
        List<ConnectycubeUser> addedUsers = new ArrayList<>();
        for (ConnectycubeUser currentUser : currentUsers) {
            boolean wasInChatBefore = false;
            for (ConnectycubeUser previousUser : previousUsers) {
                if (currentUser.getId().equals(previousUser.getId())) {
                    wasInChatBefore = true;
                    break;
                }
            }
            if (!wasInChatBefore) {
                addedUsers.add(currentUser);
            }
        }

        ConnectycubeUser currentUser = ChatHelper.getCurrentUser();
        addedUsers.remove(currentUser);

        return addedUsers;
    }

    public static List<ConnectycubeUser> getRemovedUsers(ConnectycubeChatDialog dialog, List<ConnectycubeUser> currentUsers) {
        return getRemovedUsers(getUsersFromDialog(dialog), currentUsers);
    }

    public static List<ConnectycubeUser> getRemovedUsers(List<ConnectycubeUser> previousUsers, List<ConnectycubeUser> currentUsers) {
        List<ConnectycubeUser> removedUsers = new ArrayList<>();
        for (ConnectycubeUser previousUser : previousUsers) {
            boolean isUserStillPresented = false;
            for (ConnectycubeUser currentUser : currentUsers) {
                if (previousUser.getId().equals(currentUser.getId())) {
                    isUserStillPresented = true;
                    break;
                }
            }
            if (!isUserStillPresented) {
                removedUsers.add(previousUser);
            }
        }

        ConnectycubeUser currentUser = ChatHelper.getCurrentUser();
        removedUsers.remove(currentUser);

        return removedUsers;
    }

    public static void logDialogUsers(ConnectycubeChatDialog dialog) {
        Log.v(TAG, "Dialog " + getDialogName(dialog));
        logUsersByIds(dialog.getOccupants());
    }

    public static void logUsers(List<ConnectycubeUser> users) {
        for (ConnectycubeUser user : users) {
            Log.i(TAG, user.getId() + " " + user.getLogin());
        }
    }

    private static void logUsersByIds(List<Integer> users) {
        for (Integer id : users) {
            ConnectycubeUser user = UsersHolder.getInstance().getUserById(id);
            Log.i(TAG, user.getId() + " " + user.getLogin());
        }
    }

    public static Integer[] getUserIds(List<ConnectycubeUser> users) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (ConnectycubeUser user : users) {
            ids.add(user.getId());
        }
        return ids.toArray(new Integer[ids.size()]);
    }

    public static String getDialogName(ConnectycubeChatDialog dialog) {
        if (dialog.getType().equals(ConnectycubeDialogType.GROUP)) {
            return dialog.getName();
        } else {
            // It's a private dialog, let's use opponent's name as chat name
            Integer opponentId = dialog.getRecipientId();
            ConnectycubeUser user = UsersHolder.getInstance().getUserById(opponentId);
            if (user != null) {
                return TextUtils.isEmpty(user.getFullName()) ? user.getLogin() : user.getFullName();
            } else {
                return dialog.getName();
            }
        }
    }

    private static List<ConnectycubeUser> getUsersFromDialog(ConnectycubeChatDialog dialog) {
        List<ConnectycubeUser> previousDialogUsers = new ArrayList<>();
        for (Integer id : dialog.getOccupants()) {
            ConnectycubeUser user = UsersHolder.getInstance().getUserById(id);
            if (user == null) {
                throw new RuntimeException("User from dialog is not in memory. This should never happen, or we are screwed");
            }
            previousDialogUsers.add(user);
        }
        return previousDialogUsers;
    }

    public static List<Integer> getOccupantsIdsListFromString(String occupantIds) {
        List<Integer> occupantIdsList = new ArrayList<>();
        String[] occupantIdsArray = occupantIds.split(",");
        for (String occupantId : occupantIdsArray) {
            occupantIdsList.add(Integer.valueOf(occupantId));
        }
        return occupantIdsList;
    }

    public static String getOccupantsIdsStringFromList(Collection<Integer> occupantIdsList) {
        return TextUtils.join(",", occupantIdsList);
    }

    public static ConnectycubeChatDialog buildPrivateChatDialog(String dialogId, Integer recipientId) {
        ConnectycubeChatDialog chatDialog = DialogUtils.buildPrivateDialog(recipientId);
        chatDialog.setDialogId(dialogId);

        return chatDialog;
    }
}
