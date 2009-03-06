package net.sf.chellow.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.InternalException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.physical.EntityList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Reports extends EntityList {
	public static final UriPathElement URI_ID;

	static {
		try {
			URI_ID = new UriPathElement("reports");
		} catch (HttpException e) {
			throw new RuntimeException(e);
		}
	}

	public Reports() {
	}

	/*
	 * @SuppressWarnings("unchecked") private Map<Long, Report> getReports()
	 * throws HttpException { SortedMap<Long, Report> reports = new TreeMap<Long,
	 * Report>(); if (Monad.getConfigDir() != null) { File reportsPath = new
	 * File(Monad.getConfigDir().toString() + getUri().toString().replace("/",
	 * File.separator)); File[] fileListing = reportsPath.listFiles(); if
	 * (fileListing != null) { for (File file : fileListing) { if
	 * (file.isDirectory() && !file.getName().equals("default")) { Long id = new
	 * Long(Long.parseLong(file.getName())); MonadUri uri = new
	 * MonadUri(reportsPath.toString() .substring(
	 * Monad.getConfigDir().toString() .length()).replace("\\", "/") + "/" +
	 * file.getName() + "/"); reports.put(id, new Report(this, id, uri)); } } }
	 * reportsPath = new File(Monad.getConfigDir().toString() +
	 * Chellow.ORGANIZATIONS_INSTANCE.getUri().toString() .replace("/",
	 * File.separator) + "default" + File.separator + "reports"); fileListing =
	 * reportsPath.listFiles(); if (fileListing != null) { for (File file :
	 * fileListing) { if (file.isDirectory() &&
	 * !file.getName().equals("default")) { Long id = new
	 * Long(Long.parseLong(file.getName())); MonadUri uri = new
	 * MonadUri(reportsPath.toString() .substring(
	 * Monad.getConfigDir().toString() .length()).replace("\\", "/") + "/" +
	 * file.getName() + "/"); reports.put(id, new Report(this, id, uri)); } } } }
	 * Set<String> allPaths = new HashSet<String>(); Set<String> paths =
	 * Monad.getContext().getResourcePaths( Monad.getConfigPrefix() +
	 * getUri().toString()); if (paths != null) { allPaths.addAll(paths); }
	 * paths = Monad.getContext().getResourcePaths( Monad.getConfigPrefix() +
	 * Chellow.ORGANIZATIONS_INSTANCE.getUri().toString() + "default/reports/");
	 * allPaths.addAll(paths); for (String path : allPaths) { if
	 * (path.endsWith("/") && !path.endsWith("/default/")) { String idPath =
	 * path.substring(0, path.length() - 1); Long id = new
	 * Long(Long.parseLong(idPath.substring(idPath .lastIndexOf("/") + 1,
	 * idPath.length()))); MonadUri reportUri = new
	 * MonadUri(path.substring(Monad .getConfigPrefix().length()));
	 * reports.put(id, new Report(this, id, reportUri)); } } return reports; }
	 */

	@SuppressWarnings("unchecked")
	private Document document() throws HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = doc.getDocumentElement();
		Element reportsElement = toXml(doc);

		source.appendChild(reportsElement);
		for (Report report : (List<Report>) Hiber.session().createQuery(
				"from Report report order by report.name").list()) {
			reportsElement.appendChild(report.toXml(doc));
		}
		return doc;
	}

	public UriPathElement getUriId() {
		return URI_ID;
	}

	public MonadUri getUri() throws HttpException {
		return Chellow.getUrlableRoot().getUri().resolve(URI_ID).append("/");
	}

	@SuppressWarnings("unchecked")
	public void httpGet(Invocation inv) throws HttpException {
		if (inv.hasParameter("view")) {
			inv.getResponse().setStatus(HttpServletResponse.SC_OK);
			inv.getResponse().setContentType("text/plain");
			inv.getResponse().setHeader("Content-Disposition",
					"filename=reports.xml;");
			PrintWriter pw = null;
			try {
				pw = inv.getResponse().getWriter();
			} catch (IOException e) {
				throw new InternalException(e);
			}
			pw.println("<?xml version=\"1.0\"?>");
			pw.println("<csv>");
			pw.println("  <line>");
			pw.println("    <value>action</value>");
			pw.println("    <value>type</value>");
			pw.println("  </line>");
			pw.flush();
			for (Report report : (List<Report>) Hiber
					.session()
					.createQuery(
							"from Report report where report.name like '0 %' order by report.id")
					.list()) {
				pw.println("  <line>");
				pw.println("    <value>insert</value>");
				pw.println("    <value>report</value>");
				pw.println("    <value>" + report.getId() + "</value>");
				pw.println("    <value>" + report.getName() + "</value>");
				pw.println("    <value><![CDATA["
						+ report.getScript().replace("<![CDATA[",
								"&lt;![CDATA[").replace("]]>", "]]&gt;")
						+ "]]></value>");
				pw.print("    <value><![CDATA[");
				if (report.getTemplate() != null) {
					pw.print(report.getTemplate().replace("<![CDATA[",
							"&lt;![CDATA[").replace("]]>", "]]&gt;"));
				}
				pw.println("]]></value>");
				pw.println("  </line>");
			}
			pw.println("</csv>");
			pw.close();
		} else {
			inv.sendOk(document());
		}
	}

	public void httpPost(Invocation inv) throws HttpException {
		String name = inv.getString("name");
		try {
			Report report = Report.insertReport(null, name, "", null);
			Hiber.commit();
			inv.sendCreated(document(), report.getUri());
		} catch (HttpException e) {
			e.setDocument(document());
			throw e;
		}
	}

	public Urlable getChild(UriPathElement uriId) throws HttpException {
		return Report.getReport(Long.parseLong(uriId.toString()));
	}

	public Element toXml(Document doc) throws HttpException {
		return doc.createElement("reports");
	}
}
