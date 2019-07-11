
<%@page import="java.util.Enumeration"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.LinkedHashSet"%>
<%@page import="java.util.Locale"%>

<%@page import="com.ptc.core.components.rendering.guicomponents.TextBox"%>
<%@page import="com.ptc.windchill.enterprise.report.Report"%>
<%@page import="wt.inf.team.ContainerTeamManagedState"%>
<%@page import="wt.fc.ReferenceFactory"%>
<%@page import="wt.session.SessionHelper"%>
<%@page import="wt.util.WTMessage"%>
<%@page import="wt.util.WTRuntimeException"%>
<%@page import="ext.carrier.wc.cognos.CognosReportRB"%>
<%@page import="ext.carrier.wc.cognos.service.CognosReportsService"%>

<%@taglib prefix="wctags" tagdir="/WEB-INF/tags"%>
<%@taglib uri="http://www.ptc.com/windchill/taglib/wrappers" prefix="w"%>

<!-- Edited to add a CADModel Section ESR 41652 -->


<%
         String RESOURCE = "ext.carrier.wc.cognos.CognosReportRB";
         Locale locale = request.getLocale();
		
		String reportOid = request.getParameter("oid");
         if (reportOid == null || reportOid.trim().length() == 0) {
            throw new WTRuntimeException("Report object identifier (oid) missing from the request. Report generation can not proceed.");
         }
         Report report = (Report) new ReferenceFactory().getReference(reportOid).getObject();
         if (report == null) {
            throw new WTRuntimeException("Unable to construct a Report object from object identifier: " + reportOid);
         }
        
         String inputParametersTitleLabel = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.INPUT_PARAMETERS_TITLE_LABEL, null, locale);
         String reportLabel = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.REPORT_NAME, null, locale);
         String ecnNumberLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.ECNLABLE_NAME, null, locale);
		 String obsoltePartsLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.OBSOLETE_PART_SECTION, null, locale);
		 String newPartsLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.NEW_PART_SECTION, null, locale);
		 String revsiedPartsLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.REVISED_PART_SECTION, null, locale);
		 String revisedDrawingsLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.REVISED_DRAWING_SECTION, null, locale);
		 String CADModelLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.CAD_Model, null, locale);
		 String bomLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.BOM_PART_SECTION, null, locale);
		 String allLbl = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.ALL_PART_SECTION, null, locale);
         String generateLabel = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.GENERATE_LABEL, null, locale);
         String cancelLabel = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.CANCEL_BUTTON_LABEL, null, locale);
         String clearLabel = WTMessage.getLocalizedMessage(RESOURCE, CognosReportRB.CLEAR_BUTTON_LABEL, null, locale);
		 // forward the request to report generation if renderCustomPage request param is set to true
		 String renderInputForm = ((String) request.getParameter("renderCustomPage"));
         System.out.println("renderInputForm-->>"+renderInputForm);
		 if ("false".equals(renderInputForm)) 
		 {
%>
		 <jsp:forward page="/wtcore/jsp/report/reportGenForm.jsp" />
<%
		 }
%>


<%@include file="/netmarkets/jsp/util/beginPopup.jspf"%>

<!-- Conditionally render this page, if all the inputs are provided and valid, we will set this to true to forward the
request to report generation. One can not POST the form instead to the report generation jsp, hence this mechanism is used. -->






<script language='JavaScript' type="text/javascript"
	src='netmarkets/javascript/pjlreporting/common.js'></script>
<%
  String ECNNumber = request.getParameter("ECNNumber") == null ? "" : request.getParameter("ECNNumber");
  String Obsolete= request.getParameter("Obsolete") == null ? "" : request.getParameter("Obsolete");
  String NewPart=request.getParameter("NewPart") == null ? "" : request.getParameter("NewPart");
  String RevisedPart=request.getParameter("RevisedPart") == null ? "" : request.getParameter("RevisedPart");
  String RevisedDrawing=request.getParameter("RevisedDrawing") == null ? "" : request.getParameter("RevisedDrawing");
  String CADModel=request.getParameter("CADModel") == null ? "" : request.getParameter("CADModel");
  String BOM=request.getParameter("BOM") == null ? "" : request.getParameter("BOM");
  
%>

<input type="hidden" id="ECNNumber" name="ECNNumber" value="<%=ECNNumber%>" />
<input type="hidden" id="Obsolete" name="Obsolete" value="<%=Obsolete%>" />
<input type="hidden" id="NewPart" name="NewPart" value="<%=NewPart%>" />
<input type="hidden" id="RevisedPart" name="RevisedPart" value="<%=RevisedPart%>" />
<input type="hidden" id="RevisedDrawing" name="RevisedDrawing" value="<%=RevisedDrawing%>" />
<input type="hidden" id="CADModel" name="CADModel" value="<%=CADModel%>" />
<input type="hidden" id="BOM" name="BOM" value="<%=BOM%>" />

<input type="hidden" id="extraReportParameters" name="extraReportParameters" value="ECNNumber,Obsolete,NewPart,RevisedPart,RevisedDrawing,CADModel,BOM" />
<input type="hidden" id="renderCustomPage" name="renderCustomPage" 	value="true" />

<script language="JavaScript">


function keyValue(event){
	//alert("111111");
	var keyvalue;
	if((event.which && event.which == 13) || (event.keyCode && event.keyCode == 13)){
		//alert("22222");
		keyvalue = true;
		//bt.click(); 
		//return true;
	}
	//alert("key vlaue "+keyvalue);
	if(keyvalue){
		//alert("333333");
		submitReport();
	}
}

function submitReport()

{
    if(document.getElementById('ecnNum').value){
			document.getElementById('ECNNumber').value = document.getElementById('ecnNum').value;
			document.getElementById('Obsolete').value = document.getElementById('Obsolete_Part').checked;
			document.getElementById('NewPart').value = document.getElementById('New_Part').checked;
			document.getElementById('RevisedPart').value = document.getElementById('Revised_Part').checked;
			document.getElementById('RevisedDrawing').value = document.getElementById('Revised_Drawings').checked;
			document.getElementById('CADModel').value = document.getElementById('CAD_Model').checked;
			document.getElementById('BOM').value = document.getElementById('BOM_Part').checked;
			if(document.getElementById('All_Sections').checked)
			{
				document.getElementById('Obsolete').value = document.getElementById('All_Sections').checked;
				document.getElementById('NewPart').value = document.getElementById('All_Sections').checked;
				document.getElementById('RevisedPart').value = document.getElementById('All_Sections').checked;
				document.getElementById('RevisedDrawing').value = document.getElementById('All_Sections').checked;
				document.getElementById('CADModel').value = document.getElementById('All_Sections').checked;
				document.getElementById('BOM').value = document.getElementById('All_Sections').checked;
			}
			document.getElementById("renderCustomPage").value = "false";
			document.forms.mainform.submit(); // submit to reporting engine
		}
		
		else if(!document.getElementById('All_Sections').checked&&!document.getElementById('Obsolete_Part').checked&&! document.getElementById('New_Part').checked&&!document.getElementById('Revised_Drawings').checked&&!document.getElementById('CAD_Model').checked&&!document.getElementById('Revised_Part').checked&&!document.getElementById('BOM_Part').checked){
		  alert("Please select any one of the checkboxes for report generation");
		  return false;
		}
		else
		{
		    alert("Please Enter the ECN Number");
			document.getElementById('ecnNum').value="";
			document.getElementById('ecnNum').setFocus();
		}
}

function clearForm() 
{

   // clear all report parameters

   document.getElementById('ecnNum').value="";
   document.getElementById('All_Sections').disabled=false;
   
   showAll(true);
   showAllChecked(true);
   document.getElementById('All_Sections').checked=true;
}

function cancelForm()
{
  // document.forms.mainform.close();
   window.close();
}

function hideAll()
{
 
	if(!document.getElementById('All_Sections').checked)
	{
	showAll(false);
	showAllChecked(false);
	
		//document.getElementById('All_Sections').disabled=true;
	}
	
	if(document.getElementById('All_Sections').checked)
	{
		showAll(true);
		showAllChecked(true);
		
	}
}

function showAll( value)
{
	document.getElementById('Obsolete_Part').disabled=value;
	document.getElementById('New_Part').disabled=value;
	document.getElementById('Revised_Part').disabled=value;
	document.getElementById('Revised_Drawings').disabled=value;
	document.getElementById('CAD_Model').disabled=value;
	document.getElementById('BOM_Part').disabled=value;
}

function showAllChecked( value)
{
	document.getElementById('Obsolete_Part').checked=value;
	document.getElementById('New_Part').checked=value;
	document.getElementById('Revised_Part').checked=value;
	document.getElementById('Revised_Drawings').checked=value;
	document.getElementById('CAD_Model').checked=value;
	document.getElementById('BOM_Part').checked=value;
}
</script>

<style>
.wizHdr         {text-align: right; margin-top: 8px; margin-bottom: 8px; background-color: #0C4891;table-layout:fixed;
background: #0C4891; padding: 4px; color: #FDF8CE; font-weight: bold; font-size: 1.1em; vertical-align: middle;}
.hlpBg_Hidden {visibility: hidden; background-color: #E4D57D; border-top: 1px solid #f6f2d8; border-left: 1px solid #f6f2d8; border-right: 1px solid #918a6e; border-bottom: 1px solid #918a6e; text-align: center; padding-top:2px; padding-bottom:2px; height: 16px; width: 16px; margin-right:1px; margin-top:1px; margin-bottom:1px;}
.wizBtn {background-color: #E4D57D; margin-right:10px; margin-bottom:2px; margin-top:2px;}
.wizBtn:hover, .wizBtn:focus, .wizBtn:active {text-decoration: underline;}

</style>

<div class="wizHdr" style="text-align: right"><span
	style="float: left"><%=reportLabel%></span> <span
	class=hlpBg_Hidden></span></div>

<h3><%=inputParametersTitleLabel%></h3>
<table onkeypress="keyValue(event)" >
    
	<tr>
		<td><B><%=ecnNumberLbl%> : <B></td>
		<td><input type="text" id="ecnNum" MAXLENGTH='25' name="ecnNum"  value="" />
		</td>
	</tr>
	
	<tr>	
		<td><input type="checkbox" id="All_Sections" name="All_Sections"  checked="true" onClick="hideAll();" />
		<B><%=allLbl%><B></td>
		
	</tr>
	<tr>
		<td><input type="checkbox" id="Obsolete_Part" name="Obsolete_Part"  disabled="false" checked="true" />
		<B><%=obsoltePartsLbl%><B></td>
	</tr>
    <tr>	
		<td><input type="checkbox" id="New_Part" name="New_Part"  disabled="false" checked="true" />
		<B><%=newPartsLbl%><B></td>
	</tr>
    <tr>	
		<td><input type="checkbox" id="Revised_Part" name="Revised_Part"  disabled="false" checked="true" />
		<B><%=revsiedPartsLbl%><B></td>
	</tr>
    <tr>	
		<td><input type="checkbox" id="Revised_Drawings" name="Revised_Drawings"  disabled="false" checked="true" />
		<B><%=revisedDrawingsLbl%><B></td>
	</tr>
	<tr>	
		<td><input type="checkbox" id="CAD_Model" name="CAD_Model"  disabled="false" checked="true" />
		<B><%=CADModelLbl%><B></td>
	</tr>
    <tr>	
		<td><input type="checkbox" id="BOM_Part" name="BOM_Part"  disabled="false" checked="true" />
		<B><%=bomLbl%><B></td>
	</tr>
    
	
	
	
	

<br >
<table>
	<tr>
		<td><input type="submit" id="continueButton"
			name="continueButton" class=wizBtn value="<%=generateLabel%>" onClick="submitReport()" /></td>
		<td><input type="button" id="cancelButton" name="cancelButton"
			class=wizBtn value="<%=cancelLabel%>" onClick="cancelForm()" /></td>
		<td><input type="button" id="clearButton" name="clearButton"
			class=wizBtn value="<%=clearLabel%>" onClick="clearForm()" /></td>
	</tr>
</table>

<%
         // don't show bottom ptc banner
         nmcontext.setPortlet(NmContextBean.Portlet.POPPED_UP);
%>
<%@include file="/netmarkets/jsp/util/end.jspf"%>