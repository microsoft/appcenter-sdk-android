package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

import java.net.URISyntaxException;
import java.util.Collections;

import static com.microsoft.appcenter.distribute.BrowserUtils.GOOGLE_CHROME_URL_SCHEME;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("WrongConstant")
public class BrowserUtilsTest {

    private static final String TEST_URL = "https://www.contoso.com?a=b";

    private static final ArgumentMatcher<Intent> CHROME_MATCHER = new ArgumentMatcher<Intent>() {

        @Override
        public boolean matches(Object o) {
            Intent intent = (Intent) o;
            return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + TEST_URL).equals(intent.getData());
        }
    };

    @Test
    public void init() {
        assertNotNull(new BrowserUtils());
    }

    @Test
    public void appendQueryToUriWithNoQuery() throws URISyntaxException {
        String url = "http://mock";
        url = BrowserUtils.appendUri(url, "x=y");
        assertEquals(url, "http://mock?x=y");
    }

    @Test
    public void appendQueryToUriWithExistingQuery() throws URISyntaxException {
        String url = "http://mock?a=b";
        url = BrowserUtils.appendUri(url, "x=y");
        assertEquals(url, "http://mock?a=b&x=y");
    }

    @Test
    public void chrome() throws Exception {
        Activity activity = mock(Activity.class);
        BrowserUtils.openBrowser(TEST_URL, activity);
        verify(activity).startActivity(argThat(CHROME_MATCHER));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void noBrowserFound() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        PackageManager packageManager = mock(PackageManager.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(any(Intent.class));
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        /* Open Chrome then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        verify(activity).startActivity(argThat(CHROME_MATCHER));

        /* Verify no more call to startActivity. */
        verify(activity).startActivity(any(Intent.class));
    }

    @Test
    public void onlySystemBrowserNoDefaultAsNull() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(CHROME_MATCHER));
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(null);
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "browser";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.singletonList(resolveInfo));
        }

        /* Open Chrome then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(CHROME_MATCHER));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void onlySystemBrowserNoDefaultAsPicker() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(CHROME_MATCHER));
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "picker";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        }
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "browser";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.singletonList(resolveInfo));
        }

        /* Open Chrome then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(CHROME_MATCHER));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void onlySystemBrowserAndIsDefault() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(BrowserUtilsTest.CHROME_MATCHER));
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "browser";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        }
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "browser";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.singletonList(resolveInfo));
        }

        /* Open Chrome then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(CHROME_MATCHER));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void twoBrowsersAndNoDefault() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(CHROME_MATCHER));
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "picker";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        }
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "browser";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            ActivityInfo activityInfo2 = new ActivityInfo();
            activityInfo2.packageName = "mozilla";
            activityInfo2.name = "firefox";
            ResolveInfo resolveInfo2 = new ResolveInfo();
            resolveInfo2.activityInfo = activityInfo;
            when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(asList(resolveInfo, resolveInfo2));
        }

        /* Open Chrome then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(CHROME_MATCHER));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void secondBrowserIsDefault() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(CHROME_MATCHER));
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "mozilla";
            activityInfo.name = "firefox";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        }
        {
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.packageName = "system";
            activityInfo.name = "browser";
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            ActivityInfo activityInfo2 = new ActivityInfo();
            activityInfo2.packageName = "mozilla";
            activityInfo2.name = "firefox";
            ResolveInfo resolveInfo2 = new ResolveInfo();
            resolveInfo2.activityInfo = activityInfo2;
            when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(asList(resolveInfo, resolveInfo2));
        }

        /* Open Chrome then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(CHROME_MATCHER));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent().getClassName().equals("firefox");
            }
        }));
        order.verifyNoMoreInteractions();
    }
}
