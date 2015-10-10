1、topic 长度 Byte.MAX_VALUE
2、message property长度 Short.MAX_VALUE
3、消息标记按topicconfig设置的readqueuenums和writequeuenums进行随机或是hash一致存储

MQClientAPIImpl.pullMessage 客户端拉取消息 command code RequestCode.PULL_MESSAGE

BrokerController.registerProcessor 注册broker的各种处理器
 endMessageProcessor sendProcessor = new SendMessageProcessor(this);
this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE, sendProcessor,this.sendMessageExecutor);
this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE_V2, sendProcessor,this.sendMessageExecutor);
this.remotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, sendProcessor,this.sendMessageExecutor);

/**
* PullMessageProcessor
*/
this.remotingServer.registerProcessor(RequestCode.PULL_MESSAGE, this.pullMessageProcessor,this.pullMessageExecutor);
this.pullMessageProcessor.registerConsumeMessageHook(consumeMessageHookList);

/**
* QueryMessageProcessor
*/
NettyRequestProcessor queryProcessor = new QueryMessageProcessor(this);
this.remotingServer.registerProcessor(RequestCode.QUERY_MESSAGE, queryProcessor,this.pullMessageExecutor);
this.remotingServer.registerProcessor(RequestCode.VIEW_MESSAGE_BY_ID, queryProcessor,this.pullMessageExecutor);

/**
* ClientManageProcessor
*/
ClientManageProcessor clientProcessor = new ClientManageProcessor(this);
clientProcessor.registerConsumeMessageHook(this.consumeMessageHookList);
this.remotingServer.registerProcessor(RequestCode.HEART_BEAT, clientProcessor,this.clientManageExecutor);
this.remotingServer.registerProcessor(RequestCode.UNREGISTER_CLIENT, clientProcessor,this.clientManageExecutor);
this.remotingServer.registerProcessor(RequestCode.GET_CONSUMER_LIST_BY_GROUP, clientProcessor,this.clientManageExecutor);

/**
* Offset存储更新转移到ClientProcessor处理
*/
this.remotingServer.registerProcessor(RequestCode.UPDATE_CONSUMER_OFFSET, clientProcessor,this.clientManageExecutor);
this.remotingServer.registerProcessor(RequestCode.QUERY_CONSUMER_OFFSET, clientProcessor,this.clientManageExecutor);

/**
* EndTransactionProcessor
*/
this.remotingServer.registerProcessor(RequestCode.END_TRANSACTION, new EndTransactionProcessor(this),this.sendMessageExecutor);

/**
* Default
*/
AdminBrokerProcessor adminProcessor = new AdminBrokerProcessor(this);
this.remotingServer.registerDefaultProcessor(adminProcessor, this.adminBrokerExecutor);

## nameserver 
 @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
            throws RemotingCommandException {
        if (log.isDebugEnabled()) {
            log.debug("receive request, {} {} {}",//
                request.getCode(), //
                RemotingHelper.parseChannelRemoteAddr(ctx.channel()), //
                request);
        }

        switch (request.getCode()) {
            case RequestCode.PUT_KV_CONFIG:
                return this.putKVConfig(ctx, request);
            case RequestCode.GET_KV_CONFIG:
                return this.getKVConfig(ctx, request);
            case RequestCode.DELETE_KV_CONFIG:
                return this.deleteKVConfig(ctx, request);
            case RequestCode.REGISTER_BROKER:
                Version brokerVersion = MQVersion.value2Version(request.getVersion());
                // 新版本Broker，支持Filter Server
                if (brokerVersion.ordinal() >= MQVersion.Version.V3_0_11.ordinal()) {
                    return this.registerBrokerWithFilterServer(ctx, request);
                }
                // 低版本Broker，不支持Filter Server
                else {
                    return this.registerBroker(ctx, request);
                }
            case RequestCode.UNREGISTER_BROKER:
                return this.unregisterBroker(ctx, request);
            case RequestCode.GET_ROUTEINTO_BY_TOPIC:
                return this.getRouteInfoByTopic(ctx, request);
            case RequestCode.GET_BROKER_CLUSTER_INFO:
                return this.getBrokerClusterInfo(ctx, request);
            case RequestCode.WIPE_WRITE_PERM_OF_BROKER:
                return this.wipeWritePermOfBroker(ctx, request);
            case RequestCode.GET_ALL_TOPIC_LIST_FROM_NAMESERVER:
                return getAllTopicListFromNameserver(ctx, request);
            case RequestCode.DELETE_TOPIC_IN_NAMESRV:
                return deleteTopicInNamesrv(ctx, request);
            case RequestCode.GET_KV_CONFIG_BY_VALUE:
                return getKVConfigByValue(ctx, request);
            case RequestCode.DELETE_KV_CONFIG_BY_VALUE:
                return deleteKVConfigByValue(ctx, request);
            case RequestCode.GET_KVLIST_BY_NAMESPACE:
                return this.getKVListByNamespace(ctx, request);
            case RequestCode.GET_TOPICS_BY_CLUSTER:
                return this.getTopicsByCluster(ctx, request);
            case RequestCode.GET_SYSTEM_TOPIC_LIST_FROM_NS:
                return this.getSystemTopicListFromNs(ctx, request);
            case RequestCode.GET_UNIT_TOPIC_LIST:
                return this.getUnitTopicList(ctx, request);
            case RequestCode.GET_HAS_UNIT_SUB_TOPIC_LIST:
                return this.getHasUnitSubTopicList(ctx, request);
            case RequestCode.GET_HAS_UNIT_SUB_UNUNIT_TOPIC_LIST:
                return this.getHasUnitSubUnUnitTopicList(ctx, request);
            default:
                break;
            }
        return null;
    }

## 环境变量
ROCKETMQ_HOME=E:\\git\\RocketMQ

## 确认文件位置
int index =(int) ((offset / this.mapedFileSize) - (mapedFile.getFileFromOffset() / this.mapedFileSize));
## 确认文件的起始读行
mapedFile.selectMapedBuffer((int) (offset % mapedFileSize));

获取消息position
ConsumerQueue.getIndexBuffer(final long startIndex);
追加消息
mapedFile.appendMessage

写入文件
 public AppendMessageResult doAppend(final long fileFromOffset, final ByteBuffer byteBuffer,
                final int maxBlank, final Object msg) {
            // STORETIMESTAMP + STOREHOSTADDRESS + OFFSET <br>
            MessageExtBrokerInner msgInner = (MessageExtBrokerInner) msg;
            // PHY OFFSET
            long wroteOffset = fileFromOffset + byteBuffer.position();
            String msgId = MessageDecoder.createMessageId(this.msgIdMemory, msgInner.getStoreHostBytes(),
                        wroteOffset);

            // Record ConsumeQueue information
            String key = msgInner.getTopic() + "-" + msgInner.getQueueId();
            Long queueOffset = CommitLog.this.topicQueueTable.get(key);
            if (null == queueOffset) {
                queueOffset = 0L;
                CommitLog.this.topicQueueTable.put(key, queueOffset);
            }

            // Transaction messages that require special handling
            final int tranType = MessageSysFlag.getTransactionValue(msgInner.getSysFlag());
            switch (tranType) {
            // Prepared and Rollback message is not consumed, will not enter the
            // consumer queue
            case MessageSysFlag.TransactionPreparedType:
            case MessageSysFlag.TransactionRollbackType:
                queueOffset = 0L;
                break;
            case MessageSysFlag.TransactionNotType:
            case MessageSysFlag.TransactionCommitType:
            default:
                break;
            }

            /**
             * Serialize message
             */
            final byte[] propertiesData =
                    msgInner.getPropertiesString() == null ? null : msgInner.getPropertiesString().getBytes();
            final int propertiesLength = propertiesData == null ? 0 : propertiesData.length;

            final byte[] topicData = msgInner.getTopic().getBytes();
            final int topicLength = topicData == null ? 0 : topicData.length;

            final int bodyLength = msgInner.getBody() == null ? 0 : msgInner.getBody().length;

            final int msgLen = 4 // 1 TOTALSIZE
                    + 4 // 2 MAGICCODE
                    + 4 // 3 BODYCRC
                    + 4 // 4 QUEUEID
                    + 4 // 5 FLAG
                    + 8 // 6 QUEUEOFFSET
                    + 8 // 7 PHYSICALOFFSET
                    + 4 // 8 SYSFLAG
                    + 8 // 9 BORNTIMESTAMP
                    + 8 // 10 BORNHOST
                    + 8 // 11 STORETIMESTAMP
                    + 8 // 12 STOREHOSTADDRESS
                    + 4 // 13 RECONSUMETIMES
                    + 8 // 14 Prepared Transaction Offset
                    + 4 + bodyLength // 14 BODY
                    + 1 + topicLength // 15 TOPIC
                    + 2 + propertiesLength // 16 propertiesLength
                    + 0;

            // Exceeds the maximum message
            if (msgLen > this.maxMessageSize) {
                CommitLog.log.warn("message size exceeded, msg total size: " + msgLen + ", msg body size: "
                        + bodyLength + ", maxMessageSize: " + this.maxMessageSize);
                return new AppendMessageResult(AppendMessageStatus.MESSAGE_SIZE_EXCEEDED);
            }

            // Determines whether there is sufficient free space
            if ((msgLen + END_FILE_MIN_BLANK_LENGTH) > maxBlank) {
                this.resetMsgStoreItemMemory(maxBlank);
                // 1 TOTALSIZE
                this.msgStoreItemMemory.putInt(maxBlank);
                // 2 MAGICCODE
                this.msgStoreItemMemory.putInt(CommitLog.BlankMagicCode);
                // 3 The remaining space may be any value
                //

                // Here the length of the specially set maxBlank
                byteBuffer.put(this.msgStoreItemMemory.array(), 0, maxBlank);
                return new AppendMessageResult(AppendMessageStatus.END_OF_FILE, wroteOffset, maxBlank, msgId,
                    msgInner.getStoreTimestamp(), queueOffset);
            }

            // Initialization of storage space
            this.resetMsgStoreItemMemory(msgLen);
            // 1 TOTALSIZE
            this.msgStoreItemMemory.putInt(msgLen);
            // 2 MAGICCODE
            this.msgStoreItemMemory.putInt(CommitLog.MessageMagicCode);
            // 3 BODYCRC
            this.msgStoreItemMemory.putInt(msgInner.getBodyCRC());
            // 4 QUEUEID
            this.msgStoreItemMemory.putInt(msgInner.getQueueId());
            // 5 FLAG
            this.msgStoreItemMemory.putInt(msgInner.getFlag());
            // 6 QUEUEOFFSET
            this.msgStoreItemMemory.putLong(queueOffset);
            // 7 PHYSICALOFFSET
            this.msgStoreItemMemory.putLong(fileFromOffset + byteBuffer.position());
            // 8 SYSFLAG
            this.msgStoreItemMemory.putInt(msgInner.getSysFlag());
            // 9 BORNTIMESTAMP
            this.msgStoreItemMemory.putLong(msgInner.getBornTimestamp());
            // 10 BORNHOST
            this.msgStoreItemMemory.put(msgInner.getBornHostBytes());
            // 11 STORETIMESTAMP
            this.msgStoreItemMemory.putLong(msgInner.getStoreTimestamp());
            // 12 STOREHOSTADDRESS
            this.msgStoreItemMemory.put(msgInner.getStoreHostBytes());
            // 13 RECONSUMETIMES
            this.msgStoreItemMemory.putInt(msgInner.getReconsumeTimes());
            // 14 Prepared Transaction Offset
            this.msgStoreItemMemory.putLong(msgInner.getPreparedTransactionOffset());
            // 15 BODY
            this.msgStoreItemMemory.putInt(bodyLength);
            if (bodyLength > 0)
                this.msgStoreItemMemory.put(msgInner.getBody());
            // 16 TOPIC
            this.msgStoreItemMemory.put((byte) topicLength);
            this.msgStoreItemMemory.put(topicData);
            // 17 PROPERTIES
            this.msgStoreItemMemory.putShort((short) propertiesLength);
            if (propertiesLength > 0)
                this.msgStoreItemMemory.put(propertiesData);

            // Write messages to the queue buffer
            byteBuffer.put(this.msgStoreItemMemory.array(), 0, msgLen);

            // long wroteOffset = fileFromOffset + byteBuffer.position();
            AppendMessageResult result =
                    new AppendMessageResult(AppendMessageStatus.PUT_OK, wroteOffset, msgLen, msgId,
                        msgInner.getStoreTimestamp(), queueOffset);

            switch (tranType) {
            case MessageSysFlag.TransactionPreparedType:
            case MessageSysFlag.TransactionRollbackType:
                break;
            case MessageSysFlag.TransactionNotType:
            case MessageSysFlag.TransactionCommitType:
                // The next update ConsumeQueue information
                CommitLog.this.topicQueueTable.put(key, ++queueOffset);
                break;
            default:
                break;
            }

            return result;
##  消息队列索引
this.byteBufferIndex.limit(CQStoreUnitSize);
this.byteBufferIndex.putLong(offset);
this.byteBufferIndex.putInt(size);
this.byteBufferIndex.putLong(tagsCode);

#计算下一个偏移量   
public long computePullFromWhere(MessageQueue mq) {
        long result = -1;
        final ConsumeFromWhere consumeFromWhere = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getConsumeFromWhere();
        final OffsetStore offsetStore = this.defaultMQPushConsumerImpl.getOffsetStore();
        switch (consumeFromWhere) {
            case CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST:
            case CONSUME_FROM_MIN_OFFSET:
            case CONSUME_FROM_MAX_OFFSET:
            case CONSUME_FROM_LAST_OFFSET: {
                long lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0) {
                    result = lastOffset;
                }
                // First start,no offset
                else if (-1 == lastOffset) {
                    if (mq.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                        result = 0L;
                    }
                    else {
                        try {
                            result = this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
                        }
                        catch (MQClientException e) {
                            result = -1;
                        }
                    }
                }
                else {
                    result = -1;
                }
                break;
            }
            case CONSUME_FROM_FIRST_OFFSET: {
                long lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0) {
                    result = lastOffset;
                }
                else if (-1 == lastOffset) {
                    result = 0L;
                }
                else {
                    result = -1;
                }
                break;
            }
            case CONSUME_FROM_TIMESTAMP: {
                long lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0) {
                    result = lastOffset;
                }
                else if (-1 == lastOffset) {
                    if (mq.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                        try {
                            result = this.mQClientFactory.getMQAdminImpl().maxOffset(mq);
                        }
                        catch (MQClientException e) {
                            result = -1;
                        }
                    }
                    else {
                        try {
                            long timestamp = UtilAll.parseDate(this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer().getConsumeTimestamp(), UtilAll.yyyyMMddHHmmss).getTime();
                            result = this.mQClientFactory.getMQAdminImpl().searchOffset(mq, timestamp);
                        }
                        catch (MQClientException e) {
                            result = -1;
                        }
                    }
                }
                else {
                    result = -1;
                }
                break;
            }

            default:
                break;
            }

        return result;
    }

##DefaultMQProducerImpl
private SendResult sendDefaultImpl(//
            Message msg,//
            final CommunicationMode communicationMode,//
            final SendCallback sendCallback, final long timeout//
    ) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        this.makeSureStateOK();
        Validators.checkMessage(msg, this.defaultMQProducer);
        final long maxTimeout = this.defaultMQProducer.getSendMsgTimeout() + 1000;
        final long beginTimestamp = System.currentTimeMillis();
        long endTimestamp = beginTimestamp;
        // 获取topic配置信息
        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());

        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            MessageQueue mq = null;
            Exception exception = null;
            SendResult sendResult = null;
            int timesTotal = 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed();
            int times = 0;
            String[] brokersSent = new String[timesTotal];
            for (; times < timesTotal && (endTimestamp - beginTimestamp) < maxTimeout; times++) {
                String lastBrokerName = null == mq ? null : mq.getBrokerName();
                MessageQueue tmpmq = topicPublishInfo.selectOneMessageQueue(lastBrokerName);
                if (tmpmq != null) {
                    mq = tmpmq;
                    brokersSent[times] = mq.getBrokerName();
                    try {
                        sendResult = this.sendKernelImpl(msg, mq, communicationMode, sendCallback, timeout);
                        endTimestamp = System.currentTimeMillis();
                        switch (communicationMode) {
                        case ASYNC:
                            return null;
                        case ONEWAY:
                            return null;
                        case SYNC:
                            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                                if (this.defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()) {
                                    continue;
                                }
                            }

                            return sendResult;
                        default:
                            break;
                        }
                    }
                    catch (RemotingException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        exception = e;
                        endTimestamp = System.currentTimeMillis();
                        continue;
                    }
                    catch (MQClientException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        exception = e;
                        endTimestamp = System.currentTimeMillis();
                        continue;
                    }
                    catch (MQBrokerException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        exception = e;
                        endTimestamp = System.currentTimeMillis();
                        switch (e.getResponseCode()) {
                        case ResponseCode.TOPIC_NOT_EXIST:
                        case ResponseCode.SERVICE_NOT_AVAILABLE:
                        case ResponseCode.SYSTEM_ERROR:
                        case ResponseCode.NO_PERMISSION:
                        case ResponseCode.NO_BUYER_ID:
                        case ResponseCode.NOT_IN_CURRENT_UNIT:
                            continue;
                        default:
                            if (sendResult != null) {
                                return sendResult;
                            }

                            throw e;
                        }
                    }
                    catch (InterruptedException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        throw e;
                    }
                }
                else {
                    break;
                }
            } // end of for

            if (sendResult != null) {
                return sendResult;
            }

          
    }

## 写入消息偏移量(负载均衡，多台broker构造成messagequeue(broker,queueid,topic)列表，循环存储)
## MessageQueue tmpmq = topicPublishInfo.selectOneMessageQueue(lastBrokerName); 选取broker和queueid进行写入
private SendResult sendDefaultImpl(//
            Message msg,//
            final CommunicationMode communicationMode,//
            final SendCallback sendCallback, final long timeout//
    ) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        this.makeSureStateOK();
        Validators.checkMessage(msg, this.defaultMQProducer);

        final long maxTimeout = this.defaultMQProducer.getSendMsgTimeout() + 1000;
        final long beginTimestamp = System.currentTimeMillis();
        long endTimestamp = beginTimestamp;
        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            MessageQueue mq = null;
            Exception exception = null;
            SendResult sendResult = null;
            int timesTotal = 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed();
            int times = 0;
            String[] brokersSent = new String[timesTotal];
            for (; times < timesTotal && (endTimestamp - beginTimestamp) < maxTimeout; times++) {
                String lastBrokerName = null == mq ? null : mq.getBrokerName();
                MessageQueue tmpmq = topicPublishInfo.selectOneMessageQueue(lastBrokerName);
                if (tmpmq != null) {
                    mq = tmpmq;
                    brokersSent[times] = mq.getBrokerName();
                    try {
                        sendResult = this.sendKernelImpl(msg, mq, communicationMode, sendCallback, timeout);
                        endTimestamp = System.currentTimeMillis();
                        switch (communicationMode) {
                        case ASYNC:
                            return null;
                        case ONEWAY:
                            return null;
                        case SYNC:
                            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                                if (this.defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()) {
                                    continue;
                                }
                            }

                            return sendResult;
                        default:
                            break;
                        }
                    }
                    catch (RemotingException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        exception = e;
                        endTimestamp = System.currentTimeMillis();
                        continue;
                    }
                    catch (MQClientException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        exception = e;
                        endTimestamp = System.currentTimeMillis();
                        continue;
                    }
                    catch (MQBrokerException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        exception = e;
                        endTimestamp = System.currentTimeMillis();
                        switch (e.getResponseCode()) {
                        case ResponseCode.TOPIC_NOT_EXIST:
                        case ResponseCode.SERVICE_NOT_AVAILABLE:
                        case ResponseCode.SYSTEM_ERROR:
                        case ResponseCode.NO_PERMISSION:
                        case ResponseCode.NO_BUYER_ID:
                        case ResponseCode.NOT_IN_CURRENT_UNIT:
                            continue;
                        default:
                            if (sendResult != null) {
                                return sendResult;
                            }

                            throw e;
                        }
                    }
                    catch (InterruptedException e) {
                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        throw e;
                    }
                }
                else {
                    break;
                }
            } // end of for

            if (sendResult != null) {
                return sendResult;
            }

            String info =
                    String.format("Send [%d] times, still failed, cost [%d]ms, Topic: %s, BrokersSent: %s", //
                        times, //
                        (System.currentTimeMillis() - beginTimestamp), //
                        msg.getTopic(),//
                        Arrays.toString(brokersSent));

            info += FAQUrl.suggestTodo(FAQUrl.SEND_MSG_FAILED);

            throw new MQClientException(info, exception);
        }

        List<String> nsList = this.getmQClientFactory().getMQClientAPIImpl().getNameServerAddressList();
        if (null == nsList || nsList.isEmpty()) {
            throw new MQClientException("No name server address, please set it."
                    + FAQUrl.suggestTodo(FAQUrl.NAME_SERVER_ADDR_NOT_EXIST_URL), null);
        }

        throw new MQClientException("No route info of this topic, " + msg.getTopic()
                + FAQUrl.suggestTodo(FAQUrl.NO_TOPIC_ROUTE_INFO), null);
    }

    ## 通过索引文件消费消息
    ## 从索引文件的当前逻辑偏移位置起读取当前位置到文件尾的所有内容，已长度20递增获取内容，从中获取在commitlong中的位置信息及消息长度,
    ## 获取消息在commitlog的位置信息返回给消费者。。最多160000条
    public GetMessageResult getMessage(final String group, final String topic, final int queueId,
            final long offset, final int maxMsgNums, final SubscriptionData subscriptionData) {
        if (this.shutdown) {
            log.warn("message store has shutdown, so getMessage is forbidden");
            return null;
        }

        if (!this.runningFlags.isReadable()) {
            log.warn("message store is not readable, so getMessage is forbidden "
                    + this.runningFlags.getFlagBits());
            return null;
        }

        long beginTime = this.getSystemClock().now();

        // 枚举变量，取消息结果
        GetMessageStatus status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;
        // 当被过滤后，返回下一次开始的Offset
        long nextBeginOffset = offset;
        // 逻辑队列中的最小Offset
        long minOffset = 0;
        // 逻辑队列中的最大Offset
        long maxOffset = 0;

        GetMessageResult getResult = new GetMessageResult();

        // 有个读写锁，所以只访问一次，避免锁开销影响性能
        final long maxOffsetPy = this.commitLog.getMaxOffset();

        ConsumeQueue consumeQueue = findConsumeQueue(topic, queueId);
        if (consumeQueue != null) {
            minOffset = consumeQueue.getMinOffsetInQuque();
            maxOffset = consumeQueue.getMaxOffsetInQuque();

            if (maxOffset == 0) {
                status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;
                nextBeginOffset = 0;
            }
            else if (offset < minOffset) {
                status = GetMessageStatus.OFFSET_TOO_SMALL;
                nextBeginOffset = minOffset;
            }
            else if (offset == maxOffset) {
                status = GetMessageStatus.OFFSET_OVERFLOW_ONE;
                nextBeginOffset = offset;
            }
            else if (offset > maxOffset) {
                status = GetMessageStatus.OFFSET_OVERFLOW_BADLY;
                if (0 == minOffset) {
                    nextBeginOffset = minOffset;
                }
                else {
                    nextBeginOffset = maxOffset;
                }
            }
            else {
                SelectMapedBufferResult bufferConsumeQueue = consumeQueue.getIndexBuffer(offset);
                if (bufferConsumeQueue != null) {
                    try {
                        status = GetMessageStatus.NO_MATCHED_MESSAGE;

                        long nextPhyFileStartOffset = Long.MIN_VALUE;
                        long maxPhyOffsetPulling = 0;

                        int i = 0;
                        final int MaxFilterMessageCount = 16000;
                        boolean diskFallRecorded = false;
                        for (; i < bufferConsumeQueue.getSize() && i < MaxFilterMessageCount; i +=ConsumeQueue.CQStoreUnitSize) {
                            long offsetPy = bufferConsumeQueue.getByteBuffer().getLong();
                            int sizePy = bufferConsumeQueue.getByteBuffer().getInt();
                            long tagsCode = bufferConsumeQueue.getByteBuffer().getLong();

                            maxPhyOffsetPulling = offsetPy;

                            // 说明物理文件正在被删除
                            if (nextPhyFileStartOffset != Long.MIN_VALUE) {
                                if (offsetPy < nextPhyFileStartOffset)
                                    continue;
                            }

                            // 判断是否拉磁盘数据
                            boolean isInDisk = checkInDiskByCommitOffset(offsetPy, maxOffsetPy);
                            // 此批消息达到上限了
                            if (this.isTheBatchFull(sizePy, maxMsgNums, getResult.getBufferTotalSize(),getResult.getMessageCount(), isInDisk)) {
                                break;
                            }

                            // 消息过滤
                            if (this.messageFilter.isMessageMatched(subscriptionData, tagsCode)) {
                                SelectMapedBufferResult selectResult = this.commitLog.getMessage(offsetPy, sizePy);
                                if (selectResult != null) {
                                    this.storeStatsService.getGetMessageTransferedMsgCount().incrementAndGet();
                                        getResult.addMessage(selectResult);
                                        status = GetMessageStatus.FOUND;
                                        nextPhyFileStartOffset = Long.MIN_VALUE;
                                        // 统计读取磁盘落后情况
                                        if (diskFallRecorded) {
                                            diskFallRecorded = true;
                                            long fallBehind = consumeQueue.getMaxPhysicOffset() - offsetPy;
                                            brokerStatsManager.recordDiskFallBehind(group, topic, queueId,fallBehind);
                                    }
                                }
                                else {
                                    if (getResult.getBufferTotalSize() == 0) {
                                        status = GetMessageStatus.MESSAGE_WAS_REMOVING;
                                    }

                                    // 物理文件正在被删除，尝试跳过
                                    nextPhyFileStartOffset = this.commitLog.rollNextFile(offsetPy);
                                }
                            }
                            else {
                                if (getResult.getBufferTotalSize() == 0) {
                                    status = GetMessageStatus.NO_MATCHED_MESSAGE;
                                }

                                if (log.isDebugEnabled()) {
                                    log.debug("message type not matched, client: " + subscriptionData
                                            + " server: " + tagsCode);
                                }
                            }
                        }

                        nextBeginOffset = offset + (i / ConsumeQueue.CQStoreUnitSize);

                        // TODO 是否会影响性能，需要测试
                        long diff = this.getMaxPhyOffset() - maxPhyOffsetPulling;
                        long memory = (long) (StoreUtil.TotalPhysicalMemorySize * (this.messageStoreConfig.getAccessMessageInMemoryMaxRatio() / 100.0));
                        getResult.setSuggestPullingFromSlave(diff > memory);
                    }
                    finally {
                        // 必须释放资源
                        bufferConsumeQueue.release();
                    }
                }else {
                    status = GetMessageStatus.OFFSET_FOUND_NULL;
                    nextBeginOffset = consumeQueue.rollNextFile(offset);
                    log.warn("consumer request topic: " + topic + "offset: " + offset + " minOffset: "
                            + minOffset + " maxOffset: " + maxOffset + ", but access logic queue failed.");
                }
            }
        }
        // 请求的队列Id没有
        else {
            status = GetMessageStatus.NO_MATCHED_LOGIC_QUEUE;
            nextBeginOffset = 0;
        }

        if (GetMessageStatus.FOUND == status) {
            this.storeStatsService.getGetMessageTimesTotalFound().incrementAndGet();
        }
        else {
            this.storeStatsService.getGetMessageTimesTotalMiss().incrementAndGet();
        }
        long eclipseTime = this.getSystemClock().now() - beginTime;
        this.storeStatsService.setGetMessageEntireTimeMax(eclipseTime);

        getResult.setStatus(status);
        getResult.setNextBeginOffset(nextBeginOffset);
        getResult.setMaxOffset(maxOffset);
        getResult.setMinOffset(minOffset);
        return getResult;
    }

    ## commitlog 新增消息
    ## 读取commitlog最后一个文件写入消息，将消息位置、到小等消息索引定位文件(定长20)。
    public PutMessageResult putMessage(final MessageExtBrokerInner msg) {
        // Set the storage time
        msg.setStoreTimestamp(System.currentTimeMillis());
        // Set the message body BODY CRC (consider the most appropriate setting
        // on the client)
        msg.setBodyCRC(UtilAll.crc32(msg.getBody()));
        // Back to Results
        AppendMessageResult result = null;

        StoreStatsService storeStatsService = this.defaultMessageStore.getStoreStatsService();

        String topic = msg.getTopic();
        int queueId = msg.getQueueId();
        long tagsCode = msg.getTagsCode();

        final int tranType = MessageSysFlag.getTransactionValue(msg.getSysFlag());
        if (tranType == MessageSysFlag.TransactionNotType//
                || tranType == MessageSysFlag.TransactionCommitType) {
            // Delay Delivery
            if (msg.getDelayTimeLevel() > 0) {
                if (msg.getDelayTimeLevel() > this.defaultMessageStore.getScheduleMessageService()
                    .getMaxDelayLevel()) {
                    msg.setDelayTimeLevel(this.defaultMessageStore.getScheduleMessageService()
                        .getMaxDelayLevel());
                }

                topic = ScheduleMessageService.SCHEDULE_TOPIC;
                queueId = ScheduleMessageService.delayLevel2QueueId(msg.getDelayTimeLevel());
                tagsCode = this.defaultMessageStore.getScheduleMessageService().computeDeliverTimestamp(msg.getDelayTimeLevel(), msg.getStoreTimestamp());

                // Backup real topic, queueId
                MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_TOPIC, msg.getTopic());
                MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_QUEUE_ID,
                    String.valueOf(msg.getQueueId()));
                msg.setPropertiesString(MessageDecoder.messageProperties2String(msg.getProperties()));

                msg.setTopic(topic);
                msg.setQueueId(queueId);
            }
        }

        long eclipseTimeInLock = 0;
        synchronized (this) {
            long beginLockTimestamp = this.defaultMessageStore.getSystemClock().now();

            // Here settings are stored timestamp, in order to ensure an orderly
            // global
            msg.setStoreTimestamp(beginLockTimestamp);

            MapedFile mapedFile = this.mapedFileQueue.getLastMapedFile();
            if (null == mapedFile) {
                log.error("create maped file1 error, topic: " + msg.getTopic() + " clientAddr: "
                        + msg.getBornHostString());
                return new PutMessageResult(PutMessageStatus.CREATE_MAPEDFILE_FAILED, null);
            }
            result = mapedFile.appendMessage(msg, this.appendMessageCallback);
            switch (result.getStatus()) {
            case PUT_OK:
                break;
            case END_OF_FILE:
                // Create a new file, re-write the message
                mapedFile = this.mapedFileQueue.getLastMapedFile();
                if (null == mapedFile) {
                    // XXX: warn and notify me
                    log.error("create maped file2 error, topic: " + msg.getTopic() + " clientAddr: "
                            + msg.getBornHostString());
                    return new PutMessageResult(PutMessageStatus.CREATE_MAPEDFILE_FAILED, result);
                }
                result = mapedFile.appendMessage(msg, this.appendMessageCallback);
                break;
            case MESSAGE_SIZE_EXCEEDED:
                return new PutMessageResult(PutMessageStatus.MESSAGE_ILLEGAL, result);
            case UNKNOWN_ERROR:
                return new PutMessageResult(PutMessageStatus.UNKNOWN_ERROR, result);
            default:
                return new PutMessageResult(PutMessageStatus.UNKNOWN_ERROR, result);
            }

            DispatchRequest dispatchRequest = new DispatchRequest(//
                topic,// 1
                queueId,// 2
                result.getWroteOffset(),// 3
                result.getWroteBytes(),// 4
                tagsCode,// 5
                msg.getStoreTimestamp(),// 6
                result.getLogicsOffset(),// 7
                msg.getKeys(),// 8
                /**
                 * Transaction
                 */
                msg.getSysFlag(),// 9
                msg.getPreparedTransactionOffset());// 10

            this.defaultMessageStore.putDispatchRequest(dispatchRequest); // 索引服务

            eclipseTimeInLock = this.defaultMessageStore.getSystemClock().now() - beginLockTimestamp;
        } // end of synchronized

        if (eclipseTimeInLock > 1000) {
            // XXX: warn and notify me
            log.warn("putMessage in lock eclipse time(ms) " + eclipseTimeInLock);
        }

        PutMessageResult putMessageResult = new PutMessageResult(PutMessageStatus.PUT_OK, result);

        // Statistics
        storeStatsService.getSinglePutMessageTopicSizeTotal(topic).addAndGet(result.getWroteBytes());

        GroupCommitRequest request = null;

        // Synchronization flush
        if (FlushDiskType.SYNC_FLUSH == this.defaultMessageStore.getMessageStoreConfig().getFlushDiskType()) {
            GroupCommitService service = (GroupCommitService) this.flushCommitLogService;
            if (msg.isWaitStoreMsgOK()) {
                request = new GroupCommitRequest(result.getWroteOffset() + result.getWroteBytes());
                service.putRequest(request);
                boolean flushOK =
                        request.waitForFlush(this.defaultMessageStore.getMessageStoreConfig()
                            .getSyncFlushTimeout());
                if (!flushOK) {
                    log.error("do groupcommit, wait for flush failed, topic: " + msg.getTopic() + " tags: "
                            + msg.getTags() + " client address: " + msg.getBornHostString());
                    putMessageResult.setPutMessageStatus(PutMessageStatus.FLUSH_DISK_TIMEOUT);
                }
            }
            else {
                service.wakeup();
            }
        }
        // Asynchronous flush
        else {
            this.flushCommitLogService.wakeup();
        }

        // Synchronous write double
        if (BrokerRole.SYNC_MASTER == this.defaultMessageStore.getMessageStoreConfig().getBrokerRole()) {
            HAService service = this.defaultMessageStore.getHaService();
            if (msg.isWaitStoreMsgOK()) {
                // Determine whether to wait
                if (service.isSlaveOK(result.getWroteOffset() + result.getWroteBytes())) {
                    if (null == request) {
                        request = new GroupCommitRequest(result.getWroteOffset() + result.getWroteBytes());
                    }
                    service.putRequest(request);

                    service.getWaitNotifyObject().wakeupAll();

                    boolean flushOK =
                    // TODO
                            request.waitForFlush(this.defaultMessageStore.getMessageStoreConfig()
                                .getSyncFlushTimeout());
                    if (!flushOK) {
                        log.error("do sync transfer other node, wait return, but failed, topic: "
                                + msg.getTopic() + " tags: " + msg.getTags() + " client address: "
                                + msg.getBornHostString());
                        putMessageResult.setPutMessageStatus(PutMessageStatus.FLUSH_SLAVE_TIMEOUT);
                    }
                }
                // Slave problem
                else {
                    // Tell the producer, slave not available
                    putMessageResult.setPutMessageStatus(PutMessageStatus.SLAVE_NOT_AVAILABLE);
                }
            }
        }

        return putMessageResult;
    }

    ## 高可用实现 HA
    HAClient 向master汇报slave的最大偏移量
    ##HAConnection
    ReadSocketService 接收slave上报的最大偏移量
    WriteSocketService 向Slave写入数据
    AcceptSocketService 控制器 broker ha服务端口 默认haListenPort = 10912

broker配置
master1
brokerClusterName=AdpMqCluster
brokerName=broker-a
brokerId=0
namesrvAddr=mqnameserver1:9876;mqnameserver2:9876
defaultTopicQueueNums=4
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
listenPort=10911
deleteWhen=04 -- 什么时候可以删除文件(小时)
fileReservedTime=120 文件保留最长时间
mapedFileSizeCommitLog=1073741824
mapedFileSizeConsumeQueue=50000000
destroyMapedFileIntervalForcibly=120000
redeleteHangedFileInterval=120000
diskMaxUsedSpaceRatio=88
storePathRootDir=/data/rocketmq/store
storePathCommitLog=/data/rocketmq/store/commitlog
maxMessageSize=65536
flushCommitLogLeastPages=4
flushConsumeQueueLeastPages=2
flushCommitLogThoroughInterval=10000
flushConsumeQueueThoroughInterval=60000
brokerRole=ASYNC_MASTER ASYNC_MASTER -- ASYNC_MASTER 异步复制master，SYNC_MASTER 同步双写master SLAVE
flushDiskType=ASYNC_FLUSH 刷盘方式，异步或是同步
checkTransactionMessageEnable=false
sendMessageThreadPoolNums=128
pullMessageThreadPoolNums=128


master2
brokerName=broker-b
brokerId=1
namesrvAddr=mqnameserver1:9876;mqnameserver2:9876
defaultTopicQueueNums=4
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
listenPort=10911
deleteWhen=04
fileReservedTime=120
mapedFileSizeCommitLog=1073741824
mapedFileSizeConsumeQueue=50000000
destroyMapedFileIntervalForcibly=120000
redeleteHangedFileInterval=120000
diskMaxUsedSpaceRatio=88
storePathRootDir=/data/rocketmq/store
storePathCommitLog=/data/rocketmq/store/commitlog
maxMessageSize=65536
flushCommitLogLeastPages=4
flushConsumeQueueLeastPages=2
flushCommitLogThoroughInterval=10000
flushConsumeQueueThoroughInterval=60000
brokerRole=ASYNC_MASTER
flushDiskType=ASYNC_FLUSH
checkTransactionMessageEnable=false
sendMessageThreadPoolNums=128
pullMessageThreadPoolNums=128