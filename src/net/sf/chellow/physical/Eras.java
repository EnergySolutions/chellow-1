/*******************************************************************************
 * 
 *  Copyright (c) 2005-2013 Wessex Water Services Limited
 *  
 *  This file is part of Chellow.
 * 
 *  Chellow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Chellow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Chellow.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *******************************************************************************/

package net.sf.chellow.physical;

import java.net.URI;

import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.NotFoundException;
import net.sf.chellow.monad.XmlTree;
import net.sf.chellow.monad.types.MonadDate;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Eras extends EntityList {
	public static final UriPathElement URI_ID;

	static {
		try {
			URI_ID = new UriPathElement("eras");
		} catch (HttpException e) {
			throw new RuntimeException(e);
		}
	}

	private Supply supply;

	Eras(Supply supply) {
		setSupply(supply);
	}

	void setSupply(Supply supply) {
		this.supply = supply;
	}

	public Supply getSupply() {
		return supply;
	}

	public MonadUri getEditUri() throws HttpException {
		return supply.getEditUri().resolve(getUriId()).append("/");
	}

	public UriPathElement getUriId() {
		return URI_ID;
	}

	public void httpGet(Invocation inv) throws HttpException {
		inv.sendOk(document());
	}

	public Document document() throws HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = doc.getDocumentElement();
		Element erasElement = toXml(doc);
		source.appendChild(erasElement);
		erasElement.appendChild(supply.toXml(doc, new XmlTree("gspGroup")));
		for (Era era : supply.getEras()) {
			erasElement
					.appendChild(era.toXml(doc, new XmlTree("mtc").put("pc")));
		}
		source.appendChild(new MonadDate().toXml(doc));
		source.appendChild(MonadDate.getMonthsXml(doc));
		source.appendChild(MonadDate.getDaysXml(doc));
		source.appendChild(MonadDate.getHoursXml(doc));
		source.appendChild(HhStartDate.getHhMinutesXml(doc));
		return doc;
	}

	public Era getChild(UriPathElement uriId) throws HttpException {
		Era era = (Era) Hiber
				.session()
				.createQuery(
						"from Era era where era.supply = :supply and era.id = :eraId")
				.setEntity("supply", supply)
				.setLong("eraId", Long.parseLong(uriId.toString()))
				.uniqueResult();
		if (era == null) {
			throw new NotFoundException("The era cannot be found.");
		}
		return era;
	}

	public Element toXml(Document doc) throws HttpException {
		return doc.createElement("eras");
	}

	@Override
	public URI getViewUri() throws HttpException {
		// TODO Auto-generated method stub
		return null;
	}
}