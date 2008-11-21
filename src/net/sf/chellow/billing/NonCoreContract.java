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

import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.InternalException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.NotFoundException;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.monad.XmlTree;
import net.sf.chellow.monad.types.MonadDate;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.physical.HhEndDate;
import net.sf.chellow.physical.MarketRole;
import net.sf.chellow.ui.Chellow;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class NonCoreContract extends Contract {
	static public NonCoreContract insertNonCoreContract(Provider provider,
			String name, HhEndDate startDate, HhEndDate finishDate,
			String chargeScript) throws HttpException {
		NonCoreContract service = new NonCoreContract(provider, name,
				startDate, finishDate, chargeScript);
		Hiber.session().save(service);
		Hiber.session().flush();
		return service;
	}

	private Provider nonCore;

	public NonCoreContract() {
	}

	public NonCoreContract(Provider nonCore, String name, HhEndDate startDate,
			HhEndDate finishDate, String chargeScript) throws HttpException {
		super(name, startDate, finishDate, chargeScript);
		setParty(nonCore);
		internalUpdate(name, chargeScript);
	}

	void setParty(Provider nonCore) {
		this.nonCore = nonCore;
	}

	public Provider getParty() {
		return nonCore;
	}

	@SuppressWarnings("unchecked")
	public void update(String name, String chargeScript) throws HttpException {
		super.update(name, chargeScript);
		Hiber.flush();
	}

	protected void internalUpdate(Provider provider, String name,
			String chargeScript) throws HttpException {
		if (provider.getRole().getCode() != MarketRole.NON_CORE_ROLE) {
			throw new InternalException(
					"The provider must be of type Z for a non-core service.");
		}
		super.internalUpdate(name, chargeScript);
		setParty(provider);
	}

	public boolean equals(Object obj) {
		boolean isEqual = false;
		if (obj instanceof NonCoreContract) {
			NonCoreContract contract = (NonCoreContract) obj;
			isEqual = contract.getId().equals(getId());
		}
		return isEqual;
	}

	public MonadUri getUri() throws HttpException {
		return Chellow.NON_CORE_SERVICES_INSTANCE.getUri().resolve(getUriId())
				.append("/");
	}

	public void delete() throws HttpException {
		super.delete();
		Hiber.session().delete(this);
	}

	public void httpPost(Invocation inv) throws HttpException {
		if (inv.hasParameter("delete")) {
			delete();
			Hiber.commit();
			inv.sendFound(Chellow.NON_CORE_SERVICES_INSTANCE.getUri());
		} else {
			String name = inv.getString("name");
			String chargeScript = inv.getString("charge-script");
			if (!inv.isValid()) {
				throw new UserException(document());
			}
			try {
				update(name, chargeScript);
			} catch (HttpException e) {
				e.setDocument(document());
				throw e;
			}
			Hiber.commit();
			inv.sendOk(document());
		}
	}

	private Document document() throws HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = doc.getDocumentElement();
		source.appendChild(toXml(doc, new XmlTree("provider")));
		source.appendChild(MonadDate.getMonthsXml(doc));
		source.appendChild(MonadDate.getDaysXml(doc));
		source.appendChild(new MonadDate().toXml(doc));
		return doc;
	}

	public void httpGet(Invocation inv) throws HttpException {
		inv.sendOk(document());
	}

	/*
	 * public HhDataImportProcesses getHhDataImportProcessesInstance() { return
	 * new HhDataImportProcesses(this); }
	 */
	public Urlable getChild(UriPathElement uriId) throws HttpException {
		if (RateScripts.URI_ID.equals(uriId)) {
			return new RateScripts(this);
		} else {
			throw new NotFoundException();
		}
	}

	public Element toXml(Document doc) throws HttpException {
		Element element = super.toXml(doc, "non-core-contract");
		return element;
	}
}