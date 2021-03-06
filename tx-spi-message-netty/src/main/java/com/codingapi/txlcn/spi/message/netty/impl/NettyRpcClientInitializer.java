/*
 * Copyright 2017-2019 CodingApi .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingapi.txlcn.spi.message.netty.impl;

import com.codingapi.txlcn.spi.message.RpcClientInitializer;
import com.codingapi.txlcn.spi.message.RpcConfig;
import com.codingapi.txlcn.spi.message.dto.TxManagerHost;
import com.codingapi.txlcn.spi.message.netty.bean.SocketManager;
import com.codingapi.txlcn.spi.message.netty.em.NettyType;
import com.codingapi.txlcn.spi.message.netty.handler.NettyRpcClientHandlerInitHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Description:
 * Company: CodingApi
 * Date: 2018/12/10
 *
 * @author ujued
 */
@Service
@Slf4j
public class NettyRpcClientInitializer implements RpcClientInitializer, DisposableBean {

    @Autowired
    private NettyRpcClientHandlerInitHandler nettyRpcClientHandlerInitHandler;

    @Autowired
    private RpcConfig rpcConfig;

    private EventLoopGroup workerGroup;

    private CountDownLatch countDownLatch;

    @Override
    public void init(List<TxManagerHost> hosts) {
        NettyContext.type = NettyType.client;
        NettyContext.params = hosts;
        workerGroup = new NioEventLoopGroup();
        this.countDownLatch = new CountDownLatch(hosts.size());
        for (TxManagerHost host : hosts) {
            connect(new InetSocketAddress(host.getHost(), host.getPort()));
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        log.info("TM cluster size:{}", SocketManager.getInstance().currentSize());
    }


    @Override
    public synchronized void connect(SocketAddress socketAddress) {
        boolean connected = false;
        for (int i = 0; i < rpcConfig.getReconnectCount(); i++) {
            if (SocketManager.getInstance().noConnect(socketAddress)) {
                try {
                    log.info("Connect TM[{}] - count {}", socketAddress, i + 1);
                    Bootstrap b = new Bootstrap();
                    b.group(workerGroup);
                    b.channel(NioSocketChannel.class);
                    b.option(ChannelOption.SO_KEEPALIVE, true);
                    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
                    b.handler(nettyRpcClientHandlerInitHandler);
                    ChannelFuture channelFuture = b.connect(socketAddress).syncUninterruptibly();
                    channelFuture.addListener(future -> countDownLatch.countDown());
                    log.info("TC connect state:{}", socketAddress, channelFuture.isSuccess());
                    connected = true;
                    break;

                } catch (Exception e) {
                    countDownLatch.countDown();
                    log.warn("Connect TM[{}] fail. {}ms latter try again.", socketAddress, rpcConfig.getReconnectDelay());
                    try {
                        Thread.sleep(rpcConfig.getReconnectDelay());
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        if (!connected) {
            log.warn("Finally, netty connection fail , address is {}", socketAddress);
            if (SocketManager.getInstance().currentSize() == 0) {
                throw new IllegalStateException("Can not connect any TM, DTX disabled.");
            }
        }
    }


    @Override
    public void destroy() throws Exception {
        workerGroup.shutdownGracefully();
        log.info("TC was down.");
    }
}
