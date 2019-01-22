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
package com.codingapi.txlcn.tm.core.transaction;

import com.codingapi.txlcn.commons.exception.TxManagerException;
import com.codingapi.txlcn.tm.config.TxManagerConfig;
import com.codingapi.txlcn.tm.core.message.RpcExecuteService;
import com.codingapi.txlcn.tm.core.message.TransactionCmd;
import com.codingapi.txlcn.spi.message.RpcClient;
import com.codingapi.txlcn.spi.message.params.InitClientParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * Description:
 * Company: CodingApi
 * Date: 2018/12/29
 *
 * @author codingapi
 */
@Service(value = "rpc_init-client")
@Slf4j
public class InitClientService implements RpcExecuteService {



    @Autowired
    private RpcClient rpcClient;

    @Autowired
    private TxManagerConfig txManagerConfig;


    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxManagerException {
        log.info("init client - >{}",transactionCmd);
        InitClientParams initClientParams = transactionCmd.getMsg().loadBean(InitClientParams.class);
        rpcClient.bindAppName(transactionCmd.getRemoteKey(),initClientParams.getAppName());
        initClientParams.setDtxTime(txManagerConfig.getDtxTime());
        return initClientParams;
    }
}