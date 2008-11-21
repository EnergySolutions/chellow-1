/*
 
 Copyright 2005, 2008 Meniscus Systems Ltd
 
 This file is part of Chellow.

 Chellow is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Chellow is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Chellow; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

 */

package net.sf.chellow.billing;

import java.util.Date;
import java.util.List;

import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.NotFoundException;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.monad.XmlDescriber;
import net.sf.chellow.monad.XmlTree;
import net.sf.chellow.monad.types.MonadDate;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.physical.HhEndDate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings("serial")
public class DsoContracts implements Urlable, XmlDescriber {
	public static final UriPathElement URI_ID;

	static {
		try {
			URI_ID = new UriPathElement("services");
		} catch (HttpException e) {
			throw new RuntimeException(e);
		}
	}

	private Dso dso;

	public DsoContracts(Dso dso) {
		this.dso = dso;
	}

	public UriPathElement getUrlId() {
		return URI_ID;
	}

	public MonadUri getUri() throws HttpException {
		return dso.getUri().resolve(getUrlId()).append("/");
	}

	public void httpPost(Invocation inv) throws HttpException {
		String name = inv.getString("name");
		Date startDate = inv.getDate("start-date");
		String chargeScript = inv.getString("charge-script");
		if (!inv.isValid()) {
			throw new UserException(document());
		}
		Date finishDate = null;
		if (inv.hasParameter("has-finished")) {
			finishDate = inv.getDate("finish-date");
		}
		DsoContract contract = dso.insertContract(name, HhEndDate
				.roundDown(startDate), finishDate == null ? null : HhEndDate
				.roundDown(finishDate), chargeScript);
		Hiber.commit();
		inv.sendCreated(document(), contract.getUri());
	}

	@SuppressWarnings("unchecked")
	private Document document() throws HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = doc.getDocumentElement();
		Element servicesElement = toXml(doc);
		source.appendChild(servicesElement);
		servicesElement.appendChild(dso.toXml(doc));
		for (DsoContract service : (List<DsoContract>) Hiber
				.session()
				.createQuery(
						"from DsoService service where service.dso = :dso order by service.finishRateScript.finishDate.date desc")
				.setEntity("dso", dso).list()) {
			servicesElement.appendChild(service.toXml(doc));
		}
		source.appendChild(MonadDate.getMonthsXml(doc));
		source.appendChild(MonadDate.getDaysXml(doc));
		source.appendChild(new MonadDate().toXml(doc));
		return doc;
	}

	public void httpGet(Invocation inv) throws HttpException {
		inv.sendOk(document());
	}

	public DsoContract getChild(UriPathElement uriId) throws HttpException {
		DsoContract service = (DsoContract) Hiber
				.session()
				.createQuery(
						"from DsoService service where service.dso = :dso and service.id = :serviceId")
				.setEntity("dso", dso).setLong("serviceId",
						Long.parseLong(uriId.getString())).uniqueResult();
		if (service == null) {
			throw new NotFoundException();
		}
		return service;
	}

	public void httpDelete(Invocation inv) throws HttpException {
		// TODO Auto-generated method stub

	}

	public Element toXml(Document doc) throws HttpException {
		Element contractsElement = doc.createElement("dso-services");
		return contractsElement;
	}

	public Node toXml(Document doc, XmlTree tree) throws HttpException {
		// TODO Auto-generated method stub
		return null;
	}
}