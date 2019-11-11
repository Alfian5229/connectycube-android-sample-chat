package com.connectycube.sample.chat.utils.holder;

import android.util.SparseArray;

import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Basically in your app you should store users in database
 * And load users to memory on demand
 * We're using runtime SpaceArray holder just to simplify app logic
 */
public class UsersHolder {

    private static UsersHolder instance;

    private SparseArray<ConnectycubeUser> userSparseArray;

    public static synchronized UsersHolder getInstance() {
        if (instance == null) {
            instance = new UsersHolder();
        }

        return instance;
    }

    private UsersHolder() {
        userSparseArray = new SparseArray<>();
    }

    public void putUsers(List<ConnectycubeUser> users) {
        for (ConnectycubeUser user : users) {
            putUser(user);
        }
    }

    public void putUser(ConnectycubeUser user) {
        userSparseArray.put(user.getId(), user);
    }

    public ConnectycubeUser getUserById(int id) {
        return userSparseArray.get(id);
    }

    public List<ConnectycubeUser> getUsersByIds(List<Integer> ids) {
        List<ConnectycubeUser> users = new ArrayList<>();
        for (Integer id : ids) {
            ConnectycubeUser user = getUserById(id);
            if (user != null) {
                users.add(user);
            }
        }

        return users;
    }

}
