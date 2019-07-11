package ext.carrier.wc.datautility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import org.json.JSONObject;
import wt.doc.WTDocument;
import wt.enterprise._RevisionControlled;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.ReferenceFactory;
import wt.inf.team.ContainerTeamManaged;
import wt.log4j.LogR;
import wt.org.WTPrincipal;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTProperties;
import com.ptc.core.components.descriptor.LogicSeparatedDataUtility;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.AbstractDataUtility;
import com.ptc.core.components.rendering.GuiComponent;
import com.ptc.core.components.rendering.guicomponents.IconComponent;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.ui.resources.ComponentMode;
import ext.carrier.common.commonResource;
import ext.carrier.wc.cognos.CognosReportHelper;
import ext.carrier.wc.cognos.OpenViewableUtility;

/**
 * @author Adarsh Sambhav
 * Date:- 02-08-2012
 * Modified By-Akhila(ESR 35588)
 */
public class PDFLinkDataUtility extends AbstractDataUtility implements LogicSeparatedDataUtility{

	private static final org.apache.log4j.Logger logger = LogR.getLogger(PDFLinkDataUtility.class.getName());
	
	
    /**
     * Constructor
     */
    public PDFLinkDataUtility() {
        super();
    }

    /* (non-Javadoc)
     * @see com.ptc.core.components.descriptor.DataUtility#getDataValue(java.lang.String, java.lang.Object, com.ptc.core.components.descriptor.ModelContext)
     */
    @ Override
    public Object getDataValue(String component_id, Object datum, ModelContext mc) throws WTException {

    	boolean isUserRole = false;
    	boolean isState= false;
    	
        logger.debug("In  getDataValue method of PDF Veiewable Utility");
        GuiComponent gui = null;
        
        ReferenceFactory refFactory = new ReferenceFactory();
        Object selObj = refFactory.getReference((Persistable) datum).getObject();
        
        
        
        if( selObj instanceof WTPart)
        {
        	/*for WTPart*/
        	try
        	{ 
				  selObj = (WTPart)datum;
				  /*fetch the roles from Property file*/
				  isUserRole = getProperty(selObj,"User.Role");
				  /*fetch states from property file*/
				  isState = getProperty(selObj,"LifeCycle.State");
				  
        	}
        	catch(Exception z)
            {
           	 z.printStackTrace();
            }
       	}
        else if(selObj instanceof EPMDocument)
        {
        	/*for EPMDocument*/
			try
				{
					selObj= (EPMDocument) datum;
					/*fetch the roles from Property file*/
					isUserRole = getProperty(selObj,"User.Role");
					 /*fetch states from property file*/
					isState = getProperty(selObj,"LifeCycle.State");
					
				}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
         }
        else if(selObj instanceof WTDocument)
        {
        	/*for WTDocument*/
           try
			{
        	   selObj= (WTDocument) datum;
        	   /*fetch the roles from Property file*/
        	   isUserRole = getProperty(selObj,"User.Role");
        	   /*fetch states from property file*/
			   isState = getProperty(selObj,"LifeCycle.State");
			   
			}
           catch(Exception t)
			{
        	   t.printStackTrace();
			}
        	   
        }
        logger.debug("Selected Object :"+selObj);
        boolean pdfCheck=false;
        try {
			pdfCheck= OpenViewableUtility.checkViewableForPDF(selObj.toString());
			logger.debug("PDFCheck :"+pdfCheck);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
       
        WTProperties serverProperties = null;
        try {
			serverProperties = WTProperties.getServerProperties();
		} catch (java.io.IOException e) {		
			e.printStackTrace();
		}
        String serverBaseURL = serverProperties.getProperty("wt.server.codebase");       
        String detailImage = "/wt/clients/images/pdf.gif";
        Locale locale = SessionHelper.getLocale();
        
        if (mc.getDescriptorMode().equals(ComponentMode.VIEW)){
        	logger.debug("In side view model of PDFLinkDataUtility");
        	if(pdfCheck){               
                String pdfIconLink="";
                pdfIconLink=serverBaseURL;
                
                // Code is modified to remove unwanted exception of MS and servlet implementation instead of JSP.
                //pdfIconLink += "/netmarkets/jsp/ext/carrier/wc/part/openPDF.jsp?objectOID=OR:";                
                pdfIconLink += "/servlet/OpenPDFViewableServlet?objectOID=OR:";
                pdfIconLink +=selObj.toString();
                logger.debug("PDF Icon Link :"+pdfIconLink);
                String detailTip = WTMessage.getLocalizedMessage(commonResource.class.getName(), commonResource.PDF_ICON_MSG, null, locale);
                IconComponent detailIconDisplay = new IconComponent();
                detailIconDisplay.setSrc(serverBaseURL + detailImage);
                detailIconDisplay.setTooltip(detailTip);
               //Modifications for ESR 35588             
                if((isState)&&(isUserRole))
                {
					//Display the icon if the state is OBSOLETE or user is mapped to OBSERVERLITE||OBSERVER Role
                	gui = detailIconDisplay;
            		
                }else
        		{
                	//Display the icon and provide download option if the state is OBSOLETE or user is mapped to OBSERVERLITE||OBSERVER  Role
                	gui = detailIconDisplay;
                	detailIconDisplay.addJsAction("onClick", "window.open('" + pdfIconLink + "')");
        		}
             	}  
        	else
        	{  
        		TextDisplayComponent text = new TextDisplayComponent("String");
        		String msg=WTMessage.getLocalizedMessage(commonResource.class.getName(), commonResource.NO_PDF_ICON_MSG, null, locale);
        		text.setValue(msg);
        		gui=text;        		
        	}
        }
        return gui;
    }

	@Override
	public Object getPlainDataValue(String component_id, Object datum, ModelContext mc)
			throws WTException {
				
			logger.debug("In  getPlainDataValue() method of PDF Veiewable Utility");	        
	        
	        ReferenceFactory refFactory = new ReferenceFactory();
	        Object selObj = refFactory.getReference((Persistable) datum).getObject();
	        if( selObj instanceof WTPart){
	        	 selObj= (WTPart) datum;
	        }
	        else if(selObj instanceof EPMDocument){
	        	selObj= (EPMDocument) datum;
	        }
	        else if(selObj instanceof WTDocument){
	           
	        	selObj= (WTDocument) datum;
	        }
	        logger.debug("Selected Object :"+selObj);
	        boolean pdfCheck=false;
	        try {
				pdfCheck= OpenViewableUtility.checkViewableForPDF(selObj.toString());
				logger.debug("PDFCheck :"+pdfCheck);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
	       
	        WTProperties serverProperties = null;
	        try {
				serverProperties = WTProperties.getServerProperties();
			} catch (java.io.IOException e) {		
				e.printStackTrace();
			}
	        String serverBaseURL = serverProperties.getProperty("wt.server.codebase");       
	        String detailImage = "/wt/clients/images/pdf.gif";
	        Locale locale = SessionHelper.getLocale();
	        JSONObject icon_object = new JSONObject();
	       
	        logger.debug("In side view model of PDFLinkDataUtility");
        	if(pdfCheck){               
                String pdfIconLink="";
                pdfIconLink=serverBaseURL;
                
                // Code is modified to remove unwanted exception of MS and servlet implementation instead of JSP.
                //pdfIconLink += "/netmarkets/jsp/ext/carrier/wc/part/openPDF.jsp?objectOID=OR:";                
                pdfIconLink += "/servlet/OpenPDFViewableServlet?objectOID=OR:";
                pdfIconLink +=selObj.toString();
                logger.debug("PDF Icon Link :"+pdfIconLink);
                String detailTip = WTMessage.getLocalizedMessage(commonResource.class.getName(), commonResource.PDF_ICON_MSG, null, locale);	              
                
                try {	                 
                  icon_object.put("type", "icon");
                  icon_object.put("iconPath", serverBaseURL + detailImage);
                  icon_object.put("tooltip", detailTip);
                  icon_object.put("target", "_new");
                  icon_object.put("link", pdfIconLink);	                   
               	} catch (Exception e) {
                   throw new WTException(e);
               	}         	              
        		
        	}  
        	else{          		
        		String msg=WTMessage.getLocalizedMessage(commonResource.class.getName(), commonResource.NO_PDF_ICON_MSG, null, locale);
        		try {
					icon_object.put("type", "text");
					icon_object.put("text", msg);
				} catch (Exception e) {
					  throw new WTException(e);
				}
        	}   
        	 return icon_object.toJSONString();	
	}	
	
	
/* 
* Fetches the roles and compares if the user session exists in the OBSERVERLITE Role
* @param selObj -WTObject
* @return Boolean - str
* @throws WTException
* @author Akhila(ESR 35588)
*/

		@SuppressWarnings("deprecation")
		   public boolean getUserDetails(Object selObj,Properties prop)
			      throws IOException,WTException
			      {	      
			boolean isRole = false;
			
				  try
				    {
				      //Fetch the user who has logged in
				        WTPrincipal wtprincipal = SessionHelper.manager.getPrincipal();	
					  //Fetch the container
				        wt.inf.container.WTContainer cont = ((wt.inf.container.WTContained)selObj).getContainer();
					  //Fetch container team
				        wt.inf.team.ContainerTeam team2 = wt.inf.team.ContainerTeamHelper.service.getContainerTeam((ContainerTeamManaged) cont);
					  //Fetch team Roles
				        java.util.Enumeration teamrolese = team2.getRoles().elements();
				      //Fetch the values for roles from propert file
				        String roleobj1 =prop.getProperty("User.Role").trim();
				        String[] myrole=roleobj1.split(",");
				      //If multiple roles exist in properties 
				        ArrayList<String> displayList = new ArrayList<String>();
				        displayList.addAll(Arrays.asList(myrole));		        
				        while (teamrolese.hasMoreElements())
				        {
				        					 				        	
					     Object roleobj =teamrolese.nextElement();
				         wt.project.Role role = (wt.project.Role) roleobj;
				         Iterator<String> iter = displayList.iterator();
				         while (iter.hasNext())
				        	 {
				        	 
				  			 	String myrole1 =iter.next();
				  			 	if (role.toString().equalsIgnoreCase(myrole1))
				        			{
									//Check if the Role is OBSERVERLITE||OBSERVER
										java.util.Enumeration pprincipals = (team2.getPrincipalTarget(role));							
											while (pprincipals!=null&&pprincipals.hasMoreElements())
												{
												//Fetch all the users in the OBSERVERLITE|OBSERVER role
													wt.org.WTPrincipal principal = ((wt.org.WTPrincipalReference)pprincipals.nextElement()).getPrincipal();
														if((wtprincipal.getName()).equals(principal.getName()))
															{
															//check if the session user is mapped to OBSERVERLITE Role
																isRole = true;
															}//close if
														
												}//close while
										}//Close outer if
				        	 }//Close outer while
					}//Close try      
				    }   
				    catch (WTException g)
				      {
				        g.printStackTrace();
				      }
						
				  return isRole;
		  
				}//Close getUserDetails
		
		@SuppressWarnings("deprecation")
		   public boolean getProperty(Object selObj,String propValue )
			      throws IOException,WTException
			      {
			 File propertyFile;
			 Properties prop;
			 Boolean retValue=false;
			 WTProperties wtProperties = null;
			 
				try
				{
					wtProperties = WTProperties.getLocalProperties();	
					
				} catch (IOException e1) {

					e1.printStackTrace();
				}
				String wtHome = wtProperties.getProperty("wt.codebase.location");
				//Specify the property file and location
				propertyFile = new File(wtHome + File.separator + "ext"
						+ File.separator + "carrier" + File.separator + "wc"
						+ File.separator + "datautility" + File.separator
						+ "AccessControlRole.properties");
				prop = new Properties();
					try
					{
					prop.load(new FileInputStream(propertyFile));
					//check if the property value to be fetched is role
						if (propValue.equalsIgnoreCase("User.Role"))
							{
							//call getUser details
							retValue=getUserDetails(selObj,prop);	
							return retValue;
							}else
							{
						//fetch property value for state
						String propState=prop.getProperty("LifeCycle.State").trim();
						//check if the property value to be fetched is LifeCycle.State
						if((((_RevisionControlled) selObj).getLifeCycleState().toString()).equalsIgnoreCase(prop.getProperty("LifeCycle.State").trim()))
						{
							retValue = true;
						}
					}
					
				} //close try
					catch (FileNotFoundException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			
				return retValue;
				
			      }//close getProperty
		
		
}
