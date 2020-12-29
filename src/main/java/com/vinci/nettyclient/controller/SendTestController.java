package com.vinci.nettyclient.controller;

import com.vinci.nettyclient.client.NettyClient;
import com.vinci.nettyclient.client.entity.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/echo/")
public class SendTestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendTestController.class);

    @Autowired
    private NettyClient client;

    @GetMapping(value = "signOn")
    public void signOn() {
        try {
            RemotingCommand remotingCommand = new RemotingCommand();
            remotingCommand.setId(1);
            remotingCommand.setRemark(String.format("登陆请求:%d", 11111111));
            RemotingCommand response = client.invokeSync0(remotingCommand, 30000);
            LOGGER.info(response.toString());
        }catch (Exception e){
            LOGGER.error("request is failed by netty client", e);
        }
    }

    @GetMapping(value = "key")
    public void key() {
        try {
            RemotingCommand remotingCommand = new RemotingCommand();
            remotingCommand.setId(2);
            remotingCommand.setRemark(String.format("key请求:%d", 22222222));
            RemotingCommand response = client.invokeSync0(remotingCommand, 30000);
            LOGGER.info(response.toString());
        }catch (Exception e){
            LOGGER.error("request is failed by netty client", e);
        }
    }

    @GetMapping(value = "transaction")
    public void transaction() {
        try {
            RemotingCommand remotingCommand = new RemotingCommand();
            remotingCommand.setId(3);
            remotingCommand.setRemark(String.format("transaction请求:%d", 33333333));
            RemotingCommand response = client.invokeSync0(remotingCommand, 30000);
            LOGGER.info(response.toString());
        }catch (Exception e){
            LOGGER.error("request is failed by netty client", e);
        }
    }
}
