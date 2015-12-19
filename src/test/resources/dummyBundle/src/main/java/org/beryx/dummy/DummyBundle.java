package org.beryx.dummy;

import org.apache.commons.lang3.event.EventUtils;
import org.apache.commons.lang3.text.FormatFactory;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.derby.catalog.Statistics;
import org.apache.http.auth.AuthState;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONWriter;

public class DummyBundle /*implements ViewrekaBundle*/ {
    public void printInfo() {
        System.out.println("dummy = " + DummyBundle.class.getName());
        System.out.println("JSONWriter = " + JSONWriter.class.getName());
        System.out.println("RequestConfig = " + RequestConfig.class.getName());
        System.out.println("AuthState = " + AuthState.class.getName());
        System.out.println("HttpClient = " + HttpClient.class.getName());
        System.out.println("HttpGet = " + HttpGet.class.getName());
        System.out.println("EventUtils = " + EventUtils.class.getName());
        System.out.println("FormatFactory = " + FormatFactory.class.getName());
        System.out.println("EntityArrays = " + EntityArrays.class.getName());
        System.out.println("Statistics = " + Statistics.class.getName());
    }
}
