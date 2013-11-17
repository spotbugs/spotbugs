package edu.umd.cs.findbugs.flybush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.junit.Assert;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Maps;

public abstract class AbstractFlybushServletTest<PersistenceHelper extends BasePersistenceHelper> extends TestCase {

    protected static final long SAMPLE_TIMESTAMP = System.currentTimeMillis()
            - 3L*30*24*3600*1000;

    protected HttpServletResponse mockResponse;

    protected ByteArrayOutputStream outputCollector;

    protected AbstractFlybushServlet<PersistenceHelper> servlet;

    protected HttpServletRequest mockRequest;

    protected PersistenceHelper persistenceHelper;

    protected FlybushServletTestHelper<? extends PersistenceHelper> testHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testHelper.setUp();
        persistenceHelper = testHelper.createPersistenceHelper(testHelper.getPersistenceManager());
        initServletAndMocks();
    }

    @Override
    protected void tearDown() throws Exception {
        testHelper.tearDown();
    }

    public void setFlybushServletTestHelper(FlybushServletTestHelper<? extends PersistenceHelper> flybushServletTestHelper) {
        this.testHelper = flybushServletTestHelper;
    }

    protected abstract AbstractFlybushServlet<PersistenceHelper> createServlet();

    // ========================= supporting methods
    // ================================

    
    protected void initServletAndMocks() throws IOException, ServletException {
        
        servlet = createServlet();
        servlet.setPersistenceHelper(persistenceHelper);
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
        outputCollector = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                outputCollector.write(b);
            }
        };
        Mockito.when(mockResponse.getOutputStream()).thenReturn(servletOutputStream);
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(servletOutputStream, true));
    }


    protected void executeGet(String requestUri) throws IOException, ServletException {
        executeGet(servlet, requestUri);
    }

    protected void executeGet(AbstractFlybushServlet<PersistenceHelper> servlet, String requestUri) throws IOException, ServletException {
        prepareRequestAndResponse(requestUri, null);

        servlet.doGet(mockRequest, mockResponse);
    }

    protected void executePost(String requestUri, byte[] input) throws IOException {
        executePost(servlet, requestUri, input);
    }

    protected void executePost(AbstractFlybushServlet<PersistenceHelper> servlet, String requestUri, byte[] input) throws IOException {
        prepareRequestAndResponse(requestUri, input);

        servlet.doPost(mockRequest, mockResponse);
    }

    protected void checkResponse(int responseCode, String expectedOutput) throws UnsupportedEncodingException {
        checkResponse(responseCode, "text/plain", expectedOutput);
    }

    protected void checkResponse(int responseCode, String contentType, String expectedOutput) throws UnsupportedEncodingException {
        checkResponse(responseCode);
        Mockito.verify(mockResponse, Mockito.atLeastOnce()).setContentType(contentType);
        String output = new String(outputCollector.toByteArray(), "UTF-8");
        Assert.assertEquals(expectedOutput.trim(), output.replaceAll("\r", "").trim());
    }

    protected void checkResponse(int responseCode) {
        Mockito.verify(mockResponse).setStatus(responseCode);
    }

    // ================================ end of helper methods
    // ===============================

    protected void prepareRequestAndResponse(String requestUri, byte[] input) throws IOException {
        int qpos = requestUri.indexOf('?');
        final Map<String,String> params = Maps.newHashMap();
        String actualUri;
        if (qpos == -1) {
            actualUri = requestUri;
        } else {
            actualUri = requestUri.substring(0, qpos);
            String paramstr = requestUri.substring(qpos+1);
            parseParams(params, paramstr);
        }
        Mockito.when(mockRequest.getRequestURI()).thenReturn(actualUri);
        Mockito.when(mockRequest.getParameter(Matchers.<String>any())).thenAnswer(new Answer<String>() {
            @Override
			public String answer(InvocationOnMock inv) throws Throwable {
                String key = (String) inv.getArguments()[0];
                return params.get(key);
            }
        });
        if (input != null) {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(input);
            Mockito.when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return inputStream.read();
                }
            });
        }
    }

    private void parseParams(Map<String, String> params, String paramstr) {
        for (String pair : paramstr.split("&")) {
            int epos = pair.indexOf("=");
            if (epos == -1) {
                params.put(pair, "");
            } else {
                params.put(pair.substring(0, epos), pair.substring(epos + 1));
            }
        }
    }


    protected PersistenceManager getPersistenceManager() {
        return testHelper.getPersistenceManager();
    }

   
}
