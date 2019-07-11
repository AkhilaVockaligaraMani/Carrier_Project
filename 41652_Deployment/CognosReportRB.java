package ext.carrier.wc.cognos;

import wt.util.resource.RBEntry;
import wt.util.resource.RBUUID;
import wt.util.resource.RBComment;
import wt.util.resource.WTListResourceBundle;

	@RBUUID("ext.carrier.wc.cognos.CognosReportRB")
	public class CognosReportRB extends WTListResourceBundle {
	
	@RBEntry("Enter CTD ECN Number:")
	@RBComment("Label for entering ECN number.")
	public static final String ECN_NUMBER_LABEL= "ECN_NUMBER_LABEL";

	@RBEntry("Generate")
	@RBComment("Label for Generate Button.")
	public static final String GENERATE_LABEL= "GENERATE_LABEL";

	@RBEntry("Cancel")
	@RBComment("Label for Cancel Button.")
	public static final String CANCEL_BUTTON_LABEL= "CANCEL_BUTTON_LABEL";

	@RBEntry("Clear")
	@RBComment("Label for Clear Button.")
	public static final String CLEAR_BUTTON_LABEL= "CLEAR_BUTTON_LABEL";

	@RBEntry("Input Parameters")
	@RBComment("Label for Input Parameters.")
	public static final String INPUT_PARAMETERS_TITLE_LABEL= "INPUT_PARAMETERS_TITLE_LABEL";

	@RBEntry("CTD Enterprise Change Notice")
	@RBComment("Report Label.")
	public static final String CTD_ECN_REPORT_LABEL= "CTD_ECN_REPORT_LABEL";	
	
	@RBEntry("No CTD ECN found for that number. Please try with a different number.")
	@RBComment("No CTD ECN found for that number.")
	public static final String ECN_NOT_FOUND= "ECN_NOT_FOUND";

	@RBEntry("Internal Error.Please contact Administrator.")
	@RBComment("Exception Message")
	public static final String ERROR_MESSAGE= "ERROR_MESSAGE";

	@RBEntry("Search for Number:")
	@RBComment("Number Label")
	public static final String NUMBER_LABEL= "NUMBER_LABEL";

	@RBEntry("Search")
	@RBComment("Label for SEARCH Button.")
	public static final String SEARCH_BUTTON_LABEL= "SEARCH_BUTTON_LABEL";

	@RBEntry("Number at Lifecycle State")
	@RBComment("Name of the Report")
	public static final String LCSTATE_REPORT_NAME= "LCSTATE_REPORT_NAME";

	@RBEntry("Object Types (Default option: Documents & Drawings)...")
	@RBComment("Object Types Label")
	public static final String OBJECT_TYPE_LABEL= "OBJECT_TYPE_LABEL";
	
	@RBEntry("Lifecycle States (Default option: All Released)...")
	@RBComment("Lifecycle State Label")
	public static final String LC_STATE_LABEL= "LC_STATE_LABEL";

	@RBEntry("All Objects")
	@RBComment("All Object Types Label")
	public static final String ALL_OBJECT_LABEL= "ALL_OBJECT_LABEL";

	@RBEntry("Part(Item Master)")
	@RBComment("Part Object Types Label")
	public static final String PART_OBJECT_LABEL= "PART_OBJECT_LABEL";

	@RBEntry("ProE/Assembly and Part ")
	@RBComment("ProE/Assembly and Part Label")
	public static final String PROE_ASSEMBLY_LABEL= "PROE_ASSEMBLY_LABEL";

	@RBEntry("Drawing")
	@RBComment("Drawing Object Types Label")
	public static final String DRAWING_OBJECT_LABEL= "DRAWING_OBJECT_LABEL";

	@RBEntry("Document")
	@RBComment("Document Object Type Label")
	public static final String DOCUMENT_OBJECT_LABEL= "DOCUMENT_OBJECT_LABEL";
	
	@RBEntry("EPMDocument(Pro/E) ")
	@RBComment("EPM Document Label")
	public static final String EPM_DOCUMENT_LABEL= "EPM_DOCUMENT_LABEL";

	@RBEntry("Drawing Document")
	@RBComment("Drawing Document Label")
	public static final String DRAWING_DOCUMENT_LABEL= "DRAWING_DOCUMENT_LABEL";

	@RBEntry("All Released")
	@RBComment("Latest Released LCState Label")
	public static final String LC_LATEST_RELEASED_LABEL= "LC_LATEST_RELEASED_LABEL";

	@RBEntry("All Lifecycle States")
	@RBComment("All LCStates Label")
	public static final String ALL_LCSTATE_LABEL= "ALL_LCSTATE_LABEL";

	@RBEntry("Sorting (Default option: Number)...")
	@RBComment("Sort By Label")
	public static final String SORTED_COLUMN_LABEL= "SORTED_COLUMN_LABEL";

	@RBEntry("Zip (Default option: None)...")
	@RBComment("ZIP Option Label")
	public static final String ZIP_OPTION_LABEL= "ZIP_OPTION_LABEL";
	
	@RBEntry("Include Viewables in Native format")
	@RBComment("ZIP Option Label")
	public static final String ZIP_OPTION= "ZIP_OPTION";

	@RBEntry("Latest Released Search")
	@RBComment("Name of the Report")
	public static final String LRSEARCH_REPORT_NAME= "LRSEARCH_REPORT_NAME";

	@RBEntry("Object Types (Default option: Documents, Drawings, and Part(Item Masters))...")
	@RBComment("Object Types Label")
	public static final String LRS_OBJECT_TYPE_LABEL= "LRS_OBJECT_TYPE_LABEL";

	@RBEntry("Obsolete Parts")
	@RBComment("Obsolete Part Report")
	public static final String OBSOLETE_PART_SECTION= "OBSOLETE_PART_SECTION";

	@RBEntry("New Parts")
	@RBComment("New Part Report")
	public static final String NEW_PART_SECTION= "NEW_PART_SECTION";

	@RBEntry("Revised Parts")
	@RBComment("Revised Part Report")
	public static final String REVISED_PART_SECTION= "REVISED_PART_SECTION";
	
	@RBEntry("Revised Drawings")
	@RBComment("Revised Drawings Report")
	public static final String REVISED_DRAWING_SECTION= "REVISED_DRAWING_SECTION";
// ESR 41652 Addind a CADModel Section
	@RBEntry("CAD Model")
	@RBComment("CAD Model Report")
	public static final String CAD_Model = "CAD_Model";
	
	@RBEntry("Revised BOM")
	@RBComment("Revised BOM Report")
	public static final String BOM_PART_SECTION= "BOM_PART_SECTION";

	@RBEntry("All")
	@RBComment("All Report Sections ")
	public static final String ALL_PART_SECTION= "ALL_PART_SECTION";

	@RBEntry("ECN Release Report")
	@RBComment("ECN Release Report ")
	public static final String REPORT_NAME= "REPORT_NAME";

	@RBEntry("Enter the ECN Number")
	@RBComment("Enter the ECN Number")
	public static final String ECNLABLE_NAME= "ECNLABLE_NAME";
	
	@RBEntry("YES")
	@RBComment("Label for YES Button.")
	public static final String YES_BUTTON_LABEL= "YES_BUTTON_LABEL";

	@RBEntry("NO")
	@RBComment("Label for NO Button.")
	public static final String NO_BUTTON_LABEL= "NO_BUTTON_LABEL";
	
	@RBEntry(" ECN Archival Document has recently generated PDFs. Click Yes if you want to generate or Click No if you want to view previously generated PDFs.")
	@RBComment("Message to decide")
	public static final String MESSAGE= "MESSAGE";
	
	@RBEntry("ECN Archive Report")
	@RBComment("ARCHIVE_NAME")
	public static final String ARCHIVE_NAME= "ARCHIVE_NAME";
	
	@RBEntry("ECN Archive Report")
	@RBComment("LIBNAME")
	public static final String LIBNAME= "LIBNAME";
	
	@RBEntry("Run Report in Background")
	@RBComment("BGOPT")
	public static final String BGOPT= "BGOPT";
	
	@RBEntry("Report will run in background, an email will be sent once the report is generated")
	@RBComment("JSPERROR1")
	public static final String JSPERROR1= "JSPERROR1";
	
	@RBEntry("A report request has been submitted by another user for generation. You will receive a notification Email after it is generated")
	@RBComment("JSPERROR2")
	public static final String JSPERROR2= "JSPERROR2";
	
	@RBEntry("Please Enter valid ECN Number")
	@RBComment("JSPERROR3")
	public static final String JSPERROR3= "JSPERROR3";
	
	@RBEntry("ECN is having recently generated PDFs, Do you want to generate the same again? Click OK if you want to generate new PDFs else CANCEL if you are okay to see the existing PDFs")
	@RBComment("JSPERROR4")
	public static final String JSPERROR4= "JSPERROR4";
	
	@RBEntry("A new Requested Report has been submitted for generation. A notification Email having Archival Document link, will be sent when completed. Click OK to view the report output, else click CANCEL to close the window")
	@RBComment("JSPERROR5")
	public static final String JSPERROR5= "JSPERROR5";
	
	@RBEntry("A new Requested Report has been submitted for generation. A notification Email having Archival Document link, will be sent if provided ECN exists in windchill. Click OK to view the report output, else click CANCEL to close the window")
	@RBComment("JSPERROR6")
	public static final String JSPERROR6= "JSPERROR6";
		
	@RBEntry("is not valid.")
	@RBComment("JSPERROR7")
	public static final String JSPERROR7= "JSPERROR7";
	
	@RBEntry("Existing PDFs displayed")
	@RBComment("JAVAERROR1")
	public static final String JAVAERROR1= "JAVAERROR1";
	
	@RBEntry("No Drawing/BOM as resulting objects")
	@RBComment("TASKERROR1")
	public static final String TASKERROR1= "TASKERROR1";
	
	@RBEntry("New PDFs displayed")
	@RBComment("JAVAERROR2")
	public static final String JAVAERROR2= "JAVAERROR2";
	
	@RBEntry("No Resulting Objects")
	@RBComment("TASKERROR2")
	public static final String TASKERROR2= "TASKERROR2";
	
	@RBEntry("No PDF attachments for display")
	@RBComment("TASKERROR3")
	public static final String TASKERROR3= "TASKERROR3";
	
	@RBEntry("Failed writing BOM logs")
	@RBComment("TASKERROR4")
	public static final String TASKERROR4= "TASKERROR4";
	
	@RBEntry("Failed writing DRW logs")
	@RBComment("TASKERROR5")
	public static final String TASKERROR5= "TASKERROR5";
	
	@RBEntry("Drawing/BOM data in the resulting items does not have viewable")
	@RBComment("TASKERROR6")
	public static final String TASKERROR6= "TASKERROR6";
	
	@RBEntry("No Resulting Objects")
	@RBComment("TASKERROR7")
	public static final String TASKERROR7= "TASKERROR7";
	
	@RBEntry("Invalid ECN number. Please retry.")
	@RBComment("TASKERROR8")
	public static final String TASKERROR8= "TASKERROR8";
	
	@RBEntry("Error while generating")
	@RBComment("TASKERROR9")
	public static final String TASKERROR9= "TASKERROR9";

}
