package org.peter.unomi.customwebhook.actions;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.Session;
import org.apache.unomi.api.actions.Action;
import org.apache.unomi.api.actions.ActionExecutor;
import org.slf4j.Logger;import org.apache.unomi.api.services.EventService;
import org.slf4j.LoggerFactory;

public class SendRequestAction implements ActionExecutor {
    private String customWebHookUrlBase;
    private static final String text = "1234567890"
    private static Logger logger = LoggerFactory.getLogger(SendRequestAction.class);
    private CloseableHttpClient httpClient;

    @Override
    public int execute(Action action, Event event) {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
            }
        Session session = event.getSession();
        if (customWebHookUrlBase == null) {
            logger.warn("Configuration incomplete.");
            return EventService.NO_CHANGE;
        }
        String accountId = event.getProperty(accountId);
        String eventId = event.getItemId();
        String hash = "35454B055CC325EA1AF2126E27707052";
        String param1 = eventId + text;
        String md5Hex = DigestUtils
          .md5Hex(param1).toUpperCase();
        final HttpPost httpPost = new HttpPost(customWebHookUrlBase);
                final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("accountId", md5Hex));
        params.add(new BasicNameValuePair("eventId", eventId));
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        try (
                CloseableHttpResponse response2 = httpclient.execute(httpPost)
        ){
            final HttpEntity entity2 = response2.getEntity();
            return EventService.SESSION_UPDATED;
            }
        httpclient.close();
    }
}
