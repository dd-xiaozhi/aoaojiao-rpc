package com.aoaojiao.rpc.example.provider;

import com.aoaojiao.rpc.example.api.User;
import com.aoaojiao.rpc.example.api.UserService;

public class UserServiceImpl implements UserService {
    @Override
    public User getUser(long id) {
        return new User(id, "user-" + id);
    }
}
