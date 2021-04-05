package org.peter.unomi.customwebhook.actions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
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
import org.apache.unomi.api.actions.Action;
import org.apache.unomi.api.actions.ActionExecutor;
import org.apache.unomi.api.services.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendRequestAction implements ActionExecutor {
    private String customWebhookUrlBase = null;
    private String hash = null;
    private static final Logger logger = LoggerFactory.getLogger(SendRequestAction.class);
    private CloseableHttpClient httpClient;
    private String unomiElasticSearchIndexPrefix = null;

    @Override
    public int execute(Action action, Event event) {

        logger.info("Start webhook action.");
        if (httpClient == null) {
            // TODO can be configurable depends on behavior of the request.
            int timeout = 10;
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        }

        if (this.customWebhookUrlBase == null) {
            logger.error("customWebhookUrlBase is empty.");
            return EventService.NO_CHANGE;
        }
        if (this.hash == null) {
            logger.error("hash is empty.");
            return EventService.NO_CHANGE;
        }
        Object webhookNameProperty = action.getParameterValues().get("webhookName");
        String webhookName;

        if (webhookNameProperty == null) {
            webhookName = "default";
            logger.warn("webhookName is empty. Using default webhook");
        } else {
            webhookName = webhookNameProperty.toString();
        }

        if (!this.customWebhookUrlBase.endsWith("/")) {
            this.customWebhookUrlBase += "/";
        }

        Object propertyName = action.getParameterValues().get("propertyName");
        Object propertyValue = action.getParameterValues().get("propertyValue");

        String eventId = event.getItemId();

        String md5Hex = DigestUtils.md5Hex(eventId + hash).toUpperCase();

        String url = this.customWebhookUrlBase + webhookName;

        final HttpPost httpPost = new HttpPost(url);

        final List<NameValuePair> params = new ArrayList<>();
        if (propertyValue != null && propertyName != null) {
            params.add(new BasicNameValuePair(propertyName.toString(), propertyValue.toString()));
        } else {
            logger.warn("Custom Property is empty");
        }

        if (this.unomiElasticSearchIndexPrefix != null) {
            params.add(new BasicNameValuePair("prefix", this.unomiElasticSearchIndexPrefix));
        }

        params.add(new BasicNameValuePair("eventId", eventId));
        params.add(new BasicNameValuePair("checkSum", md5Hex));
        if (logger.isDebugEnabled()) {
            logger.debug("webhook Url {}", url);
            logger.debug("Property Name {}", action.getParameterValues().get("propertyName"));
            logger.debug("Property Value {}", action.getParameterValues().get("propertyValue"));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported encoding.", e);
            return EventService.NO_CHANGE;
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                logger.error("Error with the API response with Status Code = {}", statusCode);
                return EventService.NO_CHANGE;
            }
        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            logger.error("Error with the Http Request execution. Response timed out.", e);
            return EventService.NO_CHANGE;
        } catch (IOException e) {
            logger.error("Error with the Http Request execution. ", e);
            return EventService.NO_CHANGE;
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        return EventService.NO_CHANGE;
    }
    public void setCustomWebhookUrlBase(String customWebhookUrlBase) {
        this.customWebhookUrlBase = customWebhookUrlBase;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
    public void setUnomiElasticSearchIndexPrefix(String unomiElasticSearchIndexPrefix) {
        this.unomiElasticSearchIndexPrefix = unomiElasticSearchIndexPrefix;
    }

}
