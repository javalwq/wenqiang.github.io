## ExtensionLoader
扩展类加载器。将实现与接口分离，非强依赖。wrap如同aop，加载时如果类含有传入类型的拷贝构造函数时，则将该wrapper类返回。
ExtensionLoader(Class<?> type) 每一class类型一个ExtensionLoader。同一个接口的实现类可能会有wraper、adpative。
1、 Adaptive
	如过类包含Adaptive注解，则返回Adaptive的实现类，如果没有Adaptive实现类。
	如同字节码增加，在spring中是通过配置文件或是注解实现注入，而此处要通过编码实现，接口的默认实现是通过动态拼接java代码并编译，
	主要逻辑是通过url获取对应的配置信息，再通过名称查找扩展点进行方法调用。
	如果Adaptive注解在类上，则通过extentionloader.getExtensionLoader(A.Class).getAdaptiveExtension 如AdaptiveExtensionFactory 则返回该注解的实例，
	如果注解在方法上则该方法通过字节码增强实现。字节码主要是根据url中约定的关键查找扩展，如
	ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(url.getParameter("protocol")); // default dubbo
	ExtensionLoader.getExtensionLoader(Dispatcher.class).getExtension(url.getParameter("dispatcher"); // default  url.getParameter("channel.handler", "all"))
	ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension(url.getParameter(“proxy”)); // defualt javassist
	ExtensionLoader.getExtensionLoader(Threadpool.class).getExtension(url.getParameter(“threadpool”))); // default fix
	ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(url.getParameter(transporter))); // default netty
2、wrapper 代理的实现方式
	获取扩展时如果实现类包含了对应类型的拷贝构造函数，则将该类返回。可进行多次wrapper。如获取dubbo扩展,实现dubbo扩展并且有该扩展的拷贝构造函数有ProtocolFilterWrapper
	和ProtocolListenerWrapper,则在ProtocolFilterWrapper注入DubboProtocol，再将ProtocolFilterWrapper注入ProtocolListenerWrapper

3、代理(Proxy)
	继承结构 ProxyFactory<-AbstractProxyFactory<-JavassistProxyFactory和JdkProxyFactory
			 ProxyFactory<-StubProxyFactoryWrapper



4、服务引用过程
	ReferenceBean.afterPropertiesSet() -> ReferenceConfig.init() -> ReferenceConfig.createProxy(Map<String, String> map) ->
	invoker = refprotocol.refer(interfaceClass, urls.get(0)); // refprotocol = RegistryProtocol
	private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, NetUtils.getLocalHost(), 0, type.getName(), directory.getUrl().getParameters());
        if (! Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                    Constants.CHECK_KEY, String.valueOf(false)));
        }
        directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY,
                Constants.PROVIDERS_CATEGORY
                + "," + Constants.CONFIGURATORS_CATEGORY
                + "," + Constants.ROUTERS_CATEGORY));
        return cluster.join(directory); 
    }
    class FailbackCluster {
    	public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        	return new FailbackClusterInvoker<T>(directory);
    	}
    }
    生成代理
	StubProxyFactoryWrapper.getProxy(Invoker<T> invoker) -> 
	判读url是否有interfaces参数，如存在则为接口和urlinterfaces参数中的接口生成代理类，如果参数为空，则为接口和EchoService接口生成代理类。
	AbstractProxyFactory.getProxy(Invoker<T> invoker) -> JavassistProxyFactory.getProxy(Invoker<T> invoker, Class<?>[] interfaces)->
	Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker))
	Proxy.getProxy 返回
	class Proxy$0 extends Proxy{
	public Object newInstance(InvocationHandler h) {
		return new Proxy$0(h);
		}
	}
	class Proxy$0{
		public static java.lang.reflect.Method[] methods; // 代理接口中所有方法
		private InvocationHandler handler;
		public Proxy$0(InvocationHandler handler) {
			this.handler = handler;
		}
		// 接口
		public java.lang.String sayHello(java.lang.String arg0){
			Object[] args = new Object[1]; 
			args[0] = arg0; 
			Object ret = handler.invoke(this, methods[0], args); // 根据methods数替代生成
			return (java.lang.String)ret;
		}
	}

	class Cluster{
		<T> Invoker<T> join(Directory<T> directory) throws RpcException
	} 
	 继承结构
	Cluster<-FailbackCluster return FailbackClusterInvoker
		   <-FailoverCluster return FailbackClusterInvoker
		   <-AvailableCluster return  new AbstractClusterInvoker<T>(directory) {
	            public Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
	                for (Invoker<T> invoker : invokers) {
	                    if (invoker.isAvailable()) {
	                        return invoker.invoke(invocation);
	                    }
	                }
	                throw new RpcException("No provider available in " + invokers);
	            }
	        };
	        <-FailsafeCluster return FailsafeClusterInvoker
	        <-ForkingCluster return ForkingClusterInvoker
	        <-FailfastCluster return FailfastClusterInvoker
	        <-BroadcastCluster return BroadcastClusterInvoker
	        <-MockClusterWrapper return new MockClusterInvoker<T>(directory,this.cluster.join(directory));
	        <-MergeableCluster return MergeableClusterInvoker

接口调用时触发 InvokerInvocationHandler.invoke ->内部持有的invoker.invoke(new RpcInvocation(method, args)).recreate()->
AbstractClusterInvoker.invoke(final Invocation invocation)->
	查找可用服务（methodInvokerMap Map<String, List<Invoker<T>>> 缓存所有可用远程服务）
	AbstractDirectory.List()->RegistryDirectory.list(Invocation invocation)
	服务缓存实现
	RegistryDirectory.notify(List<URL> urls) 
	url向 Invoker转换
	RegistryDirectory.refreshInvoker(List<URL> invokerUrls) ->
	toInvokers(List<URL> urls){
		invoker = new InvokerDelegete<T>(protocol.refer(serviceType, url), url, providerUrl); // url 服务地址
		{
			##DubboProtocol
			public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
	        // create rpc invoker.
	        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
	        invokers.add(invoker);
	        return invoker;
    }
		}
	} 
->loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE); //负载算法
->FailfastClusterInvoker（Invoker的具体实现).doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance)
->InvokerWrapper.invoke(Invocation invocation)
->AbstractInvoker.invoke() 
->DubboInvoker.doInvoke(final Invocation invocation) { // rpc通信
		RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.VERSION_KEY, version);
        
        ExchangeClient currentClient;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        try {
            boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
            boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
            int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY,Constants.DEFAULT_TIMEOUT);
            if (isOneway) {
            	boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);
                currentClient.send(inv, isSent);
                RpcContext.getContext().setFuture(null);
                return new RpcResult();
            } else if (isAsync) {
            	ResponseFuture future = currentClient.request(inv, timeout) ;
                RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));
                return new RpcResult();
            } else {
            	RpcContext.getContext().setFuture(null);
                return (Result) currentClient.request(inv, timeout).get();
            }
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        }
}
// TODO 网络通信

## 服务发布
ServiceBean 解析过程中创建，当spring支持事件机制并且deplay == null 或 deplay == -1 时延迟到spring初始化完成后发布服务
ServiceConfig.doExport ->ServiceConfig.doExportUrls ->ServiceConfig.doExportUrlsFor1Protocol
->protocol.export((Invoker<T> invoker))->exchanger.bind(ExchangeHandler)->transporter.bind()

private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        String name = protocolConfig.getName();
        if (name == null || name.length() == 0) {
            name = "dubbo";
        }
        String host = protocolConfig.getHost();
        if (provider != null && (host == null || host.length() == 0)) {
            host = provider.getHost();
        }
        boolean anyhost = false;
        if (NetUtils.isInvalidLocalHost(host)) {
            anyhost = true;
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                logger.warn(e.getMessage(), e);
            }
            if (NetUtils.isInvalidLocalHost(host)) {
                if (registryURLs != null && registryURLs.size() > 0) {
                    for (URL registryURL : registryURLs) {
                        try {
                            Socket socket = new Socket();
                            try {
                                SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                socket.connect(addr, 1000);
                                host = socket.getLocalAddress().getHostAddress();
                                break;
                            } finally {
                                try {
                                    socket.close();
                                } catch (Throwable e) {}
                            }
                        } catch (Exception e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                }
                if (NetUtils.isInvalidLocalHost(host)) {
                    host = NetUtils.getLocalHost();
                }
            }
        }

        Integer port = protocolConfig.getPort();
        if (provider != null && (port == null || port == 0)) {
            port = provider.getPort();
        }
        final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
        if (port == null || port == 0) {
            port = defaultPort;
        }
        if (port == null || port <= 0) {
            port = getRandomPort(name);
            if (port == null || port < 0) {
                port = NetUtils.getAvailablePort(defaultPort);
                putRandomPort(name, port);
            }
            logger.warn("Use random available port(" + port + ") for protocol " + name);
        }

        Map<String, String> map = new HashMap<String, String>();
        if (anyhost) {
            map.put(Constants.ANYHOST_KEY, "true");
        }
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
        map.put(Constants.DUBBO_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        appendParameters(map, application);
        appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY);
        appendParameters(map, protocolConfig);
        appendParameters(map, this);
        if (methods != null && methods.size() > 0) {
            for (MethodConfig method : methods) {
                appendParameters(map, method, method.getName());
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                List<ArgumentConfig> arguments = method.getArguments();
                if (arguments != null && arguments.size() > 0) {
                    for (ArgumentConfig argument : arguments) {
                        //类型自动转换.
                        if(argument.getType() != null && argument.getType().length() >0){
                            Method[] methods = interfaceClass.getMethods();
                            //遍历所有方法
                            if(methods != null && methods.length > 0){
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    //匹配方法名称，获取方法签名.
                                    if(methodName.equals(method.getName())){
                                        Class<?>[] argtypes = methods[i].getParameterTypes();
                                        //一个方法中单个callback
                                        if (argument.getIndex() != -1 ){
                                            if (argtypes[argument.getIndex()].getName().equals(argument.getType())){
                                                appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                            }else {
                                                throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :"+argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            //一个方法中多个callback
                                            for (int j = 0 ;j<argtypes.length ;j++) {
                                                Class<?> argclazz = argtypes[j];
                                                if (argclazz.getName().equals(argument.getType())){
                                                    appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j){
                                                        throw new IllegalArgumentException("argument config error : the index attribute and type attirbute not match :index :"+argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }else if(argument.getIndex() != -1){
                            appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                        }else {
                            throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        if (ProtocolUtils.isGeneric(generic)) {
            map.put("generic", generic);
            map.put("methods", Constants.ANY_VALUE);
        } else {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if(methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            }
            else {
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        if (! ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) {
                map.put("token", UUID.randomUUID().toString());
            } else {
                map.put("token", token);
            }
        }
        if ("injvm".equals(protocolConfig.getName())) {
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }
        // 导出服务
        String contextPath = protocolConfig.getContextpath();
        if ((contextPath == null || contextPath.length() == 0) && provider != null) {
            contextPath = provider.getContextpath();
        }
        URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);

        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

        String scope = url.getParameter(Constants.SCOPE_KEY);
        //配置为none不暴露
        if (! Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {

            //配置不是remote的情况下做本地暴露 (配置为remote，则表示只暴露远程服务)
            if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
                exportLocal(url);
            }
            //如果配置不是local则暴露为远程服务.(配置为local，则表示只暴露远程服务)
            if (! Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope) ){
                if (logger.isInfoEnabled()) {
                    logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                }
                if (registryURLs != null && registryURLs.size() > 0
                        && url.getParameter("register", true)) {
                    for (URL registryURL : registryURLs) {
                        url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));
                        URL monitorUrl = loadMonitor(registryURL);
                        if (monitorUrl != null) {
                            url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                        }
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));

                        Exporter<?> exporter = protocol.export(invoker);
                        exporters.add(exporter);
                    }
                } else {
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);

                    Exporter<?> exporter = protocol.export(invoker);
                    exporters.add(exporter);
                }
            }
        }
        this.urls.add(url);
    }


provider,application,moudle,monitor,registries,protocols

dubbo 必须显示配置的是application，registry。protocol未配置时默认是dubbo
服务发布处理顺序:ProtocolFilterWrapper.export->ProtocolListenerWrapper.export->RegistryProtocol.export

## netty handler包装顺序 
DecodeHandler(HeaderExchangeHandler(DubboProtocol.ExchangeHandlerAdapter))
MultiMessageHandler(HeartbeatHandler(AllChannelHandler(DecodeHandler)通过dispacher获取))
NettyHandler(MultiMessageHandler)
