package com.aoaojiao.rpc.example.boot.provider;

import com.aoaojiao.rpc.example.api.User;
import com.aoaojiao.rpc.example.api.UserService;
import com.aoaojiao.rpc.spring.annotation.RpcService;

@RpcService(version = "v1", group = "default")
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(long id) {
        return new User(id, "user-" + id);
    }
}
