package com.botter.shop.order.access;


import com.botter.shop.order.vo.User;

public class UserContext {

    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void setUser(User user) {
        userHolder.set(user);
    }

    public static User getUser() {
        return userHolder.get();
    }

    public static void remove() {
        userHolder.remove();
    }

}
