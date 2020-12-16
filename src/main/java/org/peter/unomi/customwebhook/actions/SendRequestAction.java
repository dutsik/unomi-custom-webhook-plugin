package org.peter.unomi.customwebhook.actions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.Session;
import org.apache.unomi.api.actions.Action;
import org.apache.unomi.api.actions.ActionExecutor;
import org.apache.unomi.api.services.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendRequestAction implements ActionExecutor {
    private String customWebhookUrlBase;
    private String unomiElasticSearchIndexPreafix;
    private String webhookName;
    private String url;
    private static final String text = "1234567890";
    private static Logger logger = LoggerFactory.getLogger(SendRequestAction.class);
    private CloseableHttpClient httpClient;

    @Override
    public int execute(Action action, Event event) {
        if (httpClient == null) {
            int timeout = 5;
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        }
        Session session = event.getSession();
        if (customWebhookUrlBase == null) {
            logger.warn("customWebhookUrlBase is empty.");
            return EventService.NO_CHANGE;
        }
        webhookName = action.getParameterValues().get("webhookName").toString();
        if (webhookName == null) {
            webhookName = "default";
            logger.warn("webhookName is empty. Using default webhook");
        }
        if (!customWebhookUrlBase.endsWith("/")) {
            customWebhookUrlBase += "/";
        }
        String propertyName = action.getParameterValues().get("propertyName").toString();
        String propertyValue = event.getProperty(propertyName).toString();
        String eventId = event.getItemId();
        String hash = "35454B055CC325EA1AF2126E27707052";
        String md5Hex = DigestUtils.md5Hex(eventId).toUpperCase();
        String param1 = md5Hex + text;
        url = customWebhookUrlBase + webhookName;
        final HttpPost httpPost = new HttpPost(url);
        final List<NameValuePair> params = new ArrayList<>();
        if (propertyValue != null) {
            params.add(new BasicNameValuePair(propertyName, propertyValue));
        } else {
            logger.warn("accountId is empty");
        }
        params.add(new BasicNameValuePair("eventId", param1));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported encoding.", e);
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                logger.error("Error with the API response.");
                return EventService.NO_CHANGE;
            }
        } catch (ConnectTimeoutException e) {
            logger.error("Error with the Http Request execution. Response timed out.", e);
        } catch (IOException e) {
            logger.error("Error with the Http Request execution. Wrong parameters given", e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        return EventService.SESSION_UPDATED;
    }
}
