package org.piecemaker2.api;

/**
 *	Class ApiRequest
 *
 *	<p>
 *	This class does the actual asynchronous request handling.
 *	</p>
 *
 *	@version ##version## - ##build##
 *	@author florian@motionbank.org
 */

import java.util.*;
import java.io.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

public class ApiRequest implements Runnable
{
	public static boolean DEBUG = false;

	public final static int GET    = 0;
	public final static int POST   = 1;
	public final static int PUT    = 2;
	public final static int DELETE = 3;

	private final String methodTypes[] = {
		"GET", "POST", "PUT", "DELETE"
	};

	PieceMakerApi api;
	String url;
	int methodType;
	int requestType;
	HashMap<String,String> data;
	ApiCallback callBack;

	HttpMethodBase method = null;
	String serverResponse;

	/**
	 *	Constructor ApiRequest
	 */
	public ApiRequest ( PieceMakerApi api, String api_key, int requestType, String url, int methodType, HashMap<String,String> data, ApiCallback callBack )
	{
		this.api = api;
		this.url = url + ".json";
		this.requestType = requestType;
		this.methodType = methodType >= 0 && methodType <= 3 ? methodType : GET;
		
		this.data = new HashMap();
		this.data.put( "token", api_key );

		if ( data != null ) {
			java.util.Iterator iter = data.entrySet().iterator();
			while ( iter.hasNext() ) {
				Map.Entry<String, String> pairs = (Map.Entry<String, String>)iter.next();
				this.data.put( pairs.getKey(), pairs.getValue() );
			}
		}

		this.callBack = callBack;

		if ( DEBUG ) System.out.println( methodTypes[this.methodType] + " " + this.url );
	}

	/**
	 *	@see java.lang.Runnable#run()
	 */
	public void run ()
	{
		HttpClient client = new HttpClient();

		// construct the data object for the request

		NameValuePair[] requestData = null;

		if ( data != null && data.size() > 0 )
		{
			requestData = new NameValuePair[data.size()];
			int i = 0;
			for ( Map.Entry<String,String> e : data.entrySet() )
			{
				requestData[i] = new NameValuePair( e.getKey(), e.getValue() );
				i++;
			}
		}

		// make the method depending on type
		
		method = null;

		if ( methodType == GET )
		{
			GetMethod getMethod = new GetMethod( url );
			if ( requestData != null )
				getMethod.setQueryString( requestData );
			method = getMethod;
		}
		else if ( methodType == POST )
		{
			PostMethod postMethod = new PostMethod( url );
			if ( requestData != null )
				postMethod.setRequestBody( requestData );
			method = postMethod;
		}
		else if ( methodType == PUT )
		{
			PutMethod putMethod = new PutMethod( url );
			if ( requestData != null )
			{
				GetMethod gm = new GetMethod("");
				gm.setQueryString( requestData );
				
				try {
					putMethod.setRequestEntity( 
						new StringRequestEntity(
							gm.getQueryString(), "application/x-www-form-urlencoded", "utf-8"
						)
					);
				} catch ( Exception e ) {
					e.printStackTrace();
				}

				//System.out.println( gm.getQueryString() );
			}
			method = putMethod;
		}
		else if ( methodType == DELETE )
		{
			DeleteMethod deleteMethod = new DeleteMethod( url );
			if ( requestData != null )
				deleteMethod.setQueryString( requestData );
			method = deleteMethod;
		}

		// set the header to receive JSON data

		method.addRequestHeader( new Header( "Accept", "application/json, text/javascript" ) );

		// request it ...

		try 
	    {
	        int statusCode = client.executeMethod(method);

	        switch ( statusCode ) 
	        {
	        case HttpStatus.SC_OK:
	            break;
	        	// TODO: implement better HTTP error handling here, redirect, moved, 404, 403, ... 
	        default:
	            if (DEBUG) System.err.println( "Method failed: " + method.getStatusLine() );
	            method.releaseConnection();
	            api.handleError( method.getStatusLine().getStatusCode(), method.getStatusLine().getReasonPhrase(), this, method );
	            return;
	        }

	        // byte[] responseBody = method.getResponseBody();
	        // serverResponse = new String( responseBody );

	        BufferedReader bufferedReader = new BufferedReader( 
	        	new InputStreamReader( 
	        		method.getResponseBodyAsStream() 
	        	) 
	        );
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;

			while ( (line = bufferedReader.readLine()) != null ) 
			{
				stringBuilder.append(line + "\n");
			}

			bufferedReader.close();
			serverResponse = stringBuilder.toString();
	    }
	    catch ( HttpException e ) 
	    {
	        System.err.println( "Fatal protocol violation: " + e.getMessage() );
	        e.printStackTrace();
	    } 
	    catch ( IOException e ) 
	    {
	        System.err.println( "Fatal transport error: " + e.getMessage() );
	        e.printStackTrace();
	    } 
	    finally
	    {
	        method.releaseConnection();
	    }

	    // let API handle it

	    if ( serverResponse != null && serverResponse.length() > 0 ) {
	    	api.handleResponse( this );
	    }
	    else
	    {
	    	System.out.println( "No response." );
	    }
	}

	/**
	 *	getter getCallback()
	 */
	public ApiCallback getCallback ()
	{
		return callBack;
	}

	/**
	 *	getter getCallback()
	 */
	public String getResponse ()
	{
		return serverResponse;
	}

	/**
	 *	getter getCallback()
	 */
	public int getType ()
	{
		return requestType;
	}

	/**
	 *	getter getCallback()
	 */
	public String getTypeString ()
	{
		return methodTypes[methodType];
	}

	/**
	 *	getter getCallback()
	 */
	public String getURL ()
	{
		return url;
	}

	/**
	 *	@see java.lang.Object#toString()
	 */
	public String toString ()
	{
		return String.format( "<ApiRequest #%s %s>", hashCode(), callBack.toString() );
	}
}