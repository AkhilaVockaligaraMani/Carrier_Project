package ext.carrier.wc.cognos;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import wt.adapter.BasicWebjectDelegate;
import wt.change2.ChangeActivity2;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.Changeable2;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentType;
import wt.epm.navigator.CollectItem;
import wt.epm.navigator.EPMNavigateHelper;
import wt.epm.navigator.relationship.UIRelationships;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTSet;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.State;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.pds.StatementSpec;
import wt.project.Role;
import wt.query.ClassAttribute;
import wt.query.OrderBy;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.series.MultilevelSeries;
import wt.services.applicationcontext.implementation.ServiceProperties;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;
import wt.vc.VersionControlHelper;
import wt.vc.config.ConfigHelper;
import wt.vc.config.LatestConfigSpec;
import wt.workflow.definer.UserEventVector;
import wt.workflow.definer.WfAssignedActivityTemplate;
import wt.workflow.definer.WfDefinerHelper;
import wt.workflow.definer.WfProcessTemplate;
import wt.workflow.definer.WfTemplateObject;
import wt.workflow.engine.ProcessData;
import wt.workflow.engine.WfActivity;
import wt.workflow.engine.WfEngineHelper;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfVotingEventAudit;
import wt.workflow.work.WfAssignedActivity;
import com.infoengine.object.factory.Att;
import com.infoengine.object.factory.Element;
import com.infoengine.object.factory.Group;
import com.ptc.core.ocmp.framework.ConfigResourceException;
import com.ptc.core.ocmp.framework.LinkDiffs;
import com.ptc.windchill.enterprise.history.HistoryTablesCommands;
import com.ptc.windchill.enterprise.history.MaturityHistoryInfo;
import ext.carrier.wc.change.changenotice.IBAReader;
import ext.carrier.wc.change.partslister.PartsListerUtility;

public class CognosReportHelper {

	private static final int String = 0;
	public static Properties properties = null;
	public static String DEBUG = null;
	public static ArrayList<ChangeActivity2> activityList=new ArrayList();

	public static String releaseable_states=null;
	public static ArrayList statelist=new ArrayList();

	static {
		try {
			properties = ServiceProperties.getServiceProperties("WTServiceProviderFromProperties");
			DEBUG = properties.getProperty("ext.carrier.wc.cognos.reports.DEBUG").trim();
			releaseable_states = properties.getProperty
			("ext.carrier.wc.cognos.ECNReleaseReport.releasable_states");
			StringTokenizer objectTokenizer = new StringTokenizer(releaseable_states, ",");

			while(objectTokenizer.hasMoreTokens())
			{
				String tokens=objectTokenizer.nextToken();

				statelist.add(tokens);
			}



		} catch (Exception e) {
			sop(" Exception in Static block in CognosReportHelper");
			e.printStackTrace();
		}
	}

	/**
	 * This method queries for WTChangeOrder2 object.
	 *
	 * @param strEcnNumber
	 *            - ECN Object number.
	 * @return WTChangeOrder2
	 * @throws Exception
	 */

	public static WTChangeOrder2 getECNObj(String strEcnNumber) throws Exception {
		sop("CognosReportHelper CLASS ||strEcnNumber::" + strEcnNumber);
		WTChangeOrder2 ctdECN = null;

		try {

			QuerySpec querySpec = new QuerySpec(WTChangeOrder2.class);
			querySpec.appendWhere(new SearchCondition(WTChangeOrder2.class, "master>number", "=", strEcnNumber), new int[] { 0 });
			QueryResult queryResult = PersistenceHelper.manager.find((StatementSpec) querySpec);
			if (queryResult != null && queryResult.size() > 0)// ecns MIGHT get
				// revised in
				// future. hence
				// >0
			{
				ctdECN = (WTChangeOrder2) queryResult.nextElement();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return ctdECN;
	}

	/**
	 * This method retrieves workflow roles signatures.
	 *
	 * @param ctdECN
	 *            - A WTChangeOrder2 object.
	 * @return HashMap - Returns a HashMap holding roles as key and signatures
	 *         as values.
	 * @throws Exception
	 */
	public static HashMap<String, Serializable> getWfSignatures(WTChangeOrder2 ctdECN) throws Exception {

		Vector<Object> vecActivityFromProp = new Vector<Object>();
		Vector<String> vecRolesFromProp = new Vector<String>();
		HashMap<String, Serializable> hmRoleSignature = new HashMap<String, Serializable>();
		String strRole = "";
		String strSignature;
		String strActivityName;
		String strDate;
		String strAssigneeName;
		String strVote = "";
		java.util.Locale locale = java.util.Locale.getDefault();
		Hashtable<Object, String> htRolesInActivitiesThatLoop = new Hashtable<Object, String>();

		try {

			/*
			 * Setting up Roles required. All required roles, we shall pick from
			 * the wc.xconf Propertywill be:
			 * ext.carrier.wc.cognos.reports.CTDECNReport1.rolesrequired=CHANGE
			 * NOTICE IMPLEMENTATION BOARD|CHANGE NOTICE APPROVAL BOARD
			 */
			String strRolesForthisReport = properties.getProperty("ext.carrier.wc.cognos.reports.CTDECNReport1.rolesRequired");
			String strActivitiesForthisReport = properties.getProperty("ext.carrier.wc.cognos.reports.CTDECNReport1.activitiesRequired");
			String strRolesInActivitiesThatLoop = properties.getProperty("ext.carrier.wc.cognos.reports.CTDECNReport1.RolesInActivitiesThatLoop");
			String typeDeviationIdentifier = properties.getProperty("ext.carrier.wc.cognos.reports.CTDECNTYPE");

			StringTokenizer strtoken = new StringTokenizer(strRolesForthisReport, "|");
			StringTokenizer strtoken2 = new StringTokenizer(strActivitiesForthisReport, "|");
			StringTokenizer strtoken3 = new StringTokenizer(strRolesInActivitiesThatLoop, "|");

			while (strtoken.hasMoreElements()) {
				String strPropRole = (String) strtoken.nextElement();
				vecRolesFromProp.add(strPropRole);
				String strKeyDispValOfRole = Role.toRole(strPropRole).getDisplay(locale);
				hmRoleSignature.put(strPropRole, new Hashtable<Object, Object>());
				// Each Hashtable object above will eventually contain a key
				// value
				// pair of User Name and (Comment,Date).
			}
			sop("hmRoleSignature size" + hmRoleSignature.size());

			while (strtoken2.hasMoreElements()) {
				vecActivityFromProp.add(strtoken2.nextElement());
			}

			while (strtoken3.hasMoreElements()) {
				htRolesInActivitiesThatLoop.put(strtoken3.nextElement(), "");
			}

			// sop("CTD ECN Name::"+ctdECN.getName());
			String ctdECNType = com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(ctdECN).toString();
			sop("ctdECNType::" + ctdECNType);
			if (ctdECNType.indexOf(typeDeviationIdentifier) != -1) {
				QueryResult assocProcessResult = WfEngineHelper.service.getAssociatedProcesses(ctdECN, null, ctdECN.getContainerReference());
				sop("assocProcessResult size:;" + assocProcessResult.size());
				if (assocProcessResult.hasMoreElements()) {
					WfProcess process = (WfProcess) assocProcessResult.nextElement();
					sop("process  " + process.getName());
					if (!((process.getName()).contains("Carrier CTD Change Notice Process"))) {
						process = (WfProcess) assocProcessResult.nextElement();
					}
					sop("process  " + process.getName());
					WfProcessTemplate processTemplate = (WfProcessTemplate) process.getTemplate().getObject();
					Enumeration<?> startedActivityEnum = WfEngineHelper.service.getProcessSteps(process, null);
					Enumeration<?> notStartedActivityEnum = WfDefinerHelper.service.getStepTemplates(processTemplate, null);
					Vector<WfAssignedActivity> startedActivityVector = new Vector<WfAssignedActivity>();
					Vector<WTObject> vecAllActivities = new Vector<WTObject>();
					// Initally the Non Started activities are kept here... then
					// this grows.
					while (notStartedActivityEnum.hasMoreElements()) {
						WfTemplateObject templateObject = (WfTemplateObject) notStartedActivityEnum.nextElement();
						if (templateObject instanceof WfAssignedActivityTemplate) {
							WfAssignedActivityTemplate activityTemplate = (WfAssignedActivityTemplate) templateObject;
							// sop("activityTemplate 0000  = "+activityTemplate.getName());
							vecAllActivities.add(activityTemplate);
						}
					}
					while (startedActivityEnum.hasMoreElements()) {
						WfActivity activity = (WfActivity) startedActivityEnum.nextElement();
						// sop("activity 1111  = "+activity.getName());
						if (activity instanceof WfAssignedActivity) {
							WfAssignedActivity assignedActivity = (WfAssignedActivity) activity;
							startedActivityVector.add(assignedActivity);
						}
					}
					for (int j = 0; j < vecAllActivities.size(); j++) {
						WfAssignedActivityTemplate activityTemplate = (WfAssignedActivityTemplate) vecAllActivities.get(j);
						String objectID = PersistenceHelper.getObjectIdentifier((Persistable) activityTemplate).toString();
						for (int i = 0; i < startedActivityVector.size(); i++) {
							WfAssignedActivity activity = startedActivityVector.get(i);
							// sop("activity 222 == "+activity.getName());
							if (activity.getTemplate().getKey().toString().equals(objectID)) {
								vecAllActivities.set(j, activity);
							}
						}
					}

					int intVecAllActivitiesSize = vecAllActivities.size();
					sop("intVecAllActivitiesSize   = " + intVecAllActivitiesSize);
					for (int n = 0; n < intVecAllActivitiesSize; n++) {
						// sop("vecAllActivities.get(n)  ===> "+vecAllActivities.get(n));
						if (vecAllActivities.get(n) instanceof WfAssignedActivity) {
							WfAssignedActivity activity = (WfAssignedActivity) vecAllActivities.get(n);
							strActivityName = activity.getName();
							sop("Activity:" + strActivityName);

							// Following condition checks for the activityName
							// is matching to the activity name in the
							// propertyfile.
							if (vecActivityFromProp.contains(strActivityName)) {

								// For each assignedActivity getting the voting
								// audit events and fetching all the relevant
								// info.
								QueryResult votingEventResult = getVotingEventsForActivity(activity);
								sop("votingEventResult size::" + votingEventResult.size());

								while (votingEventResult.hasMoreElements()) {
									WfVotingEventAudit votingEventAudit = (WfVotingEventAudit) votingEventResult.nextElement();
									String signedDate = PersistenceHelper.getCreateStamp(votingEventAudit).toString();
									String strArr[] = signedDate.split(" ");
									strDate = strArr[0];
									// Added if loop for ESR 8287 - Null pointer
									// exception
									if (votingEventAudit.getRole() != null) {
										strRole = votingEventAudit.getRole().toString().trim();
										String role2 = Role.toRole(strRole).getDisplay(locale);
									}

									if (vecRolesFromProp.contains(strRole)) {
										sop("Completed by::" + votingEventAudit.getAssigneeRef().getIdentity());
										strAssigneeName = ((wt.org.WTUser) (votingEventAudit.getAssigneeRef().getPrincipal())).getFullName();
										UserEventVector userEventVec = (UserEventVector) votingEventAudit.getEventList();
										Vector<?> eventVec = userEventVec.toVector();
										sop("eventVec size:" + eventVec.size());
										if (eventVec.size() > 0) {
											strVote = (String) eventVec.elementAt(0);
										}
										strSignature = strAssigneeName + "(" + strDate + "," + strVote + ")";
										sop("strSignature::" + strSignature);
										Hashtable<String, String> htInner = (Hashtable<String, String>) hmRoleSignature.get(strRole);
										if (htInner.size() == 0) // this is for
											// the
											// regular
											// users.
										{
											htInner.put(strAssigneeName, strSignature);
										} else /*
										 * when htInner is having one or
										 * more values and if..as below
										 */
											if (null != htRolesInActivitiesThatLoop.get(strRole))// (we
												// have
												// only
												// 2
												// roles here)
											{
												if (htInner.get(strAssigneeName) == null) {
													htInner.put(strAssigneeName, strSignature);
													hmRoleSignature.put(strRole, htInner);
												}
												// above code ensures that Users
												// added before are not added again.
											}

										sop("htInner inside over");
									}
								}
							}
						}
					}
				}
			}
			hmRoleSignature = convertInnerHashtabletoStringVal(hmRoleSignature);
			hmRoleSignature.put("Error", "");// Error element is null
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return hmRoleSignature;
	}

	/**
	 * This method constructs comma separated signature values for a particular
	 * role.
	 *
	 * @param hmRoleSignature
	 *            - A HashMap containing roles as key and signatures as values.
	 * @param HashMap
	 *            - A HashMap containing roles as key and comma separated
	 *            signatures as values.
	 * @throws Excaption
	 */
	public static HashMap<String, Serializable> convertInnerHashtabletoStringVal(HashMap<String, Serializable> hmRoleSignature) throws Exception {
		Hashtable<?, ?> htInner = null;
		String strName = null;
		String strNameDateandVote = null;
		String strRole = null;
		String strFinal = "";
		Collection<String> collKeys = hmRoleSignature.keySet();
		Iterator<String> iterKeys = collKeys.iterator();

		while (iterKeys.hasNext()) // for each role
		{
			strRole = iterKeys.next();
			htInner = (Hashtable<?, ?>) hmRoleSignature.get(strRole);
			Enumeration<?> enumKeysInner = htInner.keys();

			while (enumKeysInner.hasMoreElements())// For each Hashtable Element
				// for That role
			{
				strName = (String) enumKeysInner.nextElement();
				strNameDateandVote = (String) htInner.get(strName);
				sop("strNameDateandVote:" + strNameDateandVote);

				if ("".equals(strFinal)) {
					strFinal = strNameDateandVote;
				} else {
					strFinal = strFinal + "," + strNameDateandVote;
				}
			}
			hmRoleSignature.put(strRole, strFinal);
			strFinal = "";
		}
		return hmRoleSignature;
	}

	/**
	 * This method gets a QueryResult of voting audit events for a passed
	 * WfAssignedActivity object.
	 *
	 * @param wfassignedactivity
	 *            - An WfAssignedActivity object.
	 * @returns QueryResult - QueryResult of voting audit events of an activity.
	 * @throws WTException
	 */
	public static QueryResult getVotingEventsForActivity(WfAssignedActivity wfassignedactivity) throws WTException {
		// sop("getVotingEventsForActivity.method()");
		QuerySpec queryspec = new QuerySpec();
		int i = queryspec.appendClassList(WfVotingEventAudit.class, true);
		int j = queryspec.appendClassList(WfAssignedActivity.class, false);
		try {
			queryspec.setDescendantQuery(false);
		} catch (WTPropertyVetoException wtpropertyvetoexception) {
		}
		/*
		 * Start-code modified by Carrier Upgrade Team for deprecated API
		 */
		queryspec.appendWhere(new SearchCondition(WfVotingEventAudit.class, "activityKey", WfAssignedActivity.class, "key"), new int[]{i, j});
		if (wfassignedactivity != null) {
			queryspec.appendAnd();
			queryspec.appendWhere(new SearchCondition(WfActivity.class, "thePersistInfo.theObjectIdentifier.id", "=", getId(wfassignedactivity)), new int[]{j});
		}
		/*
		 * End-code modified by Carrier Upgrade Team for deprecated API
		 */
		try {
			queryspec.setQuerySet(false);
		} catch (WTPropertyVetoException wtpropertyvetoexception1) {
			throw new WTException(wtpropertyvetoexception1);
		}
		/*
		 * Start-code modified by Carrier Upgrade Team for deprecated API
		 */
		ClassAttribute classAttribute = new ClassAttribute(WfVotingEventAudit.class,  "thePersistInfo.modifyStamp");
		OrderBy orderBy = new OrderBy(classAttribute, true);
		queryspec.appendOrderBy(orderBy, new int[]{0});
		QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec)queryspec);
		/*
		 * End-code modified by Carrier Upgrade Team for deprecated API
		 */
		return queryresult;
	}

	/**
	 * This method gets the ObjectIdentifier of the passed object.
	 *
	 * @param obj
	 * @returns ObjectIdentifier
	 */
	public static ObjectIdentifier getOid(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof ObjectReference) {
			return (ObjectIdentifier) ((ObjectReference) obj).getKey();
		} else
			return PersistenceHelper.getObjectIdentifier((Persistable) obj);
	}

	/**
	 * This method gets the object's OID if the passed object is not null.
	 *
	 * @param obj
	 * @returns long - Returns an object's OID
	 */
	private static long getId(Object obj) {
		if (obj == null) {
			return 0L;
		} else
			return getOid(obj).getId();
	}

	/**
	 * This method prints the passed message on the MethodServer.
	 *
	 * @param message
	 *            - Message to be printed on the MethodServer.
	 * @return void
	 */
	public static void sop(String message) {
		try {
			if ("true".equalsIgnoreCase(DEBUG)) {
				System.out.println(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method retrieves the value for the given attribute, It is assumed
	 * that the ibaread object is initialized to the persistable obj.
	 *
	 * @param strAttribName
	 *            - A String object.
	 * @return String - Returns the IBA value if found, else returns "". For
	 *         boolean attribs, it returns "Yes" or "No"
	 * @throws Exception
	 */
	public static String getValue(String strAttribName, ext.carrier.wc.change.changenotice.IBAReader ibaRead) throws Exception {

		String strIbaVal = ibaRead.getValue(strAttribName);

		if (null == strIbaVal) {
			return "";
		} else if ("TRUE".equals(strIbaVal.toUpperCase())) {
			return "Yes";
		} else if ("FALSE".equals(strIbaVal.toUpperCase())) {
			return "No";
		}
		return strIbaVal;
	}

	/**
	 * This method retrieves the list of the ObsoletePart . When the Change
	 * Activity(PromoteToObsolete is set true) retrieves the Resulting
	 * object(WTPart).
	 *
	 * @param changeActivityGroup
	 * @return obsoleteList holds the Obsolete part list.
	 */
	public static ArrayList<WTObject> getObsoletePart(Group changeActivityGroup) {
		ArrayList<WTObject> obsoleteList = new ArrayList<WTObject>();
		HashMap<String, WTPart> allReportActivy = new HashMap<String, WTPart>();

		for (int i = 0; i < changeActivityGroup.getElementCount(); i++) {
			try {
				String projectOid = (String) changeActivityGroup.getAttributeValue(i, "obid");
				projectOid = projectOid.substring(0, projectOid.lastIndexOf(":"));
				wt.fc.ReferenceFactory rf = new wt.fc.ReferenceFactory();
				wt.change2.WTChangeActivity2 changeActivity = (wt.change2.WTChangeActivity2) rf.getReference(projectOid).getObject();

				String obsServ=getObsoleteFlag(changeActivity);


				boolean promatetoObsolete=false;
				boolean promatetoService=false;
				boolean promatetoProduction=false;

				if(obsServ.contains("obs"))
				{

					promatetoObsolete=true;

				}
				else if(obsServ.contains("serv"))
				{

					promatetoService=true;

				}
				else if(obsServ.contains("prod"))
				{

					promatetoProduction=true;

				}
				obsoleteList = getReslutingData(changeActivity, obsoleteList, promatetoObsolete,promatetoService,promatetoProduction);
				//System.out.println("!!!!!!obsoleteList   --> "+obsoleteList);
				//System.out.println("   ");
			}

			// end try, get the current EPMdocument using the obid
			catch (Exception e13) {
				sop("E13 exception in ECNSummaryReportQuery.xml getting the ChangeActivity");
				e13.printStackTrace();
			}

		}
		return obsoleteList;

	}

	/**
	 * chech values for PromoteToObsolete and PromoteToService
	 * @param changeActivity
	 * @return
	 */
	public static String getObsoleteFlag(ChangeActivity2 changeActivity)
	{
		Enumeration<?> processEnum;
		String obs="";
		String serv="";
		String prod="";
		try {
			processEnum = WfEngineHelper.service.getAssociatedProcesses(changeActivity, null, changeActivity.getContainerReference());

			WfProcess wfProcess = null;
			ProcessData processData = null;

			Enumeration<?> wfActivityEnum = null;
			boolean promatetoObsolete = false;
			TreeSet ts =new TreeSet();
			while (processEnum.hasMoreElements()) {
				wfProcess = (WfProcess) processEnum.nextElement();
				wfActivityEnum = WfEngineHelper.service.getProcessSteps(wfProcess, null);
				WfActivity wfActivity = null;
				while(wfActivityEnum.hasMoreElements()){		
					wfActivity = (WfActivity) wfActivityEnum.nextElement();
					String wfActivityName = wfActivity.getName();
					ts.add(wfActivityName);
				}
				Iterator itr=ts.iterator();
				sop(" Sorted list is: " +ts);
				while (itr.hasNext()) {		
					String item=(java.lang.String) itr.next();
					if (item.equals("Complete Change Notice Task")) {
						sop(" --------Element matched to complete Change notice task");
						processData = wfActivity.getContext();
						Object evalDueDateObj = processData.getValue("PromoteToObsolete");
						Object evalDueDateObjServ=processData.getValue("PromoteToService");
						Object evalDueDateObjProd=processData.getValue("PromoteToProduction");

						sop("PromoteToObsolete" + evalDueDateObj);

						if (evalDueDateObj != null) {
							if (evalDueDateObj instanceof Boolean) {
								promatetoObsolete = ((Boolean) evalDueDateObj).booleanValue();
								sop("Obsolete parts to be display even in all the states" + promatetoObsolete);
								if(promatetoObsolete==true)
									obs="obs";

							}
						}
						if (evalDueDateObjServ != null) {

							if (evalDueDateObjServ instanceof Boolean) {
								promatetoObsolete = ((Boolean) evalDueDateObjServ).booleanValue();
								sop("Obsolete parts to be display even in all the states" + promatetoObsolete);
								if(promatetoObsolete==true)
									serv="serv";

							}

						}
						if (evalDueDateObjProd != null) {

							if (evalDueDateObjProd instanceof Boolean) {
								promatetoObsolete = ((Boolean) evalDueDateObjProd).booleanValue();
								sop("Obsolete parts to be display even in all the states" + promatetoObsolete);
								if(promatetoObsolete==true)
									prod="prod";

							}
						}

					}
					if (item.equals("Rework Change Notice Task")) {
						sop(" --------Element matched to Review Change notice task");
						processData = wfActivity.getContext();
						Object evalDueDateObj = processData.getValue("PromoteToObsolete");
						Object evalDueDateObjServ=processData.getValue("PromoteToService");
						Object evalDueDateObjProd=processData.getValue("PromoteToProduction");

						sop("PromoteToObsolete" + evalDueDateObj);

						if (evalDueDateObj != null) {
							if (evalDueDateObj instanceof Boolean) {
								promatetoObsolete = ((Boolean) evalDueDateObj).booleanValue();
								sop("Obsolete parts to be display even in all the states" + promatetoObsolete);
								if(promatetoObsolete==true)
									obs="obs";
							}
						}
						if (evalDueDateObjServ != null) {
							if (evalDueDateObjServ instanceof Boolean) {
								promatetoObsolete = ((Boolean) evalDueDateObjServ).booleanValue();
								sop("Obsolete parts to be display even in all the states" + promatetoObsolete);
								if(promatetoObsolete==true)
									serv="serv";
							}
						}
						if (evalDueDateObjProd != null) {
							if (evalDueDateObjProd instanceof Boolean) {
								promatetoObsolete = ((Boolean) evalDueDateObjProd).booleanValue();
								sop("Obsolete parts to be display even in all the states" + promatetoObsolete);
								if(promatetoObsolete==true)
									prod="prod";
							}
						}


					}
				}

			}
		} catch (WTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return obs+","+serv+","+prod;
	}

	/**
	 * @param changeActivity
	 * @param obsoleteList
	 * @return
	 */
	public static ArrayList<WTObject> getReslutingData(wt.change2.WTChangeActivity2 changeActivity, ArrayList<WTObject> obsoleteList, boolean promatetoObsolete
			, boolean promatetoService, boolean promatetoProduction) {

		QueryResult changeableQueryResult;
		try {
			changeableQueryResult = ChangeHelper2.service.getChangeablesAfter(changeActivity);

			sop("ChangeablesCharacteristic:addCharacteristic:changeableQueryResult.size() : " + changeableQueryResult.size());

			Changeable2 changeable = null;

			while (changeableQueryResult.hasMoreElements()) {

				changeable = (Changeable2) changeableQueryResult.nextElement();

				if (changeable instanceof WTPart) {
					WTPart part = (WTPart) changeable;
					String state = part.getState().toString();
					//System.out.println("part object::::::  --->"+part);
					if(!(obsoleteList.contains(part)) ){
						//System.out.println("inside if loop ***************"+obsoleteList.contains(part));
						if (promatetoObsolete) {
							String currState = part.getState().toString();
							//--------------------read from properties---------------
							int loopCount=0;

							// Code is modified to fix ESR 25179 "ECN Release Report-Obs Parts not working"
							//It will add RO into obsolete list when RO belongs to Non PL container.
							
							if(!statelist.contains(currState))
								loopCount++;
							
							if(loopCount==0)
							{
								if(currState.equalsIgnoreCase("OBSOLETE"))
									obsoleteList.add(part);
							}

							else if ((loopCount==1) && !(part.getContainerName().equalsIgnoreCase("Parts Lister")) && currState.equalsIgnoreCase("UNDERREVIEW"))
							{
								obsoleteList.add(part);
							}
							
						} else if ((!promatetoObsolete && state.equalsIgnoreCase("OBSOLETE")) ) {
							if(part.getContainerName().equalsIgnoreCase("Parts Lister"))
								obsoleteList.add(part);
							else
								if(!promatetoService && !promatetoProduction)
									obsoleteList.add(part);

						}

					}

				}

			}

		} catch (ChangeException2 e) {

			e.printStackTrace();
		} catch (WTException e) {

			e.printStackTrace();
		}
		return obsoleteList;
	}

	/** This method will query for EPMDocument or DD document which is described for WTPart
	 * @param WTPart
	 * @return String[]
	 */

	public static String[] getEPMDocforWTPart(WTPart part) throws WTRuntimeException, WTException, IOException {

		String[] drawingData = new String[4];
		// List<EPMDocument> drawingList= new ArrayList<EPMDocument>();
		sop("Collecting the referenced EPMDocuments for WTPart " + part.getNumber());
		WTSet epmdocs = EPMNavigateHelper.navigate(part, UIRelationships.newAssociatedCADDocs(), CollectItem.OTHERSIDE).getResults(new WTHashSet());
		EPMDocument releatedEPMDoc = null;

		for (Iterator<?> it = epmdocs.persistableIterator(); it.hasNext();) {
			Object nextObj = it.next();
			if (nextObj instanceof EPMDocument) {
				releatedEPMDoc = (EPMDocument) nextObj;
				if (releatedEPMDoc.getDocType().equals(EPMDocumentType.toEPMDocumentType("CADDRAWING"))) {
					drawingData[0] = releatedEPMDoc.getNumber();
					sop("Adding the Drawing to reference Drawing List:" + releatedEPMDoc.getNumber());

					drawingData[1] = releatedEPMDoc.getVersionInfo().getIdentifier().getValue();
					drawingData[2] = releatedEPMDoc.getName();
					drawingData[3] = "EPMDoc";
				}
			}
		}
		if (drawingData[0] == null && drawingData[1] == null) {
			QueryResult qrDoc = WTPartHelper.service.getDescribedByDocuments(part);
			WTDocument thisDoc = null;
			while (qrDoc.hasMoreElements()) {
				wt.fc.WTObject wtdocobj = (wt.fc.WTObject) qrDoc.nextElement();
				if (wtdocobj instanceof wt.doc.WTDocument) {
					thisDoc = (wt.doc.WTDocument) wtdocobj;
					String docType = com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(thisDoc).toString();
					String type = wt.type.TypedUtility.getExternalTypeIdentifier(wtdocobj);
					String refDocPersistedType = wt.type.TypedUtility.getPersistedType("WCTYPE|wt.doc.WTDocument|com.utc.carrier.projectlink.UTC.DD");
					// String docType =
					// com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(document).toString();
					if (refDocPersistedType == null) {
						sop(" WARNING - WCTYPE|wt.doc.WTDocument|com.utc.carrier.projectlink.UTC.DD is not defined.\n Run loaddata.bat PLMLinkPartDocs.csv");
					}
					// if (TypedUtility.isInstanceOf(TypedUtility.getPersistedType(thisDoc), refDocPersistedType)) {
					if (docType.indexOf("DD") != -1) {
						sop("Doctype" + type);
						//Shawn Doherty:
						//All other documents must use 'name' for 'number' for ECNReleaseReport.
						drawingData[0] = thisDoc.getNumber();
						sop("Adding the Drawing to reference Drawing List:" + thisDoc.getNumber());
						drawingData[1] = thisDoc.getVersionInfo().getIdentifier().getValue();
						drawingData[2] = thisDoc.getName();
						drawingData[3] = "WTDoc";
					}
				}

			}

		}
		// }
		// }
		return drawingData;
	}

	/** This method will combine all the common elements that are required for report.
	 * @param gout
	 * @param element
	 * @param index
	 * @param wtPart
	 * @param reportName
	 * @param properties
	 * @return Element
	 */

	public static Element createECNCommonElements(Group gout, Element element, int index, WTPart wtPart, String reportName, Properties properties, String passedECNNumber) {
		
		try {
			String partNumber = wtPart.getNumber();
			String partName = wtPart.getName();
			String objectIcon = properties.getProperty("ext.carrier.wc.cognos.reports.ECNSummaryReport.WTPart_Icon");

			//System.out.println("Partnumber ____________"+partNumber);
			String version = wtPart.getVersionInfo().getIdentifier().getValue();
			String state = State.toState(wtPart.getState().toString()).getDisplay();
			ArrayList<WTChangeOrder2> changeOderList = getECN(passedECNNumber);



			String uom="";
			//block added for 14186---------
			String partcontainer=wtPart.getContainerName();
			ArrayList<WTChangeOrder2> orderList=new ArrayList();


			if(partcontainer.equalsIgnoreCase("Parts Lister") )
			{
				element.addAtt(new Att("state.state", state));
			}
			else
			{
				QueryResult qresCas=ChangeHelper2.service.getChangingChangeActivities(wtPart);
				while(qresCas.hasMoreElements())
				{
					ChangeActivity2 ca=(ChangeActivity2)qresCas.nextElement();
					//-----further add for 14186----------------
					QueryResult changeorders=ChangeHelper2.service.getChangeOrder(ca);
					while(changeorders.hasMoreElements())
					{
						orderList.add((WTChangeOrder2)changeorders.nextElement());
					}
					//-----------further add for 14186---------------
	
					int size=orderList.size();
					for(int h=0;h<size;h++)
					{
						//System.out.println("passedecnnummmmmmmmmmmmmmmmm:"+passedECNNumber+"oderlisttttttttttt"+((WTChangeOrder2)orderList.get(h)).getNumber());
						//System.out.println("CA>>>>>>>>>>>>>"+ca.getName());
						//line is edited for 14186
						if(passedECNNumber.equalsIgnoreCase(((WTChangeOrder2)orderList.get(h)).getNumber()))
						{
							int casize=activityList.size();
							for(int z=0;z<casize;z++)
							{
								if(ca.equals(activityList.get(z)))
								{
									String obsServ=getObsoleteFlag(ca);
	
									//System.out.println("_________________________________IIIIIIIIIII:"+obsServ);
									if(obsServ.contains("obs"))
										element.addAtt(new Att("state.state", "Obsolete"));
	
									else if(obsServ.contains("serv"))
										element.addAtt(new Att("state.state", "Service"));
	
									else if(obsServ.contains("prod"))
										element.addAtt(new Att("state.state", "Production"));
									else
										element.addAtt(new Att("state.state", state));
								}
							}
						}
					}
				}

			}
			//------------------------

			//String srcValue = wtPart.getSource().getDisplay();
			sop(partNumber + ":" + partName + ":" + version + ":" + state);
			//			WTPartUsageLink[] wtPartUsageLinkArray=PartHelper.getUsedBy(wtPart);
			//			// block added for unit of measure by Deb
			//			for(int g=0;g<wtPartUsageLinkArray.length;g++)
			//			{
			//			WTPartUsageLink wtPartUsageLink=wtPartUsageLinkArray[g];
			//			uom =  wtPartUsageLink.getQuantity().getUnit().getStringValue();
			//			uom=uom.substring(uom.lastIndexOf(".")+1,uom.length());
			//
			//			}

			uom=wtPart.getDefaultUnit().getStringValue();
			uom=uom.substring(uom.lastIndexOf(".")+1,uom.length());
			// element.addAtt(new Att("Number", partNumber));
			element.addAtt(new Att("fitemno", partName));
			element.addAtt(new Att("name", partName));
			//element.addAtt(new Att("state.state", state));
			element.addAtt(new Att("versionInfo.identifier.versionId", version));
			element.addAtt(new Att("uom",uom));
			String[] drawingData = CognosReportHelper.getEPMDocforWTPart(wtPart);
			if (drawingData[0] == null & drawingData[1] == null) {
				element.addAtt(new Att("drwNumber", " "));
				element.addAtt(new Att("drwRev", " "));
			} else {
				//Shawn Doherty drawing data array: {'number','rev','name','type'}
				if(drawingData[3] == "EPMDoc")
					element.addAtt(new Att("drwNumber", drawingData[0]));
				else
					element.addAtt(new Att("drwNumber", drawingData[2]));
				element.addAtt(new Att("drwRev", drawingData[1]));
			}

			// element.addAtt(new Att("ReportName", reportName));
			element.addAtt(new Att("Source", wtPart.getSource().getDisplay()));
			IBAReader iba = new IBAReader(wtPart);
			String partQual = iba.getDisplayValue("PART_QUALIFIER");
			element.addAtt(new Att("partQualifier", partQual));
			element.addAtt(new Att("itemDisp", " "));
			//commented to remove all previous unreleased ECN by Deb
			//ArrayList<WTChangeOrder2> changeOderList = getPreviousUnreleasedECN(wtPart, properties,passedECNNumber );

			element = getPreviousUnreleasedECN(element, changeOderList, reportName);
			sop("part Icon-->>" + objectIcon);
			element = createViewables(gout, element, index, properties, objectIcon, reportName);
		} catch (WTException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return element;
	}

	/** This method will retrieve all the Drawings of Resulting Items which are revised
	 * @param gout
	 * @param element
	 * @param index
	 * @param object
	 * @param properties
	 * @param reportName
	 * @param passedECNNumber
	 * @return Element
	 * @throws WTException
	 */
	public static Element createRevisedDrawings(Group gout, Element element, int index, WTObject object, Properties properties, String reportName, String passedECNNumber ) throws WTException {
		String objectIcon = "";
		if (object instanceof EPMDocument) {
			EPMDocument drawing = (EPMDocument) object;
			wt.epm.EPMDocumentType docType = drawing.getDocType();
			if (docType.toString().equals("CADDRAWING")) {

				String partName = drawing.getName();
				String version = drawing.getVersionInfo().getIdentifier().getValue();
				String state = State.toState(drawing.getState().toString()).getDisplay();
				// element.addAtt(new Att("Number", partNumber));
				element.addAtt(new Att("name", partName));
				//Below changes are reverted for fixing ESR 29187 "ECN Release Report - formatting issues and STATE for drawings"
				element.addAtt(new Att("state.state", state));
				
				//block added for 14186---------
				/*String partcontainer=drawing.getContainerName();

				String lcState=drawing.getLifeCycleState().getDisplay();
				if(partcontainer.equalsIgnoreCase("Parts Lister"))
				{
					element.addAtt(new Att("state.state", state));}
				else
				{
					QueryResult qresCas=ChangeHelper2.service.getChangingChangeActivities(drawing);
					while(qresCas.hasMoreElements())
					{
						ChangeActivity2 ca=(ChangeActivity2)qresCas.nextElement();
						int size=activityList.size();
						for(int h=0;h<size;h++)
						{
							if(ca.equals(activityList.get(h)))
							{
								String stateCa=ca.getLifeCycleState().getDisplay();

								element.addAtt(new Att("state.state", stateCa));
							}
						}
					}

				}*/
				//------------------------
				element.addAtt(new Att("versionInfo.identifier.versionId", version));

				objectIcon = properties.getProperty("ext.carrier.wc.cognos.reports.ECNSummaryReport.EPMDrw_Icon");
				sop("EPM Icon-->>" + objectIcon);
				// objectIcon = "/wt/clients/images/proe/drawing.gif";
				//commented to remove all previous unreleased ECN by Deb
				//ArrayList<WTChangeOrder2> changeOderList = getPreviousUnreleasedECN(wtPart, properties,passedECNNumber );
				ArrayList<WTChangeOrder2> changeOderList;
				try {
					changeOderList = getECN(passedECNNumber);

					element = getPreviousUnreleasedECN(element, changeOderList, reportName);
					element.addAtt(new Att("DrawingsReportName", "Yes"));
					element = createViewables(gout, element, index, properties, objectIcon, reportName);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		if (object instanceof WTDocument) {
			WTDocument document = (WTDocument) object;

			String refDocPersistedType = wt.type.TypedUtility.getPersistedType("WCTYPE|wt.doc.WTDocument|com.utc.carrier.projectlink.UTC.DD");
			String docType = com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(document).toString();
			if (refDocPersistedType == null) {
				sop(" WARNING - WCTYPE|wt.doc.WTDocument|com.utc.carrier.projectlink.UTC.DD is not defined.\n Run loaddata.bat PLMLinkPartDocs.csv");
			}
			sop("TypedUtility.getPersistedType(document) ==== " + TypedUtility.getPersistedType(document));
			sop("refDocPersistedType ==== " + refDocPersistedType);
			sop("docType ==== " + docType);
			sop("docType.indexOf == " + docType.indexOf("DD"));
			sop("documentNumber == " + document.getNumber());
			// if
			// (TypedUtility.isInstanceOf(TypedUtility.getPersistedType(document),
			// refDocPersistedType)) {
			if (docType.indexOf("DD") != -1) {
				sop("Inside if loop === ");
				String partName = document.getName();
				String version = document.getVersionInfo().getIdentifier().getValue();
				String state = State.toState(document.getState().toString()).getDisplay();
				// element.addAtt(new Att("Number", partNumber));
				
				//Below changes are reverted for fixing ESR 29187 "ECN Release Report - formatting issues and STATE for drawings"
				//block added for 14186---------
				/*String partcontainer=document.getContainerName();

				String lcState=document.getLifeCycleState().getDisplay();
				if(partcontainer.equalsIgnoreCase("Parts Lister") )
				{
					element.addAtt(new Att("state.state", state));}
				else
				{QueryResult qresCas=ChangeHelper2.service.getChangingChangeActivities(document);
				while(qresCas.hasMoreElements())
				{
					ChangeActivity2 ca=(ChangeActivity2)qresCas.nextElement();
					int size=activityList.size();
					for(int h=0;h<size;h++)
					{
						if(ca.equals(activityList.get(h)))
						{
							String stateCa=ca.getLifeCycleState().getDisplay();

							element.addAtt(new Att("state.state", stateCa));
						}
					}
				}

				}*/
				//------------------------
				element.addAtt(new Att("name", partName));
				element.addAtt(new Att("state.state", state));

				element.addAtt(new Att("versionInfo.identifier.versionId", version));
				objectIcon = properties.getProperty("ext.carrier.wc.cognos.reports.ECNSummaryReport.DD_Icon");
				sop("DD Icon-->>" + objectIcon);
				// objectIcon = "/wt/clients/images/doc_dwg.gif";
				//commented to remove all previous unreleased ECN by Deb
				//ArrayList<WTChangeOrder2> changeOderList = getPreviousUnreleasedECN(wtPart, properties,passedECNNumber );
				ArrayList<WTChangeOrder2> changeOderList = getECN(passedECNNumber);
				element = getPreviousUnreleasedECN(element, changeOderList, reportName);
				// }
				element.addAtt(new Att("DrawingsReportName", "Yes"));
				element = createViewables(gout, element, index, properties, objectIcon, reportName);
			}
		}

		return element;
	}

	/** This method will retrieve viewables of objects in Group
	 * @param gout
	 * @param element
	 * @param index
	 * @param object
	 * @param properties
	 * @param objectIcon
	 * @param reportName
	 * @return Element
	 */
	public static Element createViewables(Group gout, Element element, int index, Properties properties, String objectIcon, String reportName)
	{
		String pvIcon="";
		String Report_name="";
		String viewableURL = "";
		pvIcon = properties.getProperty("ext.carrier.wc.cognos.reports.BSSLCSTATEReport.PDF_THUMBNAIL");
		String classValue = (String) gout.getAttributeValue(index, "class");
		sop("Class information: " + classValue);
		String resultingObjectObid = (String) gout.getAttributeValue(index, "obid");
		sop("Got resulting object obid: " + resultingObjectObid);
		int cidx = resultingObjectObid.lastIndexOf(":");
		sop("Got last index of : " + cidx);
		String formatedObid = resultingObjectObid.substring(0, cidx);
		sop("Got resulting object obid, substring: " + formatedObid);
		// initialize resulting object URL
		String resultingObjectURL = null;

		try { // begin try, getting the object details page URL

			resultingObjectURL = BSSLCStateReportHelper.getDetailsPageURL((String) gout.getAttributeValue(index, "obid"));

		} // end try, getting the object details page URL
		catch (java.lang.Exception e7) {
			System.out.println("E7 exception in ECNSummaryReportQuery.xml: Gettting object details page URL: ");
			e7.printStackTrace();
		}

		String strWtInstance = "";

		try { // begin try, getting the server url properties

			WTProperties prop = WTProperties.getLocalProperties();
			strWtInstance = prop.getProperty("wt.server.codebase");

		} // end try, getting the server url properties
		catch (Exception e8) {
			System.out.println("E8 Exception in ECNSummaryReportQuery.xml getting system properties ");
			e8.printStackTrace();
		}

		// build the url to the object image
		String objectIconURL = strWtInstance + objectIcon;

		sop("The image URL is: " + objectIconURL);

		// initiate the pv icon url
		String pvIconURL = "";

		// set the representation icon only if the representation obid is not
		// blank
		String representationObidStr = (String) gout.getAttributeValue(index, "representationObid");

		if (!"".equals(representationObidStr)) { // begin if, the representation
			// exist, set path to pv
			// icon

			// set the url to the pv icon path as string
			pvIconURL = strWtInstance + pvIcon;

			sop("The PV Icon path is: " + pvIconURL);

		} else {
			pvIconURL = "";
			// end if, the representation exist, set path to pv icon
		}

		// get the url to the Productview viewable

		try { // begin try, get the url to the Productview viewable

			viewableURL = BSSLCStateReportHelper.getProductViewURL((String) gout.getAttributeValue(index, "obid"));

		} // end try, get the url to the Productview viewable
		catch (Exception e9) { // begin catch

			System.out.println("E9 exception in ECNSummaryReportQuery.xml! In getting viewable URL: ");
			e9.printStackTrace();

		} // end catch

		sop("The viewable URL is: " + viewableURL);
		// zipOption
		element.addAtt(new Att("zipOption", "false"));
		// add image url to the current element
		element.addAtt(new Att("objectIconURL", objectIconURL));
		// add resulting object url to the current element
		element.addAtt(new Att("resultingObjectURL", resultingObjectURL));
		// add resulting object obid to the current element
		element.addAtt(new Att("formatedObid", formatedObid));
		// add productview icon url to the current element
		element.addAtt(new Att("pvIconURL", pvIconURL));
		// add viewable url to the current element
		element.addAtt(new Att("viewableURL", viewableURL));

		return element;
	}

	/** This method will retrieve all the Previous ECNs of an object which are not yet released
	 * @param element
	 * @param list
	 * @param reportName
	 * @return Element
	 */
	public static Element getPreviousUnreleasedECN(Element element, ArrayList<WTChangeOrder2> list, String reportName) {

		String enNumber = " ";



		element.addAtt(new Att("previousVersionUnreleased", "No"));


		if (enNumber != null && !enNumber.equals("")) {
			element.addAtt(new Att("previousVersionUnreleased", "Yes"));

		}

		sop("ECN " + enNumber);
		return element;
	}

	/** This method will compare structure the current part and previous released part
	 * @param gout
	 * @param child
	 * @param element
	 * @param index
	 * @param properties
	 * @param currentPart
	 * @param olderRevisionPart
	 * @param revisedParts
	 * @param bomParts
	 * @return ArrayList<Serializable>
	 */
	public static ArrayList<Serializable> bomCompare(Group gout, Group child, Element element, int index, Properties properties, WTPart currentPart, WTPart olderRevisionPart, String revisedParts,
			String bomParts) {


		WTArrayList compareObjectList = new WTArrayList();
		ArrayList<Serializable> allElements = new ArrayList<Serializable>();
		try {
			compareObjectList.add(olderRevisionPart);
			compareObjectList.add(currentPart);
			com.ptc.core.ocmp.framework.SimpleComparisonSpec compare = com.ptc.core.ocmp.framework.ComparisonSpec.newSimpleInstance(compareObjectList);
			java.util.Set<String> linkstoCompareSet = new java.util.HashSet<String>();
			linkstoCompareSet.add("wt.part.WTPartUsageLink");

			compare.setSelectedLinks(linkstoCompareSet);
			compare.areAttrsToBeConsidered();
			com.ptc.core.ocmp.framework.ComparisonResult cmpResult = com.ptc.core.ocmp.service.ObjComparisonHelper.service.compareDomainObjs(compare);

			Map<String, List<? extends LinkDiffs>> allLink = cmpResult.getLinkDiffsMap();
			Iterator<String> iterator1 = allLink.keySet().iterator();
			String applicationData = "";

			HashMap<WTPartUsageLink, WTPart> currentChildMap = getChilders(currentPart);

			String parentOid = wt.fc.PersistenceHelper.getObjectIdentifier(currentPart).toString();
			sop("<------parentOid--->" + parentOid);
			//System.out.println("currentPart   "+currentPart.getNumber());

			String currPartState = currentPart.getState().toString();
			//System.out.println("currPartState   "+currPartState);

			Element childElement = null;

			while (iterator1.hasNext()) {
				String keyName = (String) iterator1.next();

				java.util.List<? extends LinkDiffs> finalList = (java.util.List<? extends LinkDiffs>) allLink.get(keyName);

				if (finalList == null && revisedParts.equals("true")) {
					//System.out.println("&&&&&&&&&&&&&&RevisedPartsReportName->");
					if(!(currPartState.equalsIgnoreCase("OBSOLETE"))){
						element.addAtt(new Att("RevisedPartsReportName", "Yes"));
						element.addAtt(new Att("BOMReportName", "No"));
					}
				} else if (bomParts.equals("true") && finalList != null) {
					element.addAtt(new Att("BOMReportName", "Yes"));
					element.addAtt(new Att("PARENTPARTOBID", parentOid));
				}

				if (finalList != null && bomParts.equals("true")) {
					for (int i = 0; i < finalList.size(); i++) {
						com.ptc.core.ocmp.framework.impl.DefaultLinkDiffs values = (com.ptc.core.ocmp.framework.impl.DefaultLinkDiffs) finalList.get(i);

						if (values.getLinkRefs() != null) {
							List differentValues1 = values.getLinkRefs();

							sop("values.getLinkRefs()-->" + values.getLinkRefs());

							// wt.fc.ObjectToObjectLink link = null;
							boolean findNO = false;
							boolean change = false;

							if (differentValues1.get(0) == null) {
								applicationData = "ADD";
								wt.fc.ObjectReference obj = (wt.fc.ObjectReference) differentValues1.get(1);
								if (obj.getObject() instanceof wt.part.WTPartUsageLink) {
									WTPartUsageLink link = (wt.part.WTPartUsageLink) obj.getObject();
									sop("<------ADD Objects--->" + currentChildMap.get(link));
									WTPart part = currentChildMap.get(link);

									childElement = new Element();
									try{
										sop("<------Null ADD Objects--->" + part.getNumber() + "_---->" + part.getVersionIdentifier().getValue() + "-->>" + part.getIterationIdentifier().getValue());
										childElement = createChildElements(child, childElement, part, link, parentOid, applicationData, " ");
									}catch(NullPointerException ne)
									{
										System.out.println("Null object");
									}

								}

							} else if (differentValues1.get(1) == null) {
								applicationData = "DEL";
								wt.fc.ObjectReference obj = (wt.fc.ObjectReference) differentValues1.get(0);
								if (obj.getObject() instanceof wt.part.WTPartUsageLink) {
									WTPartUsageLink link = (wt.part.WTPartUsageLink) obj.getObject();
									WTPart part = null;
									WTPartMaster master = (WTPartMaster) link.getUses();
									sop("<------Deleted Objects--->" + master.getNumber());
									QueryResult result = wt.vc.config.ConfigHelper.service.filteredIterationsOf(master, new LatestConfigSpec());
									sop("<------Deleted Objects--->" + result.size());
									while (result.hasMoreElements()) {

										part = (WTPart) result.nextElement();
										sop("<------Delete Objects--->" + part.getNumber() + "_---->" + part.getVersionIdentifier().getValue() + "-->>" + part.getIterationIdentifier().getValue());
									}

									childElement = new Element();
									sop("<------Null Delete Objects--->" + part.getNumber() + "_---->" + part.getVersionIdentifier().getValue() + "-->>" + part.getIterationIdentifier().getValue());
									childElement = createChildElements(child, childElement, part, link, parentOid, applicationData, " ");

								}

							} else if (differentValues1.get(0) != null && differentValues1.get(1) != null) {
								applicationData = "CHG";

								wt.fc.ObjectReference obj = (wt.fc.ObjectReference) differentValues1.get(0);
								if (obj.getObject() instanceof wt.part.WTPartUsageLink) {

									WTPartUsageLink affectedLink = null;
									WTPartUsageLink resultingLink = null;
									double affectedqty = -1;
									double resultingqty = -1;
									String affectedFindNo = "";
									String resultingindNo = "";

									if (obj.getObject() instanceof wt.part.WTPartUsageLink) {

										affectedLink = (wt.part.WTPartUsageLink) obj.getObject();
										affectedqty = affectedLink.getQuantity().getAmount();
										//affectedFindNo = affectedLink.getFindNumber();
										if(affectedLink.getLineNumber() !=  null){

											affectedFindNo = Long.toString(affectedLink.getLineNumber().getValue());
										}
										else if (affectedLink.getFindNumber() != null) {

											affectedFindNo = affectedLink.getFindNumber();
										}
										else if(affectedFindNo == null || affectedFindNo == "") {

											affectedFindNo = "-";
										}
									}
									wt.fc.ObjectReference objRes = (wt.fc.ObjectReference) differentValues1.get(1);
									if (objRes.getObject() instanceof wt.part.WTPartUsageLink) {

										resultingLink = (wt.part.WTPartUsageLink) objRes.getObject();
										resultingqty = ((wt.part.WTPartUsageLink) resultingLink).getQuantity().getAmount();
										//resultingindNo = ((wt.part.WTPartUsageLink) resultingLink).getFindNumber();
										if((resultingLink.getLineNumber()) != null){

											resultingindNo = Long.toString(((wt.part.WTPartUsageLink) resultingLink).getLineNumber().getValue());
										}
										else if (resultingLink.getFindNumber() != null) {

											resultingindNo = ((wt.part.WTPartUsageLink) resultingLink).getFindNumber();

										}
										else if(resultingindNo == null || resultingindNo == "") {

											resultingindNo = "-";
										}

									}
									String infoWas = null;
									if ((affectedqty != resultingqty)) {

										sop("infoWas::::qty::"+infoWas);
										//										infoWas = "QTY-" + affectedqty;
										String qty=Double.toString(affectedqty);
										infoWas = qty;

									}
									if (infoWas == null && !affectedFindNo.equals(resultingindNo)) {


										infoWas = "ITEM-" + affectedFindNo;

									}
									WTPart part = currentChildMap.get(resultingLink);

									sop("<------Null element Change Objects--->");
									childElement = new Element();
									childElement = createChildElements(child, childElement, part, resultingLink, parentOid, applicationData, infoWas);

								}
							}

						}
						try{
							if (childElement != null) {
								sop("final Map-->>" + childElement.getMap());
								//----test code will be removed----------------
								String status=childElement.getValue("STATUS").toString();
								if(null!=status & (!("").equals(status)))
									//-----------------------------
									child.addElement(childElement);

							}
						}catch(NullPointerException ne)
						{
							System.out.println("child element or state is null");
						}


					}

				}

			}


			//child=getItmNumSorted(child);
			//child=getStatusSorted(child);

			allElements.add(element);
			allElements.add(child);



		} catch (ConfigResourceException e) {

			e.printStackTrace();
		} catch (WTException e) {

			e.printStackTrace();
		}catch (Exception e) {

			e.printStackTrace();
		}


		return allElements;
	}

	/** This method will retrieve usagelink and the child components of Parent Part
	 * @param part
	 * @return HashMap<WTPartUsageLink, WTPart>
	 */
	public static HashMap<WTPartUsageLink, WTPart> getChilders(WTPart part)

	{
		//wt.fc.QueryResult result;
		HashMap<WTPartUsageLink, WTPart> map = new HashMap<WTPartUsageLink, WTPart>();

		try {
			//result = wt.part.WTPartHelper.service.getUsesWTParts(part, new wt.vc.config.LatestConfigSpec());
			/*
			 * Start-code modified by Carrier Upgrade Team for deprecated API
			 */
			//result = wt.part.WTPartHelper.service.getUsesWTPartMasters(part);

			//result = ConfigHelper.service.filteredIterationsOf(result, new LatestConfigSpec());
			/*
			 * End-code modified by Carrier Upgrade Team for deprecated API
			 */
			for(wt.fc.QueryResult result = wt.part.WTPartHelper.service.getUsesWTPartMasters(part); result.hasMoreElements();)
			{
				WTPartUsageLink wtPartUsageLinkObj = (WTPartUsageLink)result.nextElement();
				WTPartMaster wtpartmaster = (WTPartMaster)(wtPartUsageLinkObj).getUses();

				//Below line of code is modified for ESR: 27098 "ECN Release Report not matching Windchill change" 
				//Code is Modifed to get latest iteration of  WTPart

				//QueryResult queryresult = VersionControlHelper.service.allVersionsOf(wtpartmaster);
				QueryResult queryresult = ConfigHelper.service.filteredIterationsOf(wtpartmaster, new LatestConfigSpec());
				while(queryresult.hasMoreElements()) 
				{
					WTPart wtpart1 = (WTPart)queryresult.nextElement();
					sop("wtpart1   "+wtpart1.getNumber()+"   line Number "+wtPartUsageLinkObj.getLineNumber());
					map.put(wtPartUsageLinkObj, wtpart1);
				}
			}
			sop("-map-->>" + map.size());
			/*while (result.hasMoreElements()) {

				wt.fc.Persistable partPers[] = (wt.fc.Persistable[]) result.nextElement();
				WTObject object0 = (WTObject) partPers[0];
				WTObject object1 = (WTObject) partPers[1];
				wt.part.WTPartUsageLink link = null;
				WTPart wtPart = null;
				if (object0 instanceof wt.part.WTPartUsageLink) {
					link = (wt.part.WTPartUsageLink) object0;
				}
				if (object1 instanceof WTPart) {
					wtPart = (WTPart) object1;
					map.put(link, wtPart);
				}

			}*/

		} catch (WTException e) {
			e.printStackTrace();
		}
		return map;

	}

	/** This method will retrieve all the child details associated to Part
	 * @param child
	 * @param childElement
	 * @param part
	 * @param link
	 * @param parentOid
	 * @param status
	 * @param infowas
	 * @return Element
	 */
	public static Element createChildElements(Group child, Element childElement, WTPart part, WTPartUsageLink link, String parentOid, String status, String infowas) {



		Integer itmNo=0;
		Integer lineNo =0;
		Integer findNo = 0;
		String itemno="";
		try{
			if (link.getFindNumber() != null ) {
				findNo =Integer.valueOf(link.getFindNumber()).intValue();
				sop("-findNo-->>" + findNo);
			}

			if (link.getLineNumber() != null && findNo==0) {
				lineNo = Integer.valueOf(Long.toString(link.getLineNumber().getValue())).intValue();
				sop("-lineNo-->>" + lineNo);
			}

			if(findNo!=0)
			{
				itmNo=findNo;
			}
			else if(lineNo!=0)
			{
				itmNo=lineNo;
			}
			else
			{
				itmNo=0;
			}

			if(itmNo<10 & itmNo!=0)
				itemno="   "+Integer.toString(itmNo);
			else if(itmNo<100 & itmNo!=0)
				itemno="  "+Integer.toString(itmNo);
			else if(itmNo<1000 & itmNo!=0)
				itemno=" "+Integer.toString(itmNo);
			else if(itmNo==0)
				itemno="-";
			else
				itemno=Integer.toString(itmNo);

		}
		catch(Exception ex)
		{
			System.out.println("find number not an integer");
			itemno=link.getFindNumber();
		}
		double currentQtty = link.getQuantity().getAmount();
		String unit = link.getQuantity().getUnit().getStringValue();
		String source = "";
		try
		{
			unit=unit.substring(unit.lastIndexOf(".")+1,unit.length());

			if (part.getSource() != null) {
				source = part.getSource().getDisplay();
			} else {
				source = " ";
			}


			childElement.addAtt(new Att("CHILDOBID", parentOid));
			childElement.addAtt(new Att("ITEMNO", itemno));
			childElement.addAtt(new Att("COMPONENTNUMBER", part.getNumber()));
			childElement.addAtt(new Att("QTY", Double.toString(currentQtty)));
			childElement.addAtt(new Att("UNIT", unit));
			childElement.addAtt(new Att("CHILDSOURCE", source));
			childElement.addAtt(new Att("COMPONENTNAME", part.getName()));
			childElement.addAtt(new Att("STATUS", status));
			childElement.addAtt(new Att("INFOWAS", infowas));
		}catch(NullPointerException ne)
		{
			System.out.println("Nulll value for part source " );
		}

		return childElement;
	}


	/** This method will retrieve all the Previous ECNs of an object which are not yet released
	 * @param part
	 * @param properties
	 * @param passedECNNumber
	 * @return ArrayList<WTChangeOrder2>
	 */
	//	public static ArrayList<WTChangeOrder2> getPreviousUnreleasedECN(wt.vc.Versioned part, Properties properties, String passedECNNumber) {
	//		ArrayList<WTChangeOrder2> unReleasedECN = new ArrayList<WTChangeOrder2>();
	//		String unReleasedState = properties.getProperty("ext.carrier.wc.cognos.reports.ECNSummaryReport.UnreleasedLCStates");
	//		sop("unReleasedState-->>" + unReleasedState);
	//
	//		try {
	//
	//			wt.series.MultilevelSeries currentSeries = part.getVersionIdentifier().getSeries();
	//
	//			QueryResult allVersions = VersionControlHelper.service.allVersionsOf(part);
	//			while (allVersions.hasMoreElements()) {
	//				WTObject partVersion = (WTObject) allVersions.nextElement();
	//				String previousState = "";
	//				wt.series.MultilevelSeries previousSeries = null;
	//				wt.change2.Changeable2 changeble = null;
	//				if (partVersion instanceof WTDocument) {
	//					WTDocument doc = (WTDocument) partVersion;
	//					previousState = doc.getLifeCycleState().toString();
	//					previousSeries = doc.getVersionIdentifier().getSeries();
	//					changeble = (wt.change2.Changeable2) doc;
	//				} else if (partVersion instanceof EPMDocument) {
	//					EPMDocument doc = (EPMDocument) partVersion;
	//					previousState = doc.getLifeCycleState().toString();
	//					previousSeries = doc.getVersionIdentifier().getSeries();
	//					changeble = (wt.change2.Changeable2) doc;
	//				} else if (partVersion instanceof WTPart) {
	//					WTPart doc = (WTPart) partVersion;
	//					previousState = doc.getLifeCycleState().toString();
	//					previousSeries = doc.getVersionIdentifier().getSeries();
	//					changeble = (wt.change2.Changeable2) doc;
	//				}
	//
	//				if(previousSeries.lessThan(currentSeries)){
	//					sop("changeble-->>" + changeble);
	//					StringTokenizer lifecycleArgs = new StringTokenizer(unReleasedState, "|");
	//					while (lifecycleArgs.hasMoreTokens()) {
	//						String lifecycle = (String) lifecycleArgs.nextElement();
	//						if (previousState.equals(lifecycle)) {
	//							sop("Lic is Matched");
	//							QueryResult allChangeActivity = wt.change2.ChangeHelper2.service.getAffectingChangeActivities(changeble);
	//							sop("allChangeActivity-->>" + allChangeActivity.size());
	//							while (allChangeActivity.hasMoreElements()) {
	//								WTChangeActivity2 changeActivity = (WTChangeActivity2) allChangeActivity.nextElement();
	//								sop("changeActivity-->>" + changeActivity.getName());
	//								QueryResult allChangeOrder = wt.change2.ChangeHelper2.service.getChangeOrder(changeActivity);
	//								sop("allChangeOrder-->>" + allChangeOrder.size());
	//								while (allChangeOrder.hasMoreElements()) {
	//									WTChangeOrder2 changeOrder = (WTChangeOrder2) allChangeOrder.nextElement();
	//									sop("changeActivity-->>" + changeOrder.getName());
	//									if(changeOrder.getNumber().equalsIgnoreCase(passedECNNumber))
	//									unReleasedECN.add(changeOrder);
	//								}
	//							}
	//						}
	//					}
	//				}
	//			}
	//
	//		} catch (PersistenceException e) {
	//
	//			e.printStackTrace();
	//		} catch (WTException e) {
	//
	//			e.printStackTrace();
	//		}
	//
	//		return unReleasedECN;
	//
	//	}

	/**
	 * Method  will return respective ECN object in arraylist when respective number is passed
	 * @param ecnNumber
	 * @return
	 * @throws WTException
	 */
	public static ArrayList<WTChangeOrder2> getECN(String ecnNumber) throws WTException
	{
		ArrayList<WTChangeOrder2> currentECN = new ArrayList<WTChangeOrder2>();

		QuerySpec qspecEcn=new QuerySpec(WTChangeOrder2.class);
		qspecEcn.appendWhere(new SearchCondition(WTChangeOrder2.class, "master>number", "=", ecnNumber), new int[] { 0 });

		QueryResult qresEcn=PersistenceHelper.manager.find((StatementSpec)qspecEcn);

		while(qresEcn.hasMoreElements())
		{
			WTChangeOrder2 currentEcn=(WTChangeOrder2) qresEcn.nextElement();
			currentECN.add(currentEcn);
		}

		return currentECN;

	}

	/** This method will retrieve Properties link of an object
	 * @param wtObject
	 * @return String
	 */
	public static String getObjPropsLink(WTObject wtObject) {
		String objPropsURL = " ";
		try {
			wt.fc.ReferenceFactory rf = new wt.fc.ReferenceFactory();
			Properties objProps = new Properties();
			objProps.setProperty("action", "ObjProps");
			objProps.setProperty("oid", rf.getReferenceString(wtObject));
			objPropsURL = wt.httpgw.GatewayServletHelper.buildAuthenticatedURL(new wt.httpgw.URLFactory(), "wt.enterprise.URLProcessor", "URLTemplateAction", null, new HashMap(objProps))
			.toExternalForm();
		} catch (WTException e) {
			return " ";
		}
		return objPropsURL;
	}


	/** This method will retrieve all the ECRs associated for the given ECN
	 * @param element
	 * @param order
	 * @return Element
	 */
	public static Element getECRNumbers(Element element, WTChangeOrder2 order)

	{
		QueryResult changeRequestQueryResult;
		try {
			changeRequestQueryResult = ChangeHelper2.service.getChangeRequest(order);

			String enRNumber = "";
			String noHyperLink = "";

			// looping through each change request object
			while (changeRequestQueryResult.hasMoreElements()) {
				WTChangeRequest2 wtChangeRequest = (WTChangeRequest2) changeRequestQueryResult.nextElement();
				String url = getObjPropsLink(wtChangeRequest);
				if (enRNumber != null && enRNumber.equals("")) {
					enRNumber = "<a href='" + url + "'>" + wtChangeRequest.getNumber() + "</a>";
					noHyperLink = "<div style='mso-data-placement:same-cell'>" + wtChangeRequest.getNumber() + "</div>";

				} else {
					enRNumber += "<br><a href='" + url + "'>" + wtChangeRequest.getNumber() + "</a>";
					noHyperLink += "<div style='mso-data-placement:same-cell'>" + wtChangeRequest.getNumber() + "</div>";
				}

			}

			if (enRNumber != null && !enRNumber.equals("")) {
				element.addAtt(new Att("ECRNumbers", enRNumber));
			}
			if (noHyperLink != null && !noHyperLink.equals("")) {
				element.addAtt(new Att("ECRNuumnolink", noHyperLink));
			}

		} catch (ChangeException2 e) {

			e.printStackTrace();
		} catch (WTException e) {

			e.printStackTrace();
		}
		return element;
	}
	// ESR 41652 Adding a CADModel Section
	/** This method will retrieve all the objects for BOM Data, Revised Parts, for CAD Drawings
	 * @param gout
	 * @param Obsolete
	 * @param NewPart
	 * @param RevisedPart
	 * @param BOM
	 * @param RevisedDrawing
	 * @param passedECNNumber
	 * @param obsoletePartsList
	 * @param childGroup
	 * @param compareBOM
	 * @param bomList
	 * @param child
	 * @param CADModel
	 * @return ArrayList
	 */
	public static ArrayList codeFromTask(Group gout, String Obsolete, String NewPart, String RevisedPart,String BOM,String RevisedDrawing,
			String passedECNNumber,ArrayList obsoletePartsList,Group childGroup, ArrayList compareBOM, ArrayList bomList, Group child,String CADModel) throws Exception
			{
		//--------------18031 start------------------------

		WTChangeOrder2 currentECN=getECNObj(passedECNNumber.toUpperCase());
		String ecnState=currentECN.getState().toString();
		String ecnReleasedate=null; 
		Timestamp lastPromotedOnECN=new Timestamp(0000000000);
		Timestamp secondLastPromotedOnECN=new Timestamp(0000000000);
		
		QueryResult res = LifeCycleHelper.service.getHistory(currentECN);
				
		if(ecnState.equalsIgnoreCase("RELEASED") || ecnState.equalsIgnoreCase("RESOLVED") 
				|| ecnState.equalsIgnoreCase("CANCELLED"))
		{
			QueryResult ecnInfoQR=HistoryTablesCommands.maturityHistory(currentECN);
			ecnInfoQR.size();
			if(ecnInfoQR.hasMoreElements())
			{
				//First nextElement() will pick the timestamp for ECN Released state
				MaturityHistoryInfo firstInfo=(MaturityHistoryInfo)ecnInfoQR.nextElement();				 
				lastPromotedOnECN=firstInfo.getPromotedDate().get(0);
				//Second nextElement() will pick the timestamp for ECN Implementation state
				MaturityHistoryInfo secondInfo=(MaturityHistoryInfo)ecnInfoQR.nextElement();	
				secondLastPromotedOnECN=secondInfo.getPromotedDate().get(0);				
				
			}

		}
		//--------------18031 ends-------------------------
		ArrayList allGroups=new ArrayList();
		int childCount=0;
		int i=0;
		Element groupElement=new Element();
		Group newChildGroup = new Group();

		while ( i < gout.getElementCount())
		{
			String changeOid = (String) gout.getAttributeValue(i, "obid");
			CognosReportHelper.sop("******************************************************Start ***********************************"+gout.getElementCount()+"-->>"+i);
			if (changeOid != null)
			{

				changeOid = changeOid.substring(0, changeOid.lastIndexOf(":"));

				wt.fc.ReferenceFactory rf = new wt.fc.ReferenceFactory();
				groupElement = gout.getElementAt(i);
				WTObject object = (WTObject) rf.getReference(changeOid).getObject();
				String reportName="";
				if(object instanceof WTPart)
				{

					WTPart part=(WTPart)object;
					//System.out.println("*****************from CodefromTask   "+part.getNumber()+" "+part.getVersionIdentifier().getValue());
					WTPart previousPart=null;
					boolean reuslt=false;
					wt.fc.QueryResult allVersions = VersionControlHelper.service.allVersionsOf(part);
					wt.series.MultilevelSeries currentSeries = part.getVersionIdentifier().getSeries();
					if(Obsolete.equals("true") || NewPart.equals("true") ||RevisedPart.equals("true") || BOM.equals("true"))
					{

						groupElement= CognosReportHelper.createECNCommonElements(gout,groupElement,i,part,"Obsolete Parts",properties,passedECNNumber);
						reuslt=true;
					}

					CognosReportHelper.sop("The currentSeries series is: " +currentSeries.toString());
					while(allVersions.hasMoreElements())
					{

						WTPart partVersion=(WTPart)allVersions.nextElement();
						//System.out.println("version list ::::"+ partVersion.getNumber()+"___"+partVersion.getVersionIdentifier().getValue());
						String state = partVersion.getState().toString();
						wt.series.MultilevelSeries previousSeries = partVersion.getVersionIdentifier().getSeries();
						CognosReportHelper.sop("The previous series is: " + previousSeries.toString()+"-->>"+state);
						//------read properties-------
						if(previousSeries.lessThan(currentSeries))
						{			
							if(statelist.contains(state))
							{

								previousPart=partVersion;

								break;
							}							

						}
					}
					if(obsoletePartsList.contains(part))
					{
						if( reuslt && Obsolete.equals("true"))
						{
							groupElement.addAtt(new Att("ObsoleteReportName", "Yes"));
						}
						else
						{
							groupElement.addAtt(new Att("ObsoleteReportName", "No"));
						}
					}
					else
					{

						groupElement.addAtt(new Att("ObsoleteReportName", "No"));
					}

					if(previousPart==null)
					{

						if(reuslt && NewPart.equals("true"))
						{
							//------------18031 start-------------------------------
							String partState=part.getState().toString();
							String partVersion = part.getVersionIdentifier().getValue();
							//System.out.println("Parts state is :"+ partState);
							//System.out.println("Parts version is :"+ partVersion);

							if(ecnState.equalsIgnoreCase("RELEASED") || ecnState.equalsIgnoreCase("RESOLVED") 
									|| ecnState.equalsIgnoreCase("CANCELLED"))
							{

								Timestamp lastPromotedOnPart=new Timestamp(00000000);
								if(statelist.contains(partState))
								{									
									QueryResult ecnInfoQR=HistoryTablesCommands.maturityHistory(part);
									int count=0;
									while(ecnInfoQR.hasMoreElements())
									{
										MaturityHistoryInfo infoPart=(MaturityHistoryInfo)ecnInfoQR.nextElement();
										List promotionDates=infoPart.getPromotedDate();
										int size=promotionDates.size();
										//System.out.println("info part version is : "+ ((WTPart)infoPart.getIteration()).getVersionIdentifier().getValue());
										if(!((WTPart)infoPart.getIteration()).getVersionIdentifier().getValue().equalsIgnoreCase( partVersion)){
											continue;
										}
										if(count==0)
										{
											lastPromotedOnPart=(Timestamp) promotionDates.get(0);
											count++;
										}
										for(int p=0;p<size;p++)
										{	  									
											Timestamp templastdate=(Timestamp) promotionDates.get(p);
											if(lastPromotedOnPart.before(templastdate))
											{
												lastPromotedOnPart=templastdate;
											}
										}
									}
									
									// We need to compare the parts released time should be in between ECN Released State and ECN Implementation State.
									
									/*System.out.println("ECN Released state update Timestamp **::::"+lastPromotedOnECN.getTime() + ".......part ::" + part.getNumber()
											+ ".....lastPromotedOnPart :::" +lastPromotedOnPart.getTime()
											+ ".........ECN implementation state update Timestamp ::: " + secondLastPromotedOnECN.getTime());*/
									
									if((lastPromotedOnPart.getTime() >= secondLastPromotedOnECN.getTime()) &&
											(lastPromotedOnPart.getTime() <= lastPromotedOnECN.getTime()))
									{

										groupElement.addAtt(new Att("NewPartsReportName", "Yes"));
									}
									else
									{

										groupElement.addAtt(new Att("NewPartsReportName", "No"));
									}

								} 
								else
								{
									groupElement.addAtt(new Att("NewPartsReportName", "Yes"));
								}

							}
							else
							{
								if(!statelist.contains(partState))
								{
									//System.out.println("inside if");
									groupElement.addAtt(new Att("NewPartsReportName", "Yes"));
								}
								else
								{
									//System.out.println("Inside else");
									groupElement.addAtt(new Att("NewPartsReportName", "No"));
								}
							}
						}
						else
						{
							groupElement.addAtt(new Att("NewPartsReportName", "No"));
						}
						//-----------------18031 ends----------------------------
					}
					else
					{

						groupElement.addAtt(new Att("NewPartsReportName", "No"));
					}

					if(previousPart !=null && !(obsoletePartsList.contains(part)))
					{
						
						if((reuslt && (BOM.equals("true") || RevisedPart.equals("true")) && !("OBSOLETE".equalsIgnoreCase(part.getState().toString()))))
						{
							java.util.Date date= new java.util.Date();

							//System.out.println("***  part ---> "+part.getNumber());
							java.util.ArrayList lsitBom=CognosReportHelper.bomCompare(gout,child,groupElement
									,i,properties,part,previousPart,RevisedPart,BOM);
							//System.out.println("***  Child ---> "+child.getElementCount());
							groupElement=(Element)lsitBom.get(0);
							//childGroup=(Group)lsitBom.get(1);
							//newChildGroup =(Group)lsitBom.get(1);
							List elem = child.getElementList();
							//System.out.println("***  elem list  ---> "+elem);
							for(int x=0;x<child.getElementCount();x++)
							{
								Element newElem = child.getElementAt(x);
								childGroup.addElement(newElem);

							}
							child.removeElements();
							//System.out.println("--------Child Group-------------"+childGroup.getElementCount());
							if(BOM.equals("true") && childGroup !=null && childGroup.getElementCount() > 0)
							{
								//System.out.println("***  childGroup ---> "+childGroup.getElementCount()+"-->>"+i);

								childCount=childGroup.getElementCount();
								String parentOid = wt.fc.PersistenceHelper.getObjectIdentifier(part).toString();
								compareBOM.add(Integer.toString(i));
								//addGroup(childGroup);
								bomList.add(childGroup);
							}
							//--------------remove later-------------------
							else if(BOM.equals("true"))
							{

								childGroup.removeElements();
								bomList.clear();
								child.removeElements();
								compareBOM.clear();
								obsoletePartsList.clear();

							}
							//--------------------remove later------------


						}
						else
						{

							if(!BOM.equals("true"))
							{

								groupElement.addAtt(new Att("BOMReportName", "No"));
							}

							if(!RevisedPart.equals("true"))
							{

								groupElement.addAtt(new Att("RevisedPartsReportName", "No"));
							}
						}

					}

				}
				else
				{

					CognosReportHelper.sop("*********Remving the Cad Parts*********"+gout.getElementCount()+"-->"+(String)gout.getAttributeValue(i,"docType"));
					if(  (!"$$Document".equals((String)gout.getAttributeValue(i,"docType"))) &&  !"CADDRAWING".equals((String)gout.getAttributeValue(i,"docType")) )
					{

						gout.removeElement(groupElement);
						i--;
						CognosReportHelper.sop("*********After Remving the Cad Parts*********"+gout.getElementCount()+"-->"+i);
					}
					else
					{
						java.util.Date date= new java.util.Date();

						if(RevisedDrawing.equals("true"))
						{

							groupElement=CognosReportHelper.createRevisedDrawings(gout,groupElement,i,object,properties,"Revised_Drawings",passedECNNumber);

						}
						//ESR 41652
						if(CADModel.equals("true"))
						{

							groupElement=CognosReportHelper.createCADModel(gout,groupElement,i,object,properties,"CAD_Model",passedECNNumber);

						}

					}

					if((!RevisedDrawing.equals("true"))||(!CADModel.equals("true")))
					{

						groupElement.addAtt(new Att("DrawingsReportName", "No"));
					}
				}
			}

			CognosReportHelper.sop("*************** One data end*****************"+gout.getElementCount()+"-->>"+i+"-->>"+childCount+"-->>"+bomList.size());
			CognosReportHelper.sop("***************After One data end*****************"+gout.getElementCount()+"-->>"+i+"-->>"+childCount+"-->>"+bomList.size());

			i++;
		}

		allGroups.add(gout);
		allGroups.add(Obsolete);
		allGroups.add(NewPart);
		allGroups.add(RevisedPart);
		allGroups.add(BOM);
		allGroups.add(RevisedDrawing);
		allGroups.add(passedECNNumber);
		allGroups.add(obsoletePartsList);
		allGroups.add(childGroup);
		allGroups.add(compareBOM);
		allGroups.add(bomList);
		allGroups.add(child);
		allGroups.add(CADModel);
		allGroups.add(groupElement);

		//System.out.println("------------------All groups-----------------------"+allGroups);
		//System.out.println("------------------All groups_bomlist-----------------------"+allGroups.get(10));
		//System.out.println("------------------All groups.child-----------------------"+allGroups.get(11));
		return allGroups;
			}
	/** this method will group out Objects which has its elements with Status, others will be removed from the group.
	 * @param NewBom
	 * @return Group
	 */
	public static Group editBomList(Group NewBom)
	{
		int size=NewBom.getElementCount();
		for(int h=0;h<NewBom.getElementCount();h++)
		{
			Element newElem=NewBom.getElementAt(h);
			try{
				String status=newElem.getValue("STATUS").toString();
				CognosReportHelper.sop("status :::::::________________" + status);
				if(null==status || (("").equals(status.trim())))
				{NewBom.removeElement(newElem);

				h--;}
			}catch(NullPointerException ex)
			{
				System.out.println("no status defined");
			}

		}

		return NewBom;
	}

	/** this method will group out all the resulting Objects.
	 * @param activities
	 * @return Group
	 */
	@SuppressWarnings("deprecation")
	public static Group getAllChangeables(Group activities) throws Exception
	{
		Group changeables=new Group();
		int size=activities.getElementCount();
		//System.out.println("----------------size---------"+size);
		ArrayList aList=new ArrayList();
		ArrayList bList=new ArrayList();

		HashSet set=new HashSet();
		Changeable2 changeable;
		wt.fc.ReferenceFactory rf = new wt.fc.ReferenceFactory();
		HashMap latestObject=new HashMap();


		for(int l=0;l<size;l++)
		{
			String projectOid = (String) activities.getAttributeValue(l, "obid");
			//System.out.println("------------projectOid-----------"+projectOid);
			projectOid = projectOid.substring(0, projectOid.lastIndexOf(":"));
			//System.out.println("------------projectOid-----------"+projectOid);
			wt.change2.WTChangeActivity2 changeActivity = (wt.change2.WTChangeActivity2) rf.getReference(projectOid).getObject();
			//System.out.println("------------changeActivity-----------"+changeActivity);
			activityList.add(changeActivity);
			QueryResult changeableQueryResult = ChangeHelper2.service.getChangeablesAfter(changeActivity);
			//System.out.println("------------changeableQueryResult-----------"+changeableQueryResult.size());

			int count=0;
			boolean toB=true;
			while(changeableQueryResult.hasMoreElements())
			{
				Changeable2 chgObj = (Changeable2)changeableQueryResult.nextElement();
				aList.add(chgObj);
				bList.add(chgObj);
			}




			//			for(int m=0;m<aList.size();m++)
			//			{
			//				set.add((Changeable2)aList.get(m));
			//			}

		}

		Vector ObjCompleted = new Vector();
		if(bList.size()>0)
			for(int o=0;o<bList.size();o++)
			{
				changeable=(Changeable2) bList.get(o);
				/*
				 * Start-code modified by Carrier Upgrade Team for deprecated API
				 */
				WTObject changeable1=(WTObject)changeable;
				try{
					//if((o==0) || (!ObjCompleted.contains((changeable1.getDisplayType()).toString().substring(0,(changeable1.getDisplayType()).toString().indexOf(","))))){
					if( (o==0) || (!ObjCompleted.contains(changeable1))){
						/*
						 * Start-code modified by Carrier Upgrade Team for deprecated API
						 */
						for(int k=0;k<aList.size();k++)
						{

							Changeable2 chnested=(Changeable2) aList.get(k);

							if((changeable instanceof WTPart) & (chnested instanceof WTPart))
							{
								//latestObject.put(((WTPart)changeable).getNumber()+"part", changeable);

								if(((WTPart)changeable).getNumber().equalsIgnoreCase(((WTPart)chnested).getNumber()))
								{

									MultilevelSeries chnestedObjectRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTPart)chnested).getVersionIdentifier().getValue());
									MultilevelSeries changeableObjectLoadedRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTPart)changeable).getVersionIdentifier().getValue());


									//						Timestamp tmChangeable=((WTPart)changeable).getCreateTimestamp();
									//						Timestamp tmchnested=((WTPart)chnested).getCreateTimestamp();

									if( (chnestedObjectRevision.greaterThan(changeableObjectLoadedRevision)))
									{
										if(null==latestObject.get(((WTPart)changeable).getNumber()+"part"))
										{latestObject.put(((WTPart)changeable).getNumber()+"part", chnested);}

										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTPart)latestObject.get(((WTPart)changeable).getNumber()+"part")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTPart)chnested).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((WTPart)changeable).getNumber()+"part", chnested);

											}

										}


									}
									else
									{
										if(null==(latestObject.get(((WTPart)changeable).getNumber()+"part")))
										{latestObject.put(((WTPart)changeable).getNumber()+"part", changeable);
										}
										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTPart)latestObject.get(((WTPart)changeable).getNumber()+"part")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTPart)changeable).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((WTPart)changeable).getNumber()+"part", changeable);

											}
										}
									}

								}else{
									if(null==latestObject.get(((WTPart)changeable).getNumber()+"part"))
										latestObject.put(((WTPart)changeable).getNumber()+"part", changeable);
								}

							}
							if((changeable instanceof WTDocument) & (chnested instanceof WTDocument))
							{
								//latestObject.put(((WTDocument)changeable).getNumber()+"doc", changeable);
								if(((WTDocument)changeable).getNumber().equalsIgnoreCase(((WTDocument)chnested).getNumber()))
								{
									MultilevelSeries chnestedObjectRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTDocument)chnested).getVersionIdentifier().getValue());
									MultilevelSeries changeableObjectLoadedRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTDocument)changeable).getVersionIdentifier().getValue());


									//						Timestamp tmChangeable=((WTPart)changeable).getCreateTimestamp();
									//						Timestamp tmchnested=((WTPart)chnested).getCreateTimestamp();

									if( (chnestedObjectRevision.greaterThan(changeableObjectLoadedRevision)))
									{
										if(null==latestObject.get(((WTDocument)changeable).getNumber()+"doc"))
										{latestObject.put(((WTDocument)changeable).getNumber()+"doc", chnested);
										//System.out.println("__________1111aaa___________________"+chnested.getIdentity());
										}
										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTDocument)latestObject.get(((WTDocument)changeable).getNumber()+"doc")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTDocument)chnested).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((WTDocument)changeable).getNumber()+"doc", chnested);
												//System.out.println("__________1111bbbb___________________"+chnested.getIdentity());

											}

										}


									}
									else
									{
										if(null==latestObject.get(((WTDocument)changeable).getNumber()+"doc"))
										{latestObject.put(((WTDocument)changeable).getNumber()+"doc", changeable);
										//System.out.println("__________1111___________________"+changeable.getIdentity());
										}
										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTDocument)latestObject.get(((WTDocument)changeable).getNumber()+"doc")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((WTDocument)changeable).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((WTDocument)changeable).getNumber()+"doc", changeable);
												//System.out.println("__________2222___________________"+changeable.getIdentity());

											}
										}
									}

								}
								else{
									if(null==latestObject.get(((WTDocument)changeable).getNumber()+"doc"))
										latestObject.put(((WTDocument)changeable).getNumber()+"doc", changeable);
									//System.out.println("_______________3333______________"+changeable.getIdentity());
								}
							}
							if((changeable instanceof EPMDocument) & (chnested instanceof EPMDocument))
							{
								//latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);
								if(((EPMDocument)changeable).getNumber().equalsIgnoreCase(((EPMDocument)chnested).getNumber()) )
								{
									MultilevelSeries chnestedObjectRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)chnested).getVersionIdentifier().getValue());
									MultilevelSeries changeableObjectLoadedRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)changeable).getVersionIdentifier().getValue());


									//						Timestamp tmChangeable=((WTPart)changeable).getCreateTimestamp();
									//						Timestamp tmchnested=((WTPart)chnested).getCreateTimestamp();

									if( (chnestedObjectRevision.greaterThan(changeableObjectLoadedRevision)))
									{
										if(null==latestObject.get(((EPMDocument)changeable).getNumber()+"epm"))
										{latestObject.put(((EPMDocument)changeable).getNumber()+"epm", chnested);
										}
										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)latestObject.get(((EPMDocument)changeable).getNumber()+"epm")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)chnested).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((EPMDocument)changeable).getNumber()+"epm", chnested);

											}

										}


									}
									else
									{
										if(null==latestObject.get(((EPMDocument)changeable).getNumber()+"epm"))
										{latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);}

										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)latestObject.get(((EPMDocument)changeable).getNumber()+"epm")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)changeable).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);

											}
										}
									}

								}else{
									if(null==latestObject.get(((EPMDocument)changeable).getNumber()+"epm"))
										latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);
								}

							}


						}
						/*
						 * Start-code modified by Carrier Upgrade Team for deprecated API
						 */
						//ObjCompleted.add((changeable1.getDisplayType().toString()).substring(0,(changeable1.getDisplayType().toString()).indexOf(",")));
						ObjCompleted.add(changeable1);
						/*
						 * End-code modified by Carrier Upgrade Team for deprecated API
						 */

					}
				}catch(StringIndexOutOfBoundsException soe)
				{
					System.out.println("String Index Out Of Bounds Exception for " + changeable1.getIdentity());
					soe.printStackTrace();
				}


			}

		Collection<Changeable2> collKeys = latestObject.keySet();
		Iterator<Changeable2> iterKeys = collKeys.iterator();



		while (iterKeys.hasNext())
		{

			//changeable=(Changeable2)iterKeys.next();
			changeable=(Changeable2)latestObject.get(iterKeys.next());
			//System.out.println("inside part:::: _______________"+changeable.getIdentity());
			String drawingDocType = PartsListerUtility.getDocType("DrwDoc");
			boolean addOrNot=true;
			if(changeable instanceof WTPart)
			{


				WTPart chgPart=(WTPart)changeable;

				String obid=BasicWebjectDelegate.getUfid((WTReference)rf.getReference(chgPart));


				CognosReportHelper.sop("inside part:::: _______________"+chgPart.getNumber());
				Element elemPart=new Element();
				elemPart.addAtt(new Att("number",chgPart.getNumber()));
				elemPart.addAtt(new Att("obid",obid));
				elemPart.addAtt(new Att("class","wt.part.WTPart"));
				changeables.addElement(elemPart);


			}
			else if(changeable instanceof WTDocument)
			{

				WTDocument chgPart=(WTDocument)changeable;
				String obid=BasicWebjectDelegate.getUfid((WTReference)rf.getReference(chgPart));
				String docType = com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(chgPart).toString();
				CognosReportHelper.sop("inside doc:::: _______________"+chgPart.getNumber());
				Element elemPart=new Element();
				elemPart.addAtt(new Att("docType","$$Document"));
				elemPart.addAtt(new Att("number",chgPart.getNumber()));
				elemPart.addAtt(new Att("class","WCTYPE|wt.doc.WTDocument|com.utc.carrier.projectlink.utc.DD"));
				elemPart.addAtt(new Att("obid",obid));
				changeables.addElement(elemPart);


			}
			else if(changeable instanceof EPMDocument)
			{

				EPMDocument chgPart=(EPMDocument)changeable;
				if(EPMDocumentType.toEPMDocumentType("CADDRAWING").equals(chgPart.getDocType())){
					String obid=BasicWebjectDelegate.getUfid((WTReference)rf.getReference(chgPart));
					String docType = com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(chgPart).toString();
					CognosReportHelper.sop("inside doc:::: _______________"+chgPart.getNumber());
					Element elemPart=new Element();
					elemPart.addAtt(new Att("docType","CADDRAWING"));
					elemPart.addAtt(new Att("number",chgPart.getNumber()));
					elemPart.addAtt(new Att("class","WCTYPE|wt.epm.EPMDocument"));
					elemPart.addAtt(new Att("obid",obid));
					changeables.addElement(elemPart);

				}
			}

		}

		CognosReportHelper.sop("changeables ______________________"+changeables.getElementCount());
		return changeables;

	}

	/**
	 * Method  will return respective ECR object in arraylist when respective number is passed
	 * @param ecnNumber
	 * @return
	 * @throws WTException
	 */
	public static ArrayList<WTChangeRequest2> getECR(String ecrNumber) throws WTException
	{
		ArrayList<WTChangeRequest2> currentECR = new ArrayList<WTChangeRequest2>();

		QuerySpec qspecEcr=new QuerySpec(WTChangeRequest2.class);
		qspecEcr.appendWhere(new SearchCondition(WTChangeRequest2.class, "master>number", "=", ecrNumber), new int[] { 0 });

		QueryResult qresEcr=PersistenceHelper.manager.find((StatementSpec)qspecEcr);

		while(qresEcr.hasMoreElements())
		{
			WTChangeRequest2 currentEcr=(WTChangeRequest2) qresEcr.nextElement();
			currentECR.add(currentEcr);
		}

		return currentECR;

	}

	// ESR 41652
	/**
		*Method will return element
		*@param gout,element,index,object,properties,reportName,passedECNNumber
		*@return Element
		*@throws WTException
	*/
	
	public static Element createCADModel(Group gout, Element element, int index, WTObject object, Properties properties, String reportName, String passedECNNumber ) throws WTException {
		String objectIcon = "";
		
		if (object instanceof EPMDocument) 
		{
			EPMDocument drawing = (EPMDocument) object;
			wt.epm.EPMDocumentType docType = drawing.getDocType();
		if (!(docType.toString().equals("CADDRAWING")))
			{
				String partName = drawing.getName();
				String version = drawing.getVersionInfo().getIdentifier().getValue();
				String state = State.toState(drawing.getState().toString()).getDisplay();
				element.addAtt(new Att("name", partName));
				element.addAtt(new Att("state.state", state));
				element.addAtt(new Att("versionInfo.identifier.versionId", version));
				//Adding different icons for PRT,ASM
				if ((docType.toString().equals("CADASSEMBLY")))
				objectIcon = properties.getProperty("ext.carrier.wc.cognos.reports.ECNSummaryReport.ASM");
				if ((docType.toString().equals("CADCOMPONENT")))
				objectIcon = properties.getProperty("ext.carrier.wc.cognos.reports.ECNSummaryReport.PRT");
				
				sop("EPM Icon-->>" + objectIcon);
				ArrayList<WTChangeOrder2> changeOderList;
				try {
					changeOderList = getECN(passedECNNumber);
					element = getPreviousUnreleasedECN(element, changeOderList, reportName);
					element.addAtt(new Att("DrawingsReportName", "Yes"));
					element = createViewables(gout, element, index, properties, objectIcon, reportName);
				} catch (Exception e) {
					
					e.printStackTrace();
				}
			}
		}
		return element;
	}

	// ESR 41652
	/**
		*Method will return changeables
		*@param activities
		*@return Group
		*@throws WTException
	*/
	@SuppressWarnings("deprecation")
	public static Group getCADChangeables(Group activities) throws Exception
	{
		Group changeables=new Group();
		int size=activities.getElementCount();
		//System.out.println("----------------size---------"+size);
		ArrayList aList=new ArrayList();
		ArrayList bList=new ArrayList();
		HashSet set=new HashSet();
		Changeable2 changeable;
		wt.fc.ReferenceFactory rf = new wt.fc.ReferenceFactory();
		HashMap latestObject=new HashMap();
		for(int l=0;l<size;l++)
		{
			String projectOid = (String) activities.getAttributeValue(l, "obid");
			//System.out.println("------------projectOid-----------"+projectOid);
			projectOid = projectOid.substring(0, projectOid.lastIndexOf(":"));
			//System.out.println("------------projectOid-----------"+projectOid);
			wt.change2.WTChangeActivity2 changeActivity = (wt.change2.WTChangeActivity2) rf.getReference(projectOid).getObject();
			//System.out.println("------------changeActivity-----------"+changeActivity);
			activityList.add(changeActivity);
			QueryResult changeableQueryResult = ChangeHelper2.service.getChangeablesAfter(changeActivity);
			//System.out.println("------------changeableQueryResult-----------"+changeableQueryResult.size());
			int count=0;
			boolean toB=true;
			while(changeableQueryResult.hasMoreElements())
			{
				Changeable2 chgObj = (Changeable2)changeableQueryResult.nextElement();
				aList.add(chgObj);
				bList.add(chgObj);
			}

		}

		Vector ObjCompleted = new Vector();
		if(bList.size()>0)
			for(int o=0;o<bList.size();o++)
			{
				changeable=(Changeable2) bList.get(o);
				WTObject changeable1=(WTObject)changeable;
				try{
					if( (o==0) || (!ObjCompleted.contains(changeable1))){
					  for(int k=0;k<aList.size();k++)
						{
							Changeable2 chnested=(Changeable2) aList.get(k);
							if((changeable instanceof EPMDocument) & (chnested instanceof EPMDocument))
							{
								if(((EPMDocument)changeable).getNumber().equalsIgnoreCase(((EPMDocument)chnested).getNumber()) )
								{
									MultilevelSeries chnestedObjectRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)chnested).getVersionIdentifier().getValue());
									MultilevelSeries changeableObjectLoadedRevision = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)changeable).getVersionIdentifier().getValue());
									if( (chnestedObjectRevision.greaterThan(changeableObjectLoadedRevision)))
									{
										if(null==latestObject.get(((EPMDocument)changeable).getNumber()+"epm"))
										{latestObject.put(((EPMDocument)changeable).getNumber()+"epm", chnested);
										}
										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)latestObject.get(((EPMDocument)changeable).getNumber()+"epm")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)chnested).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((EPMDocument)changeable).getNumber()+"epm", chnested);

											}

										}
									}
									else
									{
										if(null==latestObject.get(((EPMDocument)changeable).getNumber()+"epm"))
										{latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);}

										else
										{
											if(!(MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)latestObject.get(((EPMDocument)changeable).getNumber()+"epm")).getVersionIdentifier().getValue()).greaterThan(
													MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", ((EPMDocument)changeable).getVersionIdentifier().getValue()))))
											{
												latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);

											}
										}
									}

								}else{
									if(null==latestObject.get(((EPMDocument)changeable).getNumber()+"epm"))
										latestObject.put(((EPMDocument)changeable).getNumber()+"epm", changeable);
								}

							}
						}
						ObjCompleted.add(changeable1);
					}
				}catch(StringIndexOutOfBoundsException soe)
				{
					System.out.println("String Index Out Of Bounds Exception for " + changeable1.getIdentity());
					soe.printStackTrace();
				}
	}
		Collection<Changeable2> collKeys = latestObject.keySet();
		Iterator<Changeable2> iterKeys = collKeys.iterator();
		while (iterKeys.hasNext())
		{
			changeable=(Changeable2)latestObject.get(iterKeys.next());
			String drawingDocType = PartsListerUtility.getDocType("DrwDoc");
			boolean addOrNot=true;
			if(changeable instanceof EPMDocument)
			{
				EPMDocument chgPart=(EPMDocument)changeable;
					String obid=BasicWebjectDelegate.getUfid((WTReference)rf.getReference(chgPart));
					String docType = com.ptc.core.meta.server.TypeIdentifierUtility.getTypeIdentifier(chgPart).toString();
					CognosReportHelper.sop("inside doc:::: _______________"+chgPart.getNumber());
					Element elemPart=new Element();
					elemPart.addAtt(new Att("docType","CADDRAWING"));
					elemPart.addAtt(new Att("number",chgPart.getNumber()));
					elemPart.addAtt(new Att("class","WCTYPE|wt.epm.EPMDocument"));
					elemPart.addAtt(new Att("obid",obid));
					changeables.addElement(elemPart);
			}
		}
		CognosReportHelper.sop("CADchangeables ______________________"+changeables.getElementCount());
		return changeables;

	}
	
}