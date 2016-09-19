package org.jboss.tools.teiid.ui.bot.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.jboss.reddeer.common.wait.AbstractWait;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.requirements.server.ServerReqState;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.ctab.DefaultCTabItem;
import org.jboss.reddeer.swt.impl.menu.ShellMenu;
import org.jboss.reddeer.swt.impl.styledtext.DefaultStyledText;
import org.jboss.reddeer.swt.impl.text.DefaultText;
import org.jboss.tools.runtime.reddeer.preference.JBossRuntimeDetection;
import org.jboss.tools.teiid.reddeer.connection.ConnectionProfileConstants;
import org.jboss.tools.teiid.reddeer.connection.ResourceFileHelper;
import org.jboss.tools.teiid.reddeer.connection.SimpleHttpClient;
import org.jboss.tools.teiid.reddeer.connection.TeiidJDBCHelper;
import org.jboss.tools.teiid.reddeer.dialog.XmlDocumentBuilderDialog;
import org.jboss.tools.teiid.reddeer.editor.ModelEditor;
import org.jboss.tools.teiid.reddeer.editor.RelationalModelEditor;
import org.jboss.tools.teiid.reddeer.editor.TableEditor;
import org.jboss.tools.teiid.reddeer.editor.TransformationEditor;
import org.jboss.tools.teiid.reddeer.editor.VdbEditor;
import org.jboss.tools.teiid.reddeer.editor.WebServiceModelEditor;
import org.jboss.tools.teiid.reddeer.editor.XmlModelEditor;
import org.jboss.tools.teiid.reddeer.perspective.TeiidPerspective;
import org.jboss.tools.teiid.reddeer.requirement.TeiidServerRequirement;
import org.jboss.tools.teiid.reddeer.requirement.TeiidServerRequirement.TeiidServer;
import org.jboss.tools.teiid.reddeer.view.ModelExplorer;
import org.jboss.tools.teiid.reddeer.view.ProblemsViewEx;
import org.jboss.tools.teiid.reddeer.view.ServersViewExt;
import org.jboss.tools.teiid.reddeer.wizard.newWizard.MetadataModelWizard;
import org.jboss.tools.teiid.reddeer.wizard.newWizard.VdbWizard;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author skaleta
 * tested features:
 * - create WebService Model from: WSDL file, XML Document, Relational Model
 * - create and fulfill operations
 * - generate, deploy and test SOAP WAR 
 */
@RunWith(RedDeerSuite.class)
@OpenPerspective(TeiidPerspective.class)
@TeiidServer(state = ServerReqState.RUNNING, connectionProfiles = {ConnectionProfileConstants.ORACLE_11G_PRODUCTS})
public class WebServiceCreationTest {
	private static final String PROJECT_NAME = "WsCreationProject";
	private static final String WS_MODEL = "ProductsWs.xmi";
	private static final String INTERFACE_NAME = "ProductInfo";
	private static final String OPERATION_GET_ALL = "getAllProductInfo";
	private static final String OPERATION_GET = "getProductInfo";
	private static final String OPERATION_INSERT = "insertProductInfo";
	private static final String OPERATION_DELETE = "deleteProductInfo";
	private static final String DOCUMENT_PRODUCT = "ProductDocument";
	private static final String DOCUMENT_OK = "OkResultDocument";
	private static final String DOCUMENT_FAILED = "FailedResultDocument";
	
	@InjectRequirement
	private static TeiidServerRequirement teiidServer;
	
	private ModelExplorer modelExplorer;
	private static ResourceFileHelper fileHelper;
	
	@BeforeClass
	public static void setUp() throws Exception {
		fileHelper = new ResourceFileHelper();
		fileHelper.copyFileToServer(new File("resources/flat/WsCreationTest/application-roles.properties").getAbsolutePath(), 
				teiidServer.getServerConfig().getServerBase().getHome() + "/standalone/configuration/application-roles.properties");
		new ServersViewExt().restartServer(teiidServer.getName());
	}
	
	@Before
	public void importProject(){
		modelExplorer = new ModelExplorer();
		modelExplorer.importProject(PROJECT_NAME);
		modelExplorer.refreshProject(PROJECT_NAME);;
		modelExplorer.changeConnectionProfile(ConnectionProfileConstants.ORACLE_11G_PRODUCTS, PROJECT_NAME, "sources", "ProductsSource.xmi");
	}
	
	@After
	public void cleanUp(){
		modelExplorer.deleteAllProjectsSafely();
	}
	
	@Test
	public void testCreationFromWsdl(){
		// 1. Create Web Service Model
		modelExplorer.selectItem(PROJECT_NAME, "web_services");
		MetadataModelWizard.openWizard()
				.setModelName(WS_MODEL.substring(0,10))
				.selectModelClass(MetadataModelWizard.ModelClass.WEBSERVICE)
		        .selectModelType(MetadataModelWizard.ModelType.VIEW)
		        .selectModelBuilder(MetadataModelWizard.ModelBuilder.BUILD_FROM_WSDL_URL)
				.nextPage()
				.setWsdlFileFromWorkspace(PROJECT_NAME, "others", "ProductInfo.wsdl")
				.nextPage()
				.nextPage()
				.nextPage()
				.nextPage()
				.nextPage()
				.finish();
		
		new WebServiceModelEditor(WS_MODEL).save();
		AbstractWait.sleep(TimePeriod.SHORT);
		
		// 2. Define XML documents
		modelExplorer.openModelEditor(PROJECT_NAME, "web_services", "ProductsWsResponses.xmi");
		XmlModelEditor xmlEditor = new XmlModelEditor("ProductsWsResponses.xmi");
		
		xmlEditor.deleteDocument("ProductInfo_getAllProductInfo_getAllProductInfo_Output");
		xmlEditor.renameDocument("ProductInfo_getProductInfo_getProductInfo_Output", DOCUMENT_PRODUCT);
		xmlEditor.renameDocument("ProductInfo_deleteProductInfo_deleteProductInfo_Output", DOCUMENT_OK);
		xmlEditor.renameDocument("ProductInfo_insertProductInfo_insertProductInfo_Output", DOCUMENT_FAILED);
		
		xmlEditor.openDocument(DOCUMENT_PRODUCT);
		xmlEditor.openMappingClass("ProductOutput_Instance");
		TransformationEditor transformationEditor = xmlEditor.openTransformationEditor();
		transformationEditor.insertAndValidateSql("SELECT * FROM ProductsView.ProductInfo");
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		
		xmlEditor.openDocument(DOCUMENT_OK);
		xmlEditor.openMappingClass("ResultOutput");
		transformationEditor = xmlEditor.openTransformationEditor();
		transformationEditor.insertAndValidateSql("SELECT 'Operation Successful!' AS results");
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		
		xmlEditor.openDocument(DOCUMENT_FAILED);
		xmlEditor.openMappingClass("ResultOutput");
		transformationEditor = xmlEditor.openTransformationEditor();
		transformationEditor.insertAndValidateSql("SELECT 'Operation Failed!' AS results");
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		
		AbstractWait.sleep(TimePeriod.SHORT);
		xmlEditor.saveAndClose();
		
		// 3. Define web service operations
		WebServiceModelEditor wsEditor = new WebServiceModelEditor(WS_MODEL);

		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET_ALL, 
				"ProductInfo_getAllProductInfo_getAllProductInfo_Output", DOCUMENT_PRODUCT);
		
		wsEditor.replaceAllTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET, 
				"ProductInfo_getProductInfo_getProductInfo_Output", DOCUMENT_PRODUCT);
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET, 
				"REPLACE_WITH_ELEMENT_OR_COLUMN", "ProductOutput.ProductOutput_Instance.INSTR_ID");		

		wsEditor.setOperationProcedure(INTERFACE_NAME, OPERATION_INSERT, fileHelper.getSql("WebServiceCreationTest/InsertWithDeclarations.sql"));
		wsEditor.replaceAllTextInOperationProcedure(INTERFACE_NAME, OPERATION_INSERT, 
				"XmlDocuments", "ProductsWsResponses");
		
		wsEditor.setOperationProcedure(INTERFACE_NAME, OPERATION_DELETE, fileHelper.getSql("WebServiceCreationTest/DeleteWithDeclarations.sql"));
		wsEditor.replaceAllTextInOperationProcedure(INTERFACE_NAME, OPERATION_DELETE, 
				"XmlDocuments", "ProductsWsResponses");
		
		wsEditor.saveAndClose();
		AbstractWait.sleep(TimePeriod.SHORT);
		
		ProblemsViewEx.checkErrors();

		// 4. Create VDB and deploy 
		String vdbName = "WsWsdlVdb";
		VdbWizard.openVdbWizard()
				.setLocation(PROJECT_NAME)
				.setName(vdbName)
				.addModel(PROJECT_NAME, "web_services", WS_MODEL)
				.finish();
		modelExplorer.deployVdb(PROJECT_NAME, vdbName);

		// 5. Create WAR, deploy, send requests and check responses (HTTP-Basic security)
		String warName = vdbName + "WarHttpBasic";
		modelExplorer.generateWar(true, PROJECT_NAME, vdbName)
				.setVdbJndiName(vdbName)
				.setContextName(warName)
				.setWarFileLocation(modelExplorer.getProjectPath(PROJECT_NAME) + "/others")
				.setHttpBasicSecurity("teiid-security", "products")
				.finish();
		modelExplorer.deployWar(teiidServer, PROJECT_NAME, "others", warName); 
		
		String url = "http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl";
		String request = fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllRequest.xml");
		String response = SimpleHttpClient.postSoapRequest(teiidServer, url, "getAllProductInfo", request);
		String expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllResponse.xml");
		assertEquals(expected, response);
		
		response = SimpleHttpClient.postSoapRequest(url, "getAllProductInfo", request);
		assertNull(response);
		
		// try catch since here - restore DB
		
		request = fileHelper.getXmlNoHeader("WebServiceCreationTest/InsertRequest.xml");
		response = SimpleHttpClient.postSoapRequest(teiidServer, url, "insertProductInfo", request);
		expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseSuccessful.xml");
		assertEquals(expected, response);
		
		response = SimpleHttpClient.postSoapRequest(teiidServer, url, "insertProductInfo", request);
		expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseFailed.xml");
		assertEquals(expected, response);
		
		request = fileHelper.getXmlNoHeader("WebServiceCreationTest/GetRequest.xml");
		response = SimpleHttpClient.postSoapRequest(teiidServer, url, "getProductInfo", request);
		expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/GetResponse.xml");
		assertEquals(expected, response);
		
		request = fileHelper.getXmlNoHeader("WebServiceCreationTest/DeleteRequest.xml");
		response = SimpleHttpClient.postSoapRequest(teiidServer, url, "deleteProductInfo", request);
		expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseSuccessful.xml");
		assertEquals(expected, response);
		
		response = SimpleHttpClient.postSoapRequest(teiidServer, url, "deleteProductInfo", request);
		expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseFailed.xml");
		assertEquals(expected, response);
		
		request = fileHelper.getXmlNoHeader("WebServiceCreationTest/GetRequest.xml");
		response = SimpleHttpClient.postSoapRequest(teiidServer, url, "getProductInfo", request);
		expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseNotFound.xml");
		assertEquals(expected, response);
		
		// 6. Create WAR, deploy, send requests and check responses (None security)
		
		
		
//		DELETE FROM PRODUCTSYMBOLS WHERE INSTR_ID = 'XXX1234';
//		DELETE FROM PRODUCTDATA WHERE INSTR_ID = 'XXX1234';
	}
	
	@Test
	public void testCreationFromXmlDocument(){
		modelExplorer.modelingWebService(true, PROJECT_NAME, "views", "XmlDocuments.xmi", DOCUMENT_PRODUCT)
				.setLocation(PROJECT_NAME, "web_services")
				.setModelName(WS_MODEL.substring(0,10))
				.setInterfaceName(INTERFACE_NAME)
				.setOperationName(OPERATION_GET_ALL)
				.setInputMsgElement(PROJECT_NAME, "schemas", "ProductSchema.xsd", "ProductSchema.xsd", "EmptyInput")
				.setInputMsgName("getAllProductInfo_Input")
				.setOutputMsgName("getAllProductInfo_Output")
				.finish();
		
		new WebServiceModelEditor(WS_MODEL).saveAndClose();
		
		ProblemsViewEx.checkErrors();
		
		String vdbName = "WsXmlVdb";
		VdbWizard.openVdbWizard()
				.setLocation(PROJECT_NAME)
				.setName(vdbName)
				.addModel(PROJECT_NAME, "web_services", WS_MODEL)
				.finish();
		modelExplorer.deployVdb(PROJECT_NAME, vdbName);

		String warName = vdbName + "War";
		modelExplorer.generateWar(true, PROJECT_NAME, vdbName)
				.setVdbJndiName(vdbName)
				.setContextName(warName)
				.setWarFileLocation(modelExplorer.getProjectPath(PROJECT_NAME) + "/others")
				.setHttpBasicSecurity("teiid-security", "products")
				.finish();
		modelExplorer.deployWar(teiidServer, PROJECT_NAME, "others", warName); 
		
		String response = SimpleHttpClient.postSoapRequest(teiidServer, 
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl", 
				"getAllProductInfo", 
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllRequest.xml"));
		String expected = fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllResponse.xml");
		assertEquals(expected, response);
	}

	@Test@Ignore
	public void testCreationFromViewTable(){
		// TODO qenerate WS model
	}
	
	@Test@Ignore
	public void testCreationFromSourceTable(){
		// TODO qenerate WS model
	}
	
	@Test@Ignore
	public void testCreationFromViewProcedure(){
		// TODO to be decided - project doesn't contain view procedure yet
	}
	
	@Test@Ignore
	public void testCreationFromSourceProcedure(){
		// TODO to be decided - project doesn't contain source procedure yet
	}
	
	
	@Test@Ignore
	public void testCreationFromWsdlDep() throws IOException{
		// 1. Create Web Service Model
		modelExplorer.deleteModel(PROJECT_NAME, "views", "XmlModel.xmi");
		modelExplorer.selectItem(PROJECT_NAME, "web_services");
		MetadataModelWizard.openWizard()
				.setModelName(WS_MODEL.substring(0,10))
				.selectModelClass(MetadataModelWizard.ModelClass.WEBSERVICE)
		        .selectModelType(MetadataModelWizard.ModelType.VIEW)
		        .selectModelBuilder(MetadataModelWizard.ModelBuilder.BUILD_FROM_WSDL_URL)
				.nextPage()
				.setWsdlFileFromWorkspace(PROJECT_NAME, "others", "ProductsInfo.wsdl")
				.nextPage()
				.nextPage()
				.nextPage()
				.nextPage()
				.nextPage()
				.finish();
		
		new WebServiceModelEditor(WS_MODEL).saveAndClose();
		AbstractWait.sleep(TimePeriod.SHORT);
		
		// 2. Define XML documents
		modelExplorer.renameModel("XmlModel.xmi", PROJECT_NAME, "web_services", "ProductsWsResponses.xmi");
		modelExplorer.openModelEditor(PROJECT_NAME, "web_services", "XmlModel.xmi");
		XmlModelEditor xmlEditor = new XmlModelEditor("XmlModel.xmi");
		
		xmlEditor.deleteDocument("ProductInfo_getAllProductInfo_getAllProductsInfo_NewOutput");
		new WebServiceModelEditor(WS_MODEL).close();
		xmlEditor.renameDocument("ProductInfo_getProductInfo_getProductsInfo_OutputMsg", DOCUMENT_PRODUCT);
		xmlEditor.renameDocument("ProductInfo_deleteProductInfo_deleteProductsInfo_ResultOutput", DOCUMENT_OK);
		xmlEditor.renameDocument("ProductInfo_insertProductInfo_insertProductsInfo_ResultOutput", DOCUMENT_FAILED);
		
		xmlEditor.openDocument(DOCUMENT_PRODUCT);
		xmlEditor.openMappingClass("ProductOutput_Instance");
		TransformationEditor transformationEditor = xmlEditor.openTransformationEditor();
		transformationEditor.insertAndValidateSql("SELECT * FROM RelationalModel.ProductInfo");
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		
		xmlEditor.openDocument(DOCUMENT_OK);
		xmlEditor.openMappingClass("ResultOutput");
		transformationEditor = xmlEditor.openTransformationEditor();
		transformationEditor.insertAndValidateSql("SELECT 'Operation Successful!' AS results");
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		
		xmlEditor.openDocument(DOCUMENT_FAILED);
		xmlEditor.openMappingClass("ResultOutput");
		transformationEditor = xmlEditor.openTransformationEditor();
		transformationEditor.insertAndValidateSql("SELECT 'Operation Failed!' AS results");
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		
		AbstractWait.sleep(TimePeriod.SHORT);
		xmlEditor.saveAndClose();
		
		// 3. Define web service operations
		modelExplorer.openModelEditor(PROJECT_NAME, "web_services", WS_MODEL);
		WebServiceModelEditor wsEditor = new WebServiceModelEditor(WS_MODEL);
		
		wsEditor.setOperationProcedure(INTERFACE_NAME, OPERATION_GET_ALL, fileHelper.getSql("WebServiceCreationTest/GetAll.sql"));
		
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET,
				"SELECT * FROM XmlModel.ProductInfo_getProductInfo_getProductsInfo_OutputMsg "
				+ "WHERE XmlModel.ProductInfo_getProductInfo_getProductsInfo_OutputMsg.REPLACE_WITH_ELEMENT_OR_COLUMN = VARIABLES.IN_INSTR_ID;",
				fileHelper.getSql("WebServiceCreationTest/Get.sql"));

		wsEditor.setOperationProcedure(INTERFACE_NAME, OPERATION_INSERT, fileHelper.getSql("WebServiceCreationTest/InsertWithDeclarations.sql"));
		
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_DELETE, "SELECT * FROM XmlModel.ProductInfo_deleteProductInfo_deleteProductsInfo_ResultOutput "
				+ "WHERE XmlModel.ProductInfo_deleteProductInfo_deleteProductsInfo_ResultOutput.REPLACE_WITH_ELEMENT_OR_COLUMN = VARIABLES.IN_INSTR_ID;",
				fileHelper.getSql("WebServiceCreationTest/Delete.sql"));		
		
		AbstractWait.sleep(TimePeriod.SHORT);
		wsEditor.saveAndClose();
		
		new ShellMenu("Project", "Clean...").select();
		AbstractWait.sleep(TimePeriod.SHORT);
		new OkButton().click();
		AbstractWait.sleep(TimePeriod.SHORT);
		
		ProblemsViewEx.checkErrors();
		
		// 4. Generate WAR and test it
		generateWarAndTestIt("WsWsdlVdb");
	}
	@Test@Ignore
	public void testCreationFromRelationalModelDep() throws IOException{
		// 1. Create Web Service Model
		modelExplorer.deleteModel(PROJECT_NAME, "views", "XmlModel.xmi");
		modelExplorer.modelingWebService(false, PROJECT_NAME, "views", "RelationalModel.xmi")
				.setLocation(PROJECT_NAME, "web_services")
				.setModelName(WS_MODEL)
				.setInputSchemaName("InputSchema")
				.setOutputSchemaName("OutputSchema")
				.finish();
	
		// 2. Define XML documents
		new WebServiceModelEditor(WS_MODEL).close();
		modelExplorer.renameModel("XmlModel.xmi", PROJECT_NAME, "web_services", "OutputSchema_View.xmi");		
		
		String[] xmlModelPath = new String[]{PROJECT_NAME, "web_services", "XmlModel.xmi"};
		modelExplorer.openModelEditor(xmlModelPath);
		XmlModelEditor xmlEditor = new XmlModelEditor("XmlModel.xmi");
		
		xmlEditor.renameDocument("ProductInfo_OutputView", DOCUMENT_PRODUCT);
		
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.XML_DOCUMENT, xmlModelPath);
		XmlDocumentBuilderDialog.getInstance()
				.setSchema(PROJECT_NAME,"schemas","ProductsSchema.xsd")
				.addElement("putResults : putResultsType")
				.finish();
		AbstractWait.sleep(TimePeriod.getCustom(3));
		
		xmlEditor.openMappingClass("putResults");
		TransformationEditor outputTransfEditor = xmlEditor.openTransformationEditor();
		outputTransfEditor.insertAndValidateSql("SELECT 'Operation Successful!' AS results");
		outputTransfEditor.close();
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		xmlEditor.renameDocument("putResultsDocument", DOCUMENT_OK);
		
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.XML_DOCUMENT, xmlModelPath);
		XmlDocumentBuilderDialog.getInstance()
				.setSchema(PROJECT_NAME,"schemas","ProductsSchema.xsd")
				.addElement("putResults : putResultsType")
				.finish();
		AbstractWait.sleep(TimePeriod.getCustom(3));
		
		xmlEditor.openMappingClass("putResults");
		outputTransfEditor = xmlEditor.openTransformationEditor();
		outputTransfEditor.insertAndValidateSql("SELECT 'Operation Failed!' AS results");
		outputTransfEditor.close();
		xmlEditor.returnToParentDiagram();
		xmlEditor.returnToParentDiagram();
		xmlEditor.renameDocument("putResultsDocument", DOCUMENT_FAILED);
		
		AbstractWait.sleep(TimePeriod.SHORT);
		xmlEditor.saveAndClose();	
		
		//3. Define web service operations
		String wsModelPath = PROJECT_NAME + "/web_services/" + WS_MODEL;
		modelExplorer.renameModelItem(INTERFACE_NAME, (wsModelPath+"/"+"RelationalModel_ProductInfo").split("/"));
		modelExplorer.openModelEditor(PROJECT_NAME, "web_services", WS_MODEL);
		WebServiceModelEditor wsEditor = new WebServiceModelEditor(WS_MODEL);
		
		// 3.1. Define get operation
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET, "RelationalModel_ProductInfo", INTERFACE_NAME);
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET, "ProductInfo_OutputView", DOCUMENT_PRODUCT);
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET, "ProductInfo_OutputView", DOCUMENT_PRODUCT);
		
		// 3.2. Define get all operation
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OPERATION, (wsModelPath+"/"+INTERFACE_NAME).split("/"));
		modelExplorer.renameModelItem(OPERATION_GET_ALL, (wsModelPath+"/"+INTERFACE_NAME+"/NewOperation").split("/"));
		
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.INPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_GET_ALL).split("/") );
		TableEditor tableEditor = wsEditor.openTableEditor();
		tableEditor.openTab(TableEditor.Tabs.INPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_GET_ALL, 
				"ProductsInfo_AllProducts (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		
		wsEditor.setOperationProcedure(INTERFACE_NAME, OPERATION_GET_ALL, fileHelper.getSql("WebServiceCreationTest/GetAll.sql"));
		
		// 3.3. Define insert operation	
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OPERATION, (wsModelPath+"/"+INTERFACE_NAME).split("/") );
		modelExplorer.renameModelItem(OPERATION_INSERT, (wsModelPath+"/"+INTERFACE_NAME+"/NewOperation").split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.INPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_INSERT).split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OUTPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_INSERT).split("/"));
		
		tableEditor = wsEditor.openTableEditor();
		tableEditor.openTab(TableEditor.Tabs.INPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_INSERT, 
				"ProductsInfo_New_Input (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_INSERT, 
				"putResults (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_INSERT, 
				"goodResultsDocument (Path=/WsCreationProject/web_services/XmlModel.xmi)",
				"Misc", "XML Document");
		
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_INSERT, "SELECT * FROM XmlModel.goodResultsDocument;", 
				fileHelper.getSql("WebServiceCreationTest/Insert.sql"));
		
		// 3.4. Define delete operation
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OPERATION, (wsModelPath+"/"+INTERFACE_NAME).split("/"));
		modelExplorer.renameModelItem(OPERATION_DELETE, (wsModelPath+"/"+INTERFACE_NAME+"/NewOperation").split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.INPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_DELETE).split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OUTPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_DELETE).split("/"));
		
		tableEditor = wsEditor.openTableEditor();
		tableEditor.openTab(TableEditor.Tabs.INPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_DELETE, 
				"ProductsInfo_Input (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_DELETE, 
				"putResults (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_DELETE, 
				"goodResultsDocument (Path=/WsCreationProject/web_services/XmlModel.xmi)",
				"Misc", "XML Document");
		
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_DELETE, "SELECT * FROM XmlModel.goodResultsDocument;", 
				fileHelper.getSql("WebServiceCreationTest/Delete.sql"));
		
		AbstractWait.sleep(TimePeriod.SHORT);
		wsEditor.saveAndClose();
		
		ProblemsViewEx.checkErrors();
		
		// 4. Generate WAR and test it
		//generateWarAndTestIt("WsRelVdb");
	}
	
	@Test@Ignore
	public void testCreationFromXmlDocumentDep() throws IOException{
		// 1. Create Web Service Model
		modelExplorer.modelingWebService(true, PROJECT_NAME, "views", "XmlModel.xmi", DOCUMENT_PRODUCT)
				.setLocation(PROJECT_NAME, "web_services")
				.setModelName(WS_MODEL.substring(0,10))
				.setInterfaceName(INTERFACE_NAME)
				.setOperationName(OPERATION_GET_ALL)
				.setInputMsgElement(PROJECT_NAME, "schemas", "ProductsSchema.xsd", "ProductsSchema.xsd", "ProductsInfo_AllProducts")
				.setInputMsgName("Input")
				.setOutputMsgName("Output")
				.finish();
		
		// 2. Define web service operations
		String wsModelPath = PROJECT_NAME + "/web_services/" + WS_MODEL;
		modelExplorer.openModelEditor(wsModelPath.split("/"));
		WebServiceModelEditor wsEditor = new WebServiceModelEditor(WS_MODEL);	
		
		// 2.1. Define get operation
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OPERATION, (wsModelPath+"/"+INTERFACE_NAME).split("/"));
		modelExplorer.renameModelItem(OPERATION_GET, (wsModelPath+"/"+INTERFACE_NAME+"/NewOperation").split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.INPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_GET).split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OUTPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_GET).split("/"));
		
		TableEditor tableEditor = wsEditor.openTableEditor();
		tableEditor.openTab(TableEditor.Tabs.INPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_GET, 
				"ProductsInfo_Input (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_GET, 
				"ProductsInfo_Output (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_GET, 
				"productDocument (Path=/WsCreationProject/views/XmlModel.xmi)",
				"Misc", "XML Document");
		tableEditor.close();
		
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_GET, "SELECT * FROM XmlModel.productDocument;", 
				fileHelper.getSql("WebServiceCreationTest/Get.sql"));	

		// 2.2. Define insert operation
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OPERATION, (wsModelPath+"/"+INTERFACE_NAME).split("/"));
		modelExplorer.renameModelItem(OPERATION_INSERT, (wsModelPath+"/"+INTERFACE_NAME+"/NewOperation").split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.INPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_INSERT).split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OUTPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_INSERT).split("/"));
		
		tableEditor = wsEditor.openTableEditor();
		tableEditor.openTab(TableEditor.Tabs.INPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_INSERT, 
				"ProductsInfo_New_Input (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_INSERT, 
				"putResults (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_INSERT, 
				"goodResultsDocument (Path=/WsCreationProject/views/XmlModel.xmi)",
				"Misc", "XML Document");
		tableEditor.close();
		
		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_INSERT, "SELECT * FROM XmlModel.goodResultsDocument;", 
				fileHelper.getSql("WebServiceCreationTest/Insert.sql"));	
		
		// 2.3. Define delete operation
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OPERATION, (wsModelPath+"/"+INTERFACE_NAME).split("/"));
		modelExplorer.renameModelItem(OPERATION_DELETE, (wsModelPath+"/"+INTERFACE_NAME+"/NewOperation").split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.INPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_DELETE).split("/"));
		modelExplorer.addChildToModelItem(ModelExplorer.ChildType.OUTPUT, (wsModelPath+"/"+INTERFACE_NAME+"/"+OPERATION_DELETE).split("/"));
		
		tableEditor = wsEditor.openTableEditor();
		tableEditor.openTab(TableEditor.Tabs.INPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_DELETE, 
				"ProductsInfo_Input (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_DELETE, 
				"putResults (Path=/WsCreationProject/schemas/ProductsSchema.xsd/ProductsSchema.xsd)",
				"Misc", "Content via Element");
		tableEditor.openTab(TableEditor.Tabs.OUTPUTS);
		tableEditor.setCellTextViaProperties(OPERATION_DELETE, 
				"goodResultsDocument (Path=/WsCreationProject/views/XmlModel.xmi)",
				"Misc", "XML Document");
		tableEditor.close();

		wsEditor.replaceTextInOperationProcedure(INTERFACE_NAME, OPERATION_DELETE, "SELECT * FROM XmlModel.goodResultsDocument;", 
				fileHelper.getSql("WebServiceCreationTest/Delete.sql"));
		
		AbstractWait.sleep(TimePeriod.SHORT);
		wsEditor.saveAndClose();
		
		ProblemsViewEx.checkErrors();
		
		// 3. Generate WAR and test it
		generateWarAndTestIt("WsXmlVdb");
	}
	private void generateWarAndTestIt(String vdbName) throws IOException{
		// 1. Create VDB and deploy it
		VdbWizard.openVdbWizard()
				.setLocation(PROJECT_NAME)
				.setName(vdbName)
				.addModel(PROJECT_NAME, "web_services", WS_MODEL)
				.finish();
		
		AbstractWait.sleep(TimePeriod.SHORT); 
		String translator = VdbEditor.getInstance(vdbName + ".vdb").getTranslatorName("SourceModel.xmi");
		assertEquals("Translator: " + translator, "oracle", translator);
		
		modelExplorer.deployVdb(PROJECT_NAME, vdbName);
		
		// 2. create WAR, deploy, send requests and check responses (HTTPBasic security)
		String warName = vdbName + "HttpBasicWar";
		modelExplorer.generateWar(true, PROJECT_NAME, vdbName)
				.setVdbJndiName(vdbName)
				.setContextName(warName)
				.setWarFileLocation(modelExplorer.getProjectPath(PROJECT_NAME) + "/others")
				.setHttpBasicSecurity("teiid-security", "products")
				.finish();
		
		modelExplorer.deployWar(teiidServer, PROJECT_NAME, "others", warName);

		postRequestHttpBasicSecurity("getAllProductInfo", 
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllRequest.xml"), 
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllResponse.xml"));

		try {
			postRequestNoneSecurity("getAllProductInfo", 
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllRequest.xml"), null);
		} catch (IOException e){
			// expected
		}
		
		try {
			postRequestHttpBasicSecurity("insertProductInfo", 
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/InsertRequest.xml"), 
					fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseSuccessful.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} 
		
		try {
			postRequestHttpBasicSecurity("insertProductInfo", 
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/InsertRequest.xml"), 
					fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseFailed.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} catch (IOException e) {
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new IOException(e);
		}
		
		try {
			postRequestHttpBasicSecurity("getProductInfo",
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/GetRequest.xml"),
					fileHelper.getXmlNoHeader("WebServiceCreationTest/GetResponse.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} catch (IOException e) {
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new IOException(e);
		}
		
		try {
			postRequestHttpBasicSecurity("deleteProductInfo",
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/DeleteRequest.xml"),
					fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseSuccessful.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} catch (IOException e) {
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new IOException(e);
		}
		
		postRequestHttpBasicSecurity("deleteProductInfo",
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
				fileHelper.getXmlNoHeader("WebServiceCreationTest/DeleteRequest.xml"),
				fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseFailed.xml"));
		
		postRequestHttpBasicSecurity("getProductInfo",
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetRequest.xml"),
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetResponseNotFound.xml"));
		
		// 3. create WAR, deploy, send requests and check responses (None security)
		warName = vdbName + "NoneWar";
		modelExplorer.generateWar(true, PROJECT_NAME, vdbName)
				.setVdbJndiName(vdbName)
				.setContextName(warName)
				.setWarFileLocation(modelExplorer.getProjectPath(PROJECT_NAME) + "/others")
				.setNoneSecurity()
				.finish();
		
		modelExplorer.deployWar(teiidServer, PROJECT_NAME, "others", warName);
		
		postRequestNoneSecurity("getAllProductInfo",
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllRequest.xml"), 
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetAllResponse.xml"));

		try {
			postRequestNoneSecurity("insertProductInfo", 
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/InsertRequest.xml"), 
					fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseSuccessful.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		}
		
		try {
			postRequestNoneSecurity("insertProductInfo", 
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/InsertRequest.xml"), 
					fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseFailed.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} catch (IOException e) {
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new IOException(e);
		}
		
		try {
			postRequestNoneSecurity("getProductInfo",
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/GetRequest.xml"),
					fileHelper.getXmlNoHeader("WebServiceCreationTest/GetResponse.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} catch (IOException e) {
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new IOException(e);
		}
		
		try {
			postRequestNoneSecurity("deleteProductInfo",
					"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
					fileHelper.getXmlNoHeader("WebServiceCreationTest/DeleteRequest.xml"),
					fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseSuccessful.xml"));
		} catch (AssertionError e){
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new AssertionError(e);
		} catch (IOException e) {
			System.err.println("PREVIOUSLY INSERTED DATA ARE NOT REMOVED FROM DATABASE!!!");
			throw new IOException(e);
		}
		
		postRequestNoneSecurity("deleteProductInfo",
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
				fileHelper.getXmlNoHeader("WebServiceCreationTest/DeleteRequest.xml"),
				fileHelper.getXmlNoHeader("WebServiceCreationTest/ResponseFailed.xml"));
		
		postRequestNoneSecurity("getProductInfo",
				"http://localhost:8080/" + warName + "/" + INTERFACE_NAME + "?wsdl",
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetRequest.xml"),
				fileHelper.getXmlNoHeader("WebServiceCreationTest/GetResponseNotFound.xml"));		
	}
	private void postRequestHttpBasicSecurity(String soapAction, String uri, String request, String expected) throws IOException{
		String username = teiidServer.getServerConfig().getServerBase().getProperty("teiidUser");
		String password = teiidServer.getServerConfig().getServerBase().getProperty("teiidPassword");
		System.out.println("Using HTTPBasic security with username '" + username + "' and password '" + password + "'");
		String response = new SimpleHttpClient(uri)
				.setBasicAuth(username, password)
				.addHeader("Content-Type", "text/xml; charset=utf-8")
				.addHeader("SOAPAction", soapAction)
				.post(request);
		assertEquals(expected, response);
	}
	private void postRequestNoneSecurity(String soapAction, String uri, String request, String expected) throws IOException{
		String response = new SimpleHttpClient(uri)
					.addHeader("Content-Type", "text/xml; charset=utf-8")
					.addHeader("SOAPAction", soapAction)
					.post(request);
		assertEquals(expected, response);
	}
}
