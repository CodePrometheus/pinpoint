/*
 * Copyright 2022 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.httpclient5.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.config.HttpDumpConfig;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PluginLogManager;
import com.navercorp.pinpoint.bootstrap.logging.PluginLogger;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientHeaderAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestWrapper;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestWrapperAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.DefaultRequestTraceWriter;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestTraceWriter;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.CookieExtractor;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.CookieRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.CookieRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.EntityExtractor;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.EntityRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.EntityRecorderFactory;
import com.navercorp.pinpoint.common.util.ArrayArgumentUtils;
import com.navercorp.pinpoint.plugin.httpclient5.HostUtils;
import com.navercorp.pinpoint.plugin.httpclient5.HttpClient5Constants;
import com.navercorp.pinpoint.plugin.httpclient5.HttpClient5CookieExtractor;
import com.navercorp.pinpoint.plugin.httpclient5.HttpClient5EntityExtractor;
import com.navercorp.pinpoint.plugin.httpclient5.HttpClient5PluginConfig;
import com.navercorp.pinpoint.plugin.httpclient5.HttpClient5RequestWrapper;
import com.navercorp.pinpoint.plugin.httpclient5.HttpRequest5ClientHeaderAdaptor;
import com.navercorp.pinpoint.plugin.httpclient5.HttpRequestGetter;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.lang.reflect.Method;
import java.util.Arrays;

public class CloseableHttpAsyncClientDoExecuteInterceptor implements AroundInterceptor {
    private final PluginLogger logger = PluginLogManager.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;
    private final ClientRequestRecorder<ClientRequestWrapper> clientRequestRecorder;
    private final CookieRecorder<HttpRequest> cookieRecorder;
    private final EntityRecorder<HttpRequest> entityRecorder;

    private final RequestTraceWriter<HttpRequest> requestTraceWriter;
    private final boolean markError;

    public CloseableHttpAsyncClientDoExecuteInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;

        final boolean param = HttpClient5PluginConfig.isParam(traceContext.getProfilerConfig());
        final HttpDumpConfig httpDumpConfig = HttpClient5PluginConfig.getHttpDumpConfig(traceContext.getProfilerConfig());

        ClientRequestAdaptor<ClientRequestWrapper> clientRequestAdaptor = ClientRequestWrapperAdaptor.INSTANCE;
        this.clientRequestRecorder = new ClientRequestRecorder<>(param, clientRequestAdaptor);

        CookieExtractor<HttpRequest> cookieExtractor = HttpClient5CookieExtractor.INSTANCE;
        this.cookieRecorder = CookieRecorderFactory.newCookieRecorder(httpDumpConfig, cookieExtractor);

        EntityExtractor<HttpRequest> entityExtractor = HttpClient5EntityExtractor.INSTANCE;
        this.entityRecorder = EntityRecorderFactory.newEntityRecorder(httpDumpConfig, entityExtractor);

        ClientHeaderAdaptor<HttpRequest> clientHeaderAdaptor = new HttpRequest5ClientHeaderAdaptor();
        this.requestTraceWriter = new DefaultRequestTraceWriter<>(clientHeaderAdaptor, traceContext);

        this.markError = HttpClient5PluginConfig.isMarkError(traceContext.getProfilerConfig());
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        /**
         Span{timeRecording=true, traceRoot=RemoteTraceRootImpl{traceId=DefaultTraceId{
         transactionId=http5-agentId^1739451319999^1, transactionUId=null, 
         parentSpanId=-1, spanId=6912225487037848714, flags=0}, agentId='http5-agentId', 
         localTransactionId=1, traceStartTime=1739451340900, shared=com.navercorp.pinpoint.profiler.context.id.DefaultShared@4afef030},
         startTime=1739451340900, elapsed=0, serviceType=1010, remoteAddr='0:0:0:0:0:0:0:1', annotations=null, spanEventList=null,
         parentApplicationName='null', parentApplicationType=0, acceptorHost='null', apiId=10, exceptionInfo=null}com.navercorp.pinpoint.profiler.context.Span@5cb79446
         */
        final Trace trace = traceContext.currentRawTraceObject();
        System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|before trace = " + trace);
        if (trace == null) {
            return;
        }

        try {
            final HttpHost httpHost = ArrayArgumentUtils.getArgument(args, 0, HttpHost.class);
            final HttpRequest httpRequest = getHttpRequest(args);
            if (httpRequest == null) {
                return;
            }

            final String host = HostUtils.get(httpHost, httpRequest);
            final boolean sampling = trace.canSampled();
            if (!sampling) {
                if (httpRequest != null) {
                    this.requestTraceWriter.write(httpRequest);
                }
                return;
            }

            final SpanEventRecorder recorder = trace.traceBlockBegin();
            System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|before recorder = " + recorder);
            // set remote trace
            final TraceId nextId = trace.getTraceId().getNextTraceId(); // DefaultTraceId{transactionId=http5-agentId^1739451319999^1, transactionUId=null, parentSpanId=6912225487037848714, spanId=2416187240697580862, flags=0}
            System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|before nextId = " + nextId);
            recorder.recordNextSpanId(nextId.getSpanId());
            recorder.recordServiceType(HttpClient5Constants.HTTP_CLIENT5); // 2416187240697580862
            this.requestTraceWriter.write(httpRequest, nextId, host);
            // HttpContext
            final AsyncContextAccessor asyncContextAccessor = ArrayArgumentUtils.getArgument(args, 4, AsyncContextAccessor.class); // HttpContext
            System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|before asyncContextAccessor = " + asyncContextAccessor);
            if (asyncContextAccessor != null) {
                /**
                 DefaultAsyncContext{asyncId=DefaultAsyncId{asyncId=1, sequence=0}, traceRoot=RemoteTraceRootImpl{traceId=DefaultTraceId{transactionId=http5-agentId^1739451319999^1, 
                 transactionUId=null, parentSpanId=-1, spanId=6912225487037848714, flags=0}, agentId='http5-agentId', localTransactionId=1, 
                 traceStartTime=1739451340900, shared=com.navercorp.pinpoint.profiler.context.id.DefaultShared@4afef030}, 
                 asyncState=null}
                 */
                final AsyncContext asyncContext = recorder.recordNextAsyncContext();
                System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|before asyncContext = " + asyncContext);
                asyncContextAccessor._$PINPOINT$_setAsyncContext(asyncContext);
                Method[] declaredMethods = HttpContext.class.getDeclaredMethods();
                System.out.println("declaredMethods = " + Arrays.toString(declaredMethods));
            }
        } catch (Throwable t) {
            logger.warn("Failed to BEFORE process. {}", t.getMessage(), t);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }

        /**
         AsyncDefaultTrace{asyncState=LoggingAsyncState{delegate=ListenableAsyncState{asyncStateListener=com.navercorp.pinpoint.profiler.context.SpanAsyncStateListener@4bfc78d7, 
         setup=false, await=false, finish=false}}} DefaultTrace{traceRoot=RemoteTraceRootImpl{traceId=DefaultTraceId{transactionId=http5-agentId^1739367474166^1,
         transactionUId=null, parentSpanId=-1, spanId=3165496175886451607, flags=0}, agentId='http5-agentId', localTransactionId=1, 
         traceStartTime=1739367505443, shared=com.navercorp.pinpoint.profiler.context.id.DefaultShared@370f3da9}}
         */
        final Trace trace = traceContext.currentTraceObject();
        System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|after trace = " + trace);
        if (trace == null) {
            return;
        }

        try {
            final HttpHost httpHost = ArrayArgumentUtils.getArgument(args, 0, HttpHost.class);
            final HttpRequest httpRequest = getHttpRequest(args);
            if (httpRequest == null) {
                return;
            }
            final String host = HostUtils.get(httpHost, httpRequest);
            SpanEventRecorder recorder = trace.currentSpanEventRecorder(); // SpanEvent{stackId=-1, timeRecording=true, startTime=1739453102498, elapsedTime=0, asyncIdObject=DefaultAsyncId{asyncId=1, sequence=0}, sequence=3, serviceType=9062, endPoint='null', annotations=null, depth=4, nextSpanId=-7985839919187174842, destinationId='null', apiId=0, exceptionInfo=null, executeQueryType=false} 
            System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|after recorder = " + recorder);
            // Accessing httpRequest here not BEFORE() because it can cause side effect.
            ClientRequestWrapper clientRequest = new HttpClient5RequestWrapper(httpRequest, host);
            this.clientRequestRecorder.record(recorder, clientRequest, throwable);
            this.cookieRecorder.record(recorder, httpRequest, throwable);
            this.entityRecorder.record(recorder, httpRequest, throwable);
            recorder.recordApi(methodDescriptor);
            recorder.recordException(markError, throwable);
            if (result instanceof AsyncContextAccessor) {
                // HttpContext
                final AsyncContext asyncContext = AsyncContextAccessorUtils.getAsyncContext(args, 4); // DefaultAsyncContext{asyncId=DefaultAsyncId{asyncId=1, sequence=0}, traceRoot=RemoteTraceRootImpl{traceId=DefaultTraceId{transactionId=http5-agentId^1739453074645^1, transactionUId=null, parentSpanId=-1, spanId=9088218635700154016, flags=0}, agentId='http5-agentId', localTransactionId=1, traceStartTime=1739453092222, shared=com.navercorp.pinpoint.profiler.context.id.DefaultShared@5b9d5443}, asyncState=null}
                System.out.println("my|http5|CloseableHttpAsyncClientDoExecuteInterceptor|after asyncContext = " + asyncContext);
                if (asyncContext != null) {
                    ((AsyncContextAccessor) result)._$PINPOINT$_setAsyncContext(asyncContext);
                }
            }
        } catch (Throwable t) {
            logger.warn("Failed to AFTER process. {}", t.getMessage(), t);
        } finally {
            trace.traceBlockEnd();
        }
    }

    HttpRequest getHttpRequest(final Object[] args) {
        final HttpRequestGetter httpRequestGetter = ArrayArgumentUtils.getArgument(args, 1, HttpRequestGetter.class);
        if (httpRequestGetter == null) {
            return null;
        }
        final HttpRequest httpRequest = httpRequestGetter._$PINPOINT$_getHttpRequest();
        return httpRequest;
    }
}
