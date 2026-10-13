package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

public interface UserService {

    /**
     * Authenticates a WeChat mini-program user and creates a local account when needed
     *
     * @param userLoginDTO Mini-program login code
     * @return Authenticated user
     */
    User wxLogin(UserLoginDTO userLoginDTO);
    
}
