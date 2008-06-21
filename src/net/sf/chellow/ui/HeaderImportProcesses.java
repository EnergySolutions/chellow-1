package net.sf.chellow.ui;

import java.util.HashMap;
import java.util.Map;

import net.sf.chellow.monad.DeployerException;
import net.sf.chellow.monad.DesignerException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.InternalException;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.monad.XmlDescriber;
import net.sf.chellow.monad.XmlTree;

import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.physical.Organization;

import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class HeaderImportProcesses implements Urlable, XmlDescriber {
	public static final UriPathElement URI_ID;

	private static long processSerial = 0;

	public static final Map<Long, Map<Long, HeaderImportProcess>> processes = new HashMap<Long, Map<Long, HeaderImportProcess>>();

	static {
		try {
			URI_ID = new UriPathElement("header-data-imports");
		} catch (HttpException e) {
			throw new RuntimeException(e);
		}
	}

	private Organization organization;

	public HeaderImportProcesses(Organization organization) {
		this.organization = organization;
	}

	public UriPathElement getUriId() {
		return URI_ID;
	}

	public MonadUri getUri() throws InternalException, HttpException {
		return organization.getUri().resolve(getUriId()).append("/");
	}

	public void httpGet(Invocation inv) throws DesignerException,
			InternalException, HttpException, DeployerException {
		inv.sendOk(document());
	}

	private Document document() throws InternalException, HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = (Element) doc.getFirstChild();
		Element processesElement = (Element) toXml(doc);
		source.appendChild(processesElement);
		Map<Long, HeaderImportProcess> orgProcesses = processes
				.get(organization.getId());
		if (orgProcesses != null) {
			for (HeaderImportProcess process : orgProcesses.values()) {
				processesElement.appendChild(process.toXml(doc));
			}
		}
		processesElement.appendChild(organization.toXml(doc));
		return doc;
	}

	public void httpPost(Invocation inv) throws HttpException {
		FileItem fileItem = inv.getFileItem("import-file");
		HeaderImportProcess process;

		if (!inv.isValid()) {
			throw new UserException(document());
		}
		try {
			long processId = processSerial++;
			process = new HeaderImportProcess(getUri().resolve(
					new UriPathElement(Long.toString(processId))).append("/"),
					fileItem);
			Map<Long, HeaderImportProcess> orgProcesses = processes
					.get(organization.getId());
			if (orgProcesses == null) {
				orgProcesses = new HashMap<Long, HeaderImportProcess>();
				processes.put(organization.getId(), orgProcesses);
			}
			orgProcesses.put(processId, process);
		} catch (HttpException e) {
			e.setDocument(document());
			throw e;
		}
		process.start();
		inv.sendCreated(document(), process.getUri());
	}

	public Urlable getChild(UriPathElement urlId) throws InternalException,
			HttpException {
		Map<Long, HeaderImportProcess> processMap = processes.get(organization
				.getId());
		if (processMap == null) {
			return null;
		}
		return processMap.get(Long.parseLong(urlId.toString()));
	}

	public void httpDelete(Invocation inv) throws InternalException,
			DesignerException, HttpException, DeployerException {
		// TODO Auto-generated method stub

	}

	public Node toXml(Document doc) throws InternalException, HttpException {
		Element element = doc.createElement("header-import-processes");
		return element;
	}

	public Node toXml(Document doc, XmlTree tree) throws InternalException,
			HttpException {
		// TODO Auto-generated method stub
		return null;
	}
}
