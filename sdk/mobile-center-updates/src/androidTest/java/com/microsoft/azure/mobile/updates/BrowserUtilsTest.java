package com.microsoft.azure.mobile.updates;

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

import java.util.Collections;

import static com.microsoft.azure.mobile.updates.BrowserUtils.GOOGLE_CHROME_URL_SCHEME;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void coverage() {
        assertNotNull(new BrowserUtils());
    }

    @Test
    public void chrome() throws Exception {
        Activity activity = mock(Activity.class);
        final String url = "https://www.contoso.com?a=b";
        BrowserUtils.openBrowser(url, activity);
        verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        }));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void noBrowser() throws Exception {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        PackageManager packageManager = mock(PackageManager.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(any(Intent.class));
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        /* Open Chrome then abort. */
        final String url = "https://www.contoso.com?a=b";
        BrowserUtils.openBrowser(url, activity);
        verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        }));

        /* Verify no more call to startActivity. */
        verify(activity).startActivity(any(Intent.class));
    }

    @Test
    public void onlySystemBrowserNoDefaultAsNull() throws Exception {

        /* Mock no browser. */
        final String url = "https://www.contoso.com?a=b";
        Activity activity = mock(Activity.class);
        ArgumentMatcher<Intent> chromeMatcher = new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        };
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(chromeMatcher));
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
        BrowserUtils.openBrowser(url, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(chromeMatcher));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(url).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void onlySystemBrowserNoDefaultAsPicker() throws Exception {

        /* Mock no browser. */
        final String url = "https://www.contoso.com?a=b";
        Activity activity = mock(Activity.class);
        ArgumentMatcher<Intent> chromeMatcher = new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        };
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(chromeMatcher));
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
        BrowserUtils.openBrowser(url, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(chromeMatcher));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(url).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void onlySystemBrowserAndIsDefault() throws Exception {

        /* Mock no browser. */
        final String url = "https://www.contoso.com?a=b";
        Activity activity = mock(Activity.class);
        ArgumentMatcher<Intent> chromeMatcher = new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        };
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(chromeMatcher));
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
        BrowserUtils.openBrowser(url, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(chromeMatcher));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(url).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void twoBrowsersAndNoDefault() throws Exception {

        /* Mock no browser. */
        final String url = "https://www.contoso.com?a=b";
        Activity activity = mock(Activity.class);
        ArgumentMatcher<Intent> chromeMatcher = new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        };
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(chromeMatcher));
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
        BrowserUtils.openBrowser(url, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(chromeMatcher));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(url).equals(intent.getData()) && intent.getComponent().getClassName().equals("browser");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void secondBrowserIsDefault() throws Exception {

        /* Mock no browser. */
        final String url = "https://www.contoso.com?a=b";
        Activity activity = mock(Activity.class);
        ArgumentMatcher<Intent> chromeMatcher = new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(GOOGLE_CHROME_URL_SCHEME + url).equals(intent.getData());
            }
        };
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(argThat(chromeMatcher));
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
        BrowserUtils.openBrowser(url, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(chromeMatcher));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(url).equals(intent.getData()) && intent.getComponent().getClassName().equals("firefox");
            }
        }));
        order.verifyNoMoreInteractions();
    }
}
