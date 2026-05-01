package com.agricultural.trace.vo;

import lombok.Data;

@Data
public class LoginVO {
    
    private String token;
    
    private UserInfoVO userInfo;
}
