package com.microsoft.sonoma.core.ingestion.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

interface UrlConnectionFactory {

    HttpURLConnection openConnection(URL url) throws IOException;
}
