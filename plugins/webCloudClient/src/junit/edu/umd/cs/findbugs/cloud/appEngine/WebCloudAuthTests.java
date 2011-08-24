package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import org.mockito.Mockito;

import static edu.umd.cs.findbugs.cloud.Cloud.SigninState.UNAUTHENTICATED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebCloudAuthTests extends AbstractWebCloudTest {

    // ===================== soft signin ==========================

    /**
     * soft sign-in should try to sign in with an existing session ID, then fail
     * silently
     */
    public void testSoftSignInFailSilently() throws Exception {
        final HttpURLConnection logInConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(logInConn);
        MockWebCloudClient cloud = createWebCloudClient(logInConn);
        WebCloudNetworkClient networkClient = mock(WebCloudNetworkClient.class);
        cloud.setNetworkClient(networkClient);

        when(networkClient.initialize()).thenReturn(false);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        assertEquals(0, cloud.urlsRequested.size());
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testSoftSignInSkipWhenHeadless() throws Exception {
        final HttpURLConnection logInConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(logInConn);
        MockWebCloudClient cloud = createWebCloudClient(logInConn);
        WebCloudNetworkClient spyNetworkClient = cloud.createSpyNetworkClient();

        when(cloud.mockGuiCallback.isHeadless()).thenReturn(true);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        verify(spyNetworkClient, never()).logIntoCloudForce();
    }

    public void testSoftSignInSucceed() throws Exception {
        final HttpURLConnection logInConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(logInConn);
        MockWebCloudClient cloud = createWebCloudClient(logInConn);
        WebCloudNetworkClient networkClient = mock(WebCloudNetworkClient.class);
        cloud.setNetworkClient(networkClient);

        when(networkClient.initialize()).thenReturn(true);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(Cloud.SigninState.SIGNED_IN, cloud.getSigninState());
        verify(networkClient, never()).logIntoCloudForce();
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testSoftSignInFailLoudly() throws Exception {
        final HttpURLConnection logInConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(logInConn);
        MockWebCloudClient cloud = createWebCloudClient(logInConn);
        WebCloudNetworkClient networkClient = mock(WebCloudNetworkClient.class);
        cloud.setNetworkClient(networkClient);

        when(networkClient.initialize()).thenThrow(new IOException());
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        try {
            cloud.initialize();
        } catch (IOException e) {
        }
        assertEquals(Cloud.SigninState.SIGNIN_FAILED, cloud.getSigninState());
        assertEquals(0, cloud.urlsRequested.size());
    }

    public void testSignInSignOutStateChangeEvents() throws IOException {
        // set up mocks
        final HttpURLConnection signInConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(signInConn);
        final HttpURLConnection signOutConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(signOutConn);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(signInConn, signOutConn);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        final List<String> states = new ArrayList<String>();
        cloud.addStatusListener(new Cloud.CloudStatusListener() {
            public void handleIssueDataDownloadedEvent() {
            }

            public void handleStateChange(Cloud.SigninState oldState, Cloud.SigninState state) {
                states.add(oldState.name());
                states.add(state.name());
            }
        });
        cloud.initialize();
        cloud.signIn();
        cloud.signOut();
        assertEquals(Arrays.asList("UNAUTHENTICATED", "SIGNING_IN", "SIGNING_IN", "SIGNED_IN", "SIGNED_IN", "SIGNED_OUT"), states);

        // verify
        assertEquals("/log-in", cloud.urlsRequested.get(0));
        assertEquals("/log-out/555", cloud.urlsRequested.get(1));
    }

    // ================================ authentication
    // =================================

    public void testSignInManually() throws IOException {
        // set up mocks
        final HttpURLConnection signInConn = mock(HttpURLConnection.class);
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(signInConn);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(signInConn);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        cloud.signIn();
        assertEquals(Cloud.SigninState.SIGNED_IN, cloud.getSigninState());

        // verify
        assertEquals("/log-in", cloud.urlsRequested.get(0));
        verify(signInConn).connect();
        LogIn logIn = LogIn.parseFrom(findIssuesOutput.toByteArray());
        assertEquals(555, logIn.getSessionId());
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testSignInManuallyFails() throws IOException {
        // set up mocks
        final HttpURLConnection signInConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(signInConn);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(signInConn);
        WebCloudNetworkClient spyNetworkClient = cloud.createSpyNetworkClient();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        try {
            cloud.signIn();
            fail();
        } catch (IOException e) {
        }
        assertEquals(Cloud.SigninState.SIGNIN_FAILED, cloud.getSigninState());

        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        Mockito.doThrow(new IllegalStateException()).when(spyNetworkClient).signIn(true);
        try {
            cloud.signIn();
            fail();
        } catch (IllegalStateException e) {
        }
        assertEquals(Cloud.SigninState.SIGNIN_FAILED, cloud.getSigninState());
    }

    public void testSignOut() throws IOException {
        // set up mocks
        final HttpURLConnection signInConn = mock(HttpURLConnection.class);
        ByteArrayOutputStream signInReq = setupResponseCodeAndOutputStream(signInConn);
        final HttpURLConnection signOutConn = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(signOutConn);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(signInConn, signOutConn);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        cloud.signIn();
        assertEquals(Cloud.SigninState.SIGNED_IN, cloud.getSigninState());
        cloud.signOut();
        assertEquals(Cloud.SigninState.SIGNED_OUT, cloud.getSigninState());

        // verify
        assertEquals("/log-in", cloud.urlsRequested.get(0));
        verify(signInConn).connect();
        LogIn logIn = LogIn.parseFrom(signInReq.toByteArray());
        assertEquals(555, logIn.getSessionId());
        // verify
        assertEquals("/log-out/555", cloud.urlsRequested.get(1));
        verify(signOutConn).connect();
    }
}
