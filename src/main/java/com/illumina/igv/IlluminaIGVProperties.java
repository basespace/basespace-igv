package com.illumina.igv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.illumina.basespace.BaseSpaceConfiguration;


public class IlluminaIGVProperties extends Properties
{
    public static final String SYSTEM_NAMESPACE = "jnlp.illumina.igv.basespace.";
    public static final String VERSION = SYSTEM_NAMESPACE + "version";
    public static final String ROOT_URI = SYSTEM_NAMESPACE + "api.root.uri";
    public static final String ACCESS_TOKEN_FRAGMENT = SYSTEM_NAMESPACE + "access.token.fragment";
    public static final String AUTHORIZATION_FRAGMENT = SYSTEM_NAMESPACE + "authorization.fragment";
    public static final String AUTHORIZATION_SCOPE = SYSTEM_NAMESPACE + "authorization.scope";
    public static final String ACCESS_TOKEN = SYSTEM_NAMESPACE + "access.token";
    
    public static final String PROXY_HOST = SYSTEM_NAMESPACE + "sdk.proxy.host";
    public static final String PROXY_PORT= SYSTEM_NAMESPACE + "sdk.proxy.port";
    

    public static final String CLIENT_ID = SYSTEM_NAMESPACE + "client.id";
    public static final String CLIENT_SECRET = SYSTEM_NAMESPACE + "client.secret";
    
    public static final String PROJECT_ID = SYSTEM_NAMESPACE + "project";
    public static final String APPRESULT_ID = SYSTEM_NAMESPACE + "appresult";
    public static final String SAMPLE_ID = SYSTEM_NAMESPACE + "sample";
    public static final String FILE_ID = SYSTEM_NAMESPACE + "file";
    
    private static IlluminaIGVProperties singletonObject;
    public static synchronized IlluminaIGVProperties instance()
    {
        if (singletonObject == null)
            singletonObject = new IlluminaIGVProperties();
        return singletonObject;
    }
    
    public BaseSpaceConfiguration createConfig()
    {
        return new BaseSpaceConfiguration()
        {

            @Override
            public String getVersion()
            {
                return getRequired(VERSION);
            }

            @Override
            public String getClientId()
            {
                return getRequired(CLIENT_ID);
            }

            @Override
            public String getClientSecret()
            {
                return getRequired(CLIENT_SECRET);
            }

            @Override
            public String getApiRootUri()
            {
                return getRequired(ROOT_URI);
            }

            @Override
            public String getAccessTokenUriFragment()
            {
                return getRequired(ACCESS_TOKEN_FRAGMENT);
            }
            
            @Override
            public String getAuthorizationUriFragment()
            {
                return getRequired(AUTHORIZATION_FRAGMENT);
            }

            @Override
            public String getAuthorizationScope()
            {
                return getRequired(AUTHORIZATION_SCOPE);
            }

            @Override
            public String getProxyHost()
            {
                return (String) get(PROXY_HOST);
            }

            @Override
            public int getProxyPort()
            {
                return Integer.parseInt((String)get(PROXY_PORT));
            }

            @Override
            public String getAccessToken()
            {
               return (String)get(ACCESS_TOKEN);
            }
        };
    }
    
    private String getRequired(String key)
    {
        if (get(key) == null)throw new IllegalArgumentException("Missing required system property " +key); 
        return (String) get(key);
    }
    
    public Long[]getIdsForProperty(String propName)
    {
        if (IlluminaIGVProperties.instance().containsKey(propName))
        {
            List<Long>ids = new ArrayList<Long>();
            String val = (String) IlluminaIGVProperties.instance().get(propName);
            if (val.length() == 0)return new Long[0];
            for(String sId:val.split(","))
            {
                ids.add(Long.parseLong(sId));
            }
            return ids.toArray( new Long[ids.size()]);
        }
        return new Long[0];
        
    }
    
    private IlluminaIGVProperties()
    {
        Properties props = System.getProperties();
        for(Iterator<Object> it = props.keySet().iterator();it.hasNext();)
        {
            String paramName = it.next().toString();
            if (paramName.toLowerCase().startsWith(SYSTEM_NAMESPACE))
            {
                super.put(paramName, (String) props.get(paramName));
            }
        }
        if (super.get(ACCESS_TOKEN) == null)
        {
            getClientIdAndSecret();
        }
    }

    private void getClientIdAndSecret()
    {
        try
        {
            if (get(CLIENT_ID) == null || get(CLIENT_SECRET) == null)
            {
                throw new IllegalArgumentException("IGV is missing application data necessary to perform BaseSpace authenitication. Unable to proceed.");
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException("Error getting client id",t);
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(Iterator<Object> it = keySet().iterator();it.hasNext();)
        {
            if (!first)sb.append("\r\n");
            String paramName = it.next().toString();
            String value = getProperty(paramName);
            sb.append(paramName + "=" + value);
            first = false;
        }
        
        return sb.toString();
    }


}
