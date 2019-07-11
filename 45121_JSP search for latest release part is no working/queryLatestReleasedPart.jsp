<%@page import="java.util.StringTokenizer"%>
<%@page import="wt.util.WTProperties"%>
<%@page import="wt.services.applicationcontext.implementation.ServiceProperties"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileInputStream"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.ptc.com/infoengine/taglib/core" prefix="ie"%>



<ie:getService varName="ie"/>

<%!

// initialize variables
static boolean DEBUG = true;
static String strIEInstance = "";
static String strWebapp = "";

// couldn't get this jsp to read from the properties so adding values here
static String prtLifecycleStates = "";
static String strServletPath = "";

static
{ // begin static method

	try
	{ // begin try, getting system properties

		WTProperties wtProps=wt.util.WTProperties.getLocalProperties();
		strIEInstance=wtProps.getProperty("wt.federation.ie.VMName");
		strWebapp=wtProps.getProperty("wt.server.codebase");

		java.util.Properties properties = new java.util.Properties();
		//ESR 45121
		String filePath=WTProperties.getServerProperties().getProperty("wt.home")+File.separator+"codebase"+File.separator+"ext"+File.separator+"carrier"+File.separator+"wc"+File.separator+"workflow"+File.separator+"CCD"+File.separator+"CcdPDFViewable.properties";
		
		properties.load(new FileInputStream(filePath));

		strServletPath = properties.getProperty("queryPart.servletPath");
		strWebapp = strWebapp + strServletPath;
		prtLifecycleStates = properties.getProperty("queryPart.ReleasedPrtLifecycleStates");

	} // end try, getting system properties
	catch(Exception e)
	{
		throw new RuntimeException(e);
	}

} // end static method
%>

<%
try
{ // begin try getting latest released WTPart

	if(DEBUG) System.out.println("------------------- queryLatestReleasedPart: START ------------------- ");
	if(DEBUG) System.out.println("The IE instance is: " + strIEInstance);
	if(DEBUG) System.out.println("The Webapp instance is: " + strWebapp);
	if(DEBUG) System.out.println("The prtLifecycleStates are: " + prtLifecycleStates);
	if(DEBUG) System.out.println("The the servlet path is: " + strServletPath);

	// initialize variables
	String strPartNumber  = request.getParameter("number");							// set the part number
	String strWhere   = "";															// initialize where clause to empty string
	String strLCWhere   = "";														// initialize lifecycle where clause to empty string
	String strVersionType = "LATEST";												// set version of interest to latest
	String strIterationType = "LATEST";												// set iteration of interest to latest
	String strObid = "";															// initialize obid to empty string
	String strURL = "";																// initialize url to empty string
	StringTokenizer prtLifecycle = new StringTokenizer(prtLifecycleStates, ",");	// tokenize the epm lifecycle states from properties
	
	if((strPartNumber != null) && (!strPartNumber.equals("")))
	{ // begin if, the passed part number is not null

		// set the where clause
		strWhere = "(number='" + strPartNumber +"')";

		if(DEBUG) System.out.println("PartNumber: " + strPartNumber);
		if(DEBUG) System.out.println("The where clause is: " + strWhere);

    	// define the where clause for searching for the WTPart
		int i = 1;
		int j = prtLifecycle.countTokens()-1;
		strLCWhere = "";

		if(DEBUG) System.out.println("The number of WTPart Lifecycle tokens is: " + prtLifecycle.countTokens());

		while(prtLifecycle.hasMoreTokens())
		{ // begin while, there are more WTPart lifecycle states

			strLCWhere = strLCWhere + "(state.state = '" + prtLifecycle.nextToken() + "')";

			if(i <= j)
			{ // begin if, i is less than the number of WTPart lifecycle states

				strLCWhere = strLCWhere + " | ";

			} // begin if, i is less than the number of WTPart lifecycle states

			if(DEBUG) System.out.println("Process number " + i + " of WTPart lifecycle states. Where clause is: " + strLCWhere);

			i++;

		} // end while, there are more WTPart lifecycle states

		// finalize the where string with ( at the beginning ) at the end
		strLCWhere = "(" + strLCWhere + ")";

		if(DEBUG) System.out.println("The WTPart lifecycle where clause equals: " + strLCWhere); 
    		
%>

		<ie:webject name="Search-Objects" 	type="OBJ">
			<ie:param name="INSTANCE"  	data="<%=strIEInstance%>"/>
			<ie:param name="TYPE" 		data="wt.part.WTPart"/>		
			<ie:param name="WHERE" 		data="<%=strWhere%>"/>
			<ie:param name="WHERE" 		data="<%=strLCWhere%>"/>
			<ie:param name="ATTRIBUTE" 	data="obid"/>
			<ie:param name="VERSION"	data="<%=strVersionType%>"/>
			<ie:param name="ITERATION"	data="<%=strIterationType%>"/>
			<ie:param name="GROUP_OUT" 	data="obj"/>
		</ie:webject>

<%

		// get the part's obid
		strObid = ie.getAttributeValue("obj",0,"obid");

		
		// if obid is long format, we need to cut off rest in Windchill 9, otherwise Properties Panel is not displayed.
		if((strObid != null) && (strObid.lastIndexOf("@") > -1))
		{ // begin if, the obid is not null the position of the last index of @ is not -1


			strObid = strObid.substring(0, strObid.lastIndexOf(":"));
			strURL = strWebapp + strObid;
%>
		   <!-- Redirect to properties page of a part  --> 
	       <c:redirect url="<%=strURL%>"/>
<%	    
			if(DEBUG)
			{
				System.out.println("searchpart: objects found:");
				System.out.println("OID Of part "+strObid);
				System.out.println("URL OF Part details page "+strURL);
			}

		} // end if, the obid is not null the position of the last index of @ is not -1
		else
		{ // begin else, the oid of a part is null, forward to error page. 
%>	
		     <jsp:forward page="/netmarkets/jsp/ext/carrier/wc/querypart/errorPage.jsp">
		     <jsp:param name="ErrorMessage" value="Part not Found or You may not have read access to the object."/> 
             </jsp:forward>

<%	    } // end else, the oid of a part is null, forward to error page.
		
	} // end if, the passed part number is not null
	else
	{ // begin else, number is not provided forward to error page. 
	
%>      
		 <jsp:forward page="/netmarkets/jsp/ext/carrier/wc/querypart/errorPage.jsp">
		 <jsp:param name="ErrorMessage" value="Please provide a part number."/> 
         </jsp:forward>
		 
<%
	} // end else, number is not provided forward to error page.

} // end try getting latest released WTPart
catch(Exception e)
{ // begin catch, In case of any exception forward to error page
System.out.println(" Exception error ***********:"+ e);
%>
      
		<jsp:forward page="/netmarkets/jsp/ext/carrier/wc/querypart/errorPage.jsp" >
		<jsp:param name="ErrorMessage" value="Internal Error: Please contact Administrator."/> 
        </jsp:forward>
<%
} // end catch, In case of any exception forward to error page

%>

