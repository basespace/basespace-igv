package com.illumina.basespace.igv;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.illumina.basespace.ApiConfiguration;

/**
 * 
 * @author bking
 *
 */
public class BaseSpaceConfiguration extends Properties
{
    public static final String SYSTEM_NAMESPACE = "illumina.igv.basespace.";
    public static final String VERSION = SYSTEM_NAMESPACE + "version";
    public static final String ROOT_URI = SYSTEM_NAMESPACE + "api.root.uri";
    public static final String ACCESS_TOKEN_FRAGMENT = SYSTEM_NAMESPACE + "access.token.fragment";
    public static final String AUTHORIZATION_FRAGMENT = SYSTEM_NAMESPACE + "authorization.fragment";
    public static final String AUTHORIZATION_SCOPE = SYSTEM_NAMESPACE + "authorization.scope";
    public static final String ACCESS_TOKEN = SYSTEM_NAMESPACE + "access.token";
    public static final String PROJECT_ID = SYSTEM_NAMESPACE + "project";
    public static final String APPRESULT_ID = SYSTEM_NAMESPACE + "appresult";
    public static final String APPSESSION_ID = SYSTEM_NAMESPACE + "appsession";
    public static final String SAMPLE_ID = SYSTEM_NAMESPACE + "sample";
    public static final String FILE_ID = SYSTEM_NAMESPACE + "file";
    public static final String DEBUG = SYSTEM_NAMESPACE + "debug";
    public static final String PROXY_HOST = SYSTEM_NAMESPACE + "sdk.proxy.host";
    public static final String PROXY_PORT= SYSTEM_NAMESPACE + "sdk.proxy.port";
    
    private BaseSpaceConfiguration()
    {
        
    }
    
    public static String[]extractBaseSpaceArgs(String[] args)
    {
        List<String>baseSpaceArgs = new ArrayList<String>();
        for(String arg:args)
        {
            if (arg.indexOf(SYSTEM_NAMESPACE) > -1)
            {
                baseSpaceArgs.add(arg);
            }
        }
        return baseSpaceArgs.toArray(new String[baseSpaceArgs.size()]);
    }
    
    public static String[]removeBaseSpaceArgs(String[] args)
    {
        List<String>rtn = new ArrayList<String>();
        for(String arg:args)
        {
            if (arg.indexOf(SYSTEM_NAMESPACE) > -1)
            {
                continue;
            }
            rtn.add(arg);
        }
        return rtn.toArray(new String[rtn.size()]);
    }
    
    public static BaseSpaceConfiguration create(String[] args)
    {
        BaseSpaceConfiguration rtn = new BaseSpaceConfiguration();
        for(String arg:args)
        {
            String[]parts = arg.split("=");
            rtn.put(parts[0], parts[1]);
        }
        return rtn;
    }
    
    public String getProjectId()
    {
        return getProperty(PROJECT_ID);
    }
    

    public String getAppResultId()
    {
        return getProperty(APPRESULT_ID);
    }
    
    public String getAppSessionId()
    {
        return getProperty(APPSESSION_ID);
    }
    
    @Override
    public String getProperty(String key)
    {
        String val = super.getProperty(key);
        if (val == null || val.equalsIgnoreCase("-1"))return null;
        return val;
    }

    public ApiConfiguration getApiConfiguration()
    {   
        return new ApiConfiguration()
        {

            @Override
            public String getVersion()
            {
                return getValue(VERSION,true);
            }

            @Override
            public String getApiRootUri()
            {
                return getValue(ROOT_URI,true);
            }

            @Override
            public String getAccessTokenUriFragment()
            {
                return getValue(ACCESS_TOKEN_FRAGMENT,true);
            }
            
            @Override
            public String getAuthorizationUriFragment()
            {
                return getValue(AUTHORIZATION_FRAGMENT,true);
            }

            @Override
            public String getAuthorizationScope()
            {
                return getValue(AUTHORIZATION_SCOPE,true);
            }

            @Override
            public String getProxyHost()
            {
                return null;
            }

            @Override
            public int getProxyPort()
            {
                return 0;
            }

            @Override
            public String getAccessToken()
            {
                return getValue(ACCESS_TOKEN,true);
            }

            @Override
            public int getReadTimeout()
            {
                return 0;
            }

            @Override
            public int getConnectionTimeout()
            {
                return 0;
            }

            @Override
            public String getClientId()
            {
                return null;
            }

            @Override
            public String getClientSecret()
            {
                return null;
            }

            @Override
            public String getStoreRootUri()
            {
                return null;
            }
        };
    }
    
    private String getValue(String key,boolean required)
    {
        if (get(key) == null && required)throw new IllegalArgumentException("Missing application argument " + key); 
        return (String)get(key);
    }
    
    
}
