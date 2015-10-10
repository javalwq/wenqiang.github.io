# AbstractApplicationContext
public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// 设置启动标记和启动时间
			prepareRefresh();

			// 调用AbstractRefreshableApplicationContext重写的refreshBeanFactory,完成配置文件的解析装载，实例化DefaultListableBeanFactory
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// 设置ApplicationContextAwareProcessor,忽略一些接口的自动装配(spring通过其它方式已处理)
			// 设置某些接口的自动装配功能：BeanFactory，ResourceLoader，ApplicationEventPublisher，ApplicationContext
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				// 对beanfactory的后置处理
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				// 调用factory processors 的 postProcessBeanFactory方法
				// 修改application context 内部初始化后的 beanfactory，如propertyPlaceHolderConfiger，CustomEditorConfigurer(属性解析器)
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				// 注册bean processors 用于拦截bean的创建 BeanPostProcessor
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				initMessageSource();

				// Initialize event multicaster for this context.
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				onRefresh();

				// Check for listener beans and register them.
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				// 初始化非lazy的单例实体
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				// 发送事件 在registerListeners中注册的
				finishRefresh();
			}

			catch (BeansException ex) {
				// Destroy already created singletons to avoid dangling resources.
				beanFactory.destroySingletons();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}
		}
	}

# DefaultListableBeanfactory 
# DefaultListableBeanfactory-> AbstractAutowireCapableBeanFactory -> AbstractBeanFactory 
# -> FactoryBeanRegistrySupport -> DefaultSingletonBeanRegistry ->SimpleAliasRegistry

#MergedBeanDefinitionPostProcessor -> BeanPostProcessor 修改bean定义，如 AutowiredAnnotationBeanPostProcessor和 CommonAnnotationBeanPostProcessor

## AbstractAutowireCapableBeanFactory.doCreateBean 
// 1、实例化BeanWrapper，如果是单例，从factorybeancache缓存中清除。
// 2、调用 MergedBeanDefinitionPostProcessor 拦截器
// 3、处理依赖，初始化bean（注入各种aware接口 beanname，beanclassloader，beanfactory），如果实现InitializingBean接口，调用afterPropertiesSet方法。
//	  如果有init method配置，调用该方法）
// 4、如果bean不是合成(synthetic)并且bean定义为空的bean,执行beanProcessor的postProcessAfterInitialization方法
// 5、如果bean实现了DisposableBean或是有自定义的destroy方法，则对bean进行DisposableBeanAdapter包裹

## AbstractBeanFactory.protected Object doGetBean(final String name, final Class requiredType, final Object[] args, boolean typeCheckOnly) throws BeansException {
// 1、返回类的全限定名称 
// 2、先从单例缓存中查找，如果存在该单例的facotybean，则使用该factorybean创建
// 3、创建bean实例 AbstractAutowireCapableBeanFactory.createBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
// 4、Object bean = resolveBeforeInstantiation(beanName, mbd);
// 5、resolveBeforeInstantiation 对实现了InstantiationAwareBeanPostProcessor接口调用 postProcessBeforeInstantiation （生成代理类）

protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (mbd.hasBeanClass() && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				bean = applyBeanPostProcessorsBeforeInstantiation(mbd.getBeanClass(), beanName);
				/**
					*for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
					*	BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
					**	if (beanProcessor instanceof InstantiationAwareBeanPostProcessor) {
					*		InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) beanProcessor;
					*		Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
					*		if (result != null) {
					*			return result;
					*		}
					*	}
					*}
				**/
				if (bean != null) {
					bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					/**
					*Object result = existingBean;
					*	for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
					*		BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
					*		result = beanProcessor.postProcessAfterInitialization(result, beanName);
					*	}
					*	return result;
					*/
				}
			}
			mbd.beforeInstantiationResolved = Boolean.valueOf(bean != null);
		}
		return bean;
	}

protected Object createBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
			throws BeanCreationException {
		AccessControlContext acc = AccessController.getContext();
		return AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				if (logger.isDebugEnabled()) {
					logger.debug("Creating instance of bean '" + beanName + "'");
				}
				// Make sure bean class is actually resolved at this point.
				resolveBeanClass(mbd, beanName);

				// Prepare method overrides.
				try {
					mbd.prepareMethodOverrides();
				}
				catch (BeanDefinitionValidationException ex) {
					throw new BeanDefinitionStoreException(mbd.getResourceDescription(),
							beanName, "Validation of method overrides failed", ex);
				}

				try {
					// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
					// InstantiationAwareBeanPostProcessor 
					Object bean = resolveBeforeInstantiation(beanName, mbd);
					if (bean != null) {
						return bean;
					}
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"BeanPostProcessor before instantiation of bean failed", ex);
				}

				Object beanInstance = doCreateBean(beanName, mbd, args);
				if (logger.isDebugEnabled()) {
					logger.debug("Finished creating instance of bean '" + beanName + "'");
				}
				return beanInstance;
			}
		}, acc);
	}

protected Object doGetBean(final String name, final Class requiredType, final Object[] args, boolean typeCheckOnly) throws BeansException {
		final String beanName = transformedBeanName(name);
		Object bean = null;

		// Eagerly check singleton cache for manually registered singletons.
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			if (logger.isDebugEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				String nameToLookup = originalBeanName(name);
				if (args != null) {
					// Delegation to parent with explicit args.
					return parentBeanFactory.getBean(nameToLookup, args);
				}
				else {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}

			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
			checkMergedBeanDefinition(mbd, beanName, args);

			// Guarantee initialization of beans that the current bean depends on.
			String[] dependsOn = mbd.getDependsOn();
			if (dependsOn != null) {
				for (int i = 0; i < dependsOn.length; i++) {
					String dependsOnBean = dependsOn[i];
					getBean(dependsOnBean);
					registerDependentBean(dependsOnBean, beanName);
				}
			}

			// Create bean instance.
			if (mbd.isSingleton()) {
				sharedInstance = getSingleton(beanName, new ObjectFactory() {
					public Object getObject() throws BeansException {
						try {
							return createBean(beanName, mbd, args);
						}
						catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							destroySingleton(beanName);
							throw ex;
						}
					}
				});
				bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
			}

			else if (mbd.isPrototype()) {
				// It's a prototype -> create a new instance.
				Object prototypeInstance = null;
				try {
					beforePrototypeCreation(beanName);
					prototypeInstance = createBean(beanName, mbd, args);
				}
				finally {
					afterPrototypeCreation(beanName);
				}
				bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
			}

			else {
				String scopeName = mbd.getScope();
				final Scope scope = (Scope) this.scopes.get(scopeName);
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope '" + scopeName + "'");
				}
				try {
					Object scopedInstance = scope.get(beanName, new ObjectFactory() {
						public Object getObject() throws BeansException {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							}
							finally {
								afterPrototypeCreation(beanName);
							}
						}
					});
					bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
				}
				catch (IllegalStateException ex) {
					throw new BeanCreationException(beanName,
							"Scope '" + scopeName + "' is not active for the current thread; " +
							"consider defining a scoped proxy for this bean if you intend to refer to it from a singleton",
							ex);
				}
			}
		}

		// Check if required type matches the type of the actual bean instance.
		if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}
		return bean;
	}

// 后处理的使用