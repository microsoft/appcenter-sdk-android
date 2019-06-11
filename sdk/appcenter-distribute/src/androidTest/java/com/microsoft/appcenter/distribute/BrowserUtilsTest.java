/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("WrongConstant")
public class BrowserUtilsTest {

    private static final String TEST_URL = "https://www.contoso.com?a=b";

    private final ArgumentMatcher<Intent> mBrowserArgumentMatcher = new ArgumentMatcher<Intent>() {

        @Override
        public boolean matches(Object o) {
            Intent intent = (Intent) o;
            return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent() != null && intent.getComponent().getClassName().equals("browser");
        }
    };

    @Test
    public void init() {
        new BrowserUtils();
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
    public void noBrowserFound() {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
        PackageManager packageManager = mock(PackageManager.class);
        doThrow(new ActivityNotFoundException()).when(activity).startActivity(any(Intent.class));
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        /* Open Browser then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);

        /* Verify no more call to startActivity. */
        verify(activity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void onlySystemBrowserNoDefaultAsNull() {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
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

        /* Open browser then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(mBrowserArgumentMatcher));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void onlySystemBrowserNoDefaultAsPicker() {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
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

        /* Open browser then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(mBrowserArgumentMatcher));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void onlySystemBrowserAndIsDefault() {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
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

        /* Open browser then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(mBrowserArgumentMatcher));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void twoBrowsersAndNoDefault() {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
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

        /* Open browser then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(mBrowserArgumentMatcher));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void secondBrowserIsDefault() {

        /* Mock no browser. */
        Activity activity = mock(Activity.class);
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

        /* Open browser then abort. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent() != null && intent.getComponent().getClassName().equals("firefox");
            }
        }));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void openExplicitBrowserFails() {
        Activity activity = mock(Activity.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "system";
        activityInfo.name = "browser";
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.singletonList(resolveInfo));
        doThrow(new SecurityException()).doNothing().when(activity).startActivity(notNull(Intent.class));

        /* Open browser with explicit intent then fall back to implicit intent. */
        BrowserUtils.openBrowser(TEST_URL, activity);
        InOrder order = inOrder(activity);
        order.verify(activity).startActivity(argThat(mBrowserArgumentMatcher));
        order.verify(activity).startActivity(argThat(new ArgumentMatcher<Intent>() {

            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return Intent.ACTION_VIEW.equals(intent.getAction()) && Uri.parse(TEST_URL).equals(intent.getData()) && intent.getComponent() == null;
            }
        }));
        order.verifyNoMoreInteractions();
    }
}
