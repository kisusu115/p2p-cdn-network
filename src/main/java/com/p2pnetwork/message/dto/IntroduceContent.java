package com.p2pnetwork.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IntroduceContent {
    private String ip;
    private int port;

    public static IntroduceContent of(String ip, int port){
        return new IntroduceContent(ip, port);
    }
}