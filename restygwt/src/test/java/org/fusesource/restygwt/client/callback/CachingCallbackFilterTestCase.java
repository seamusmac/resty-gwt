package org.fusesource.restygwt.client.callback;

import java.util.HashMap;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.cache.CacheKey;
import org.fusesource.restygwt.client.cache.DefaultQueueableCacheStorage;
import org.fusesource.restygwt.client.cache.SimpleCacheKey;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.junit.GWTMockUtilities;


public class CachingCallbackFilterTestCase extends TestCase {

    private CachingCallbackFilter filter;
    private DefaultQueueableCacheStorage storage;
    private SimpleCacheKey key;
    
    protected void setUp() throws Exception{
        super.setUp();
        GWTMockUtilities.disarm();

        this.storage = new DefaultQueueableCacheStorage();
        this.key = new SimpleCacheKey("key");
        final CacheKey k = key;
        this.filter = new CachingCallbackFilter(this.storage){

            @Override
            protected CacheKey cacheKey(RequestBuilder builder) {
                return k;
            }            
        };
    }
    
    protected void tearDown() {
        GWTMockUtilities.restore();
    }
    
    public void testNoCallbacksSuccess() throws Exception{
        Response response = EasyMock.createMock(Response.class);
        Method method = EasyMock.createMock(Method.class);
        EasyMock.expect(response.getStatusCode()).andReturn(201);
        EasyMock.expect(method.getData()).andReturn(new HashMap<String,String>());
        EasyMock.replay(response, method);
        
        filter.filter(method, response, null);
        
        EasyMock.verify(response, method);
        assertEquals(response, this.storage.getResultOrReturnNull(key));
    }

    public void testNoCallbacksError() throws Exception{
        Response response = EasyMock.createMock(Response.class);
        Method method = EasyMock.createMock(Method.class);
        EasyMock.expect(response.getStatusCode()).andReturn(401);
        EasyMock.replay(response, method);
        
        filter.filter(method, response, null);
        
        EasyMock.verify(response, method);
        assertNull(this.storage.getResultOrReturnNull(key));
    }

    public void testManyCallbacksSuccess() throws Exception{
        Response response = EasyMock.createMock(Response.class);
        Method method = EasyMock.createMock(Method.class);
        EasyMock.expect(method.getData()).andReturn(new HashMap<String,String>());
        RequestCallback[] myCallbacks = new RequestCallback[4];
        for( int i = 0; i < myCallbacks.length; i++){
            myCallbacks[i] = EasyMock.createMock(RequestCallback.class);
            myCallbacks[i].onResponseReceived(null, null);
            EasyMock.replay(myCallbacks[i]);
        }
        
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        
        EasyMock.replay(response, method);
        
        for( int i = 1; i < myCallbacks.length; i++){
            this.storage.addCallback(key, myCallbacks[i]);
        }
        
        RequestCallback callback = filter.filter(method, response, myCallbacks[0]);

        assertNotSame(callback, myCallbacks[0]);
        
        callback.onResponseReceived(null, null);
        
        EasyMock.verify(response, method);
        for(RequestCallback rc: myCallbacks){
            EasyMock.verify(rc);
        }
        
        assertEquals(response, this.storage.getResultOrReturnNull(key));
    }
}