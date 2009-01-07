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

package net.sf.chellow.physical;

import java.util.List;

import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.ui.Chellow;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UserRoles extends EntityList {
	public static final UriPathElement URI_ID;

	static {
		try {
			URI_ID = new UriPathElement("user-roles");
		} catch (HttpException e) {
			throw new RuntimeException(e);
		}
	}

	public UserRoles() {
	}

	public MonadUri getUri() throws HttpException {
		return Chellow.ROOT_URI.resolve(getUriId()).append("/");
	}

	@SuppressWarnings("unchecked")
	public void httpGet(Invocation inv) throws HttpException {
		inv.sendOk(document());
	}

	@SuppressWarnings("unchecked")
	private Document document() throws HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = doc.getDocumentElement();
		Element rolesElement = toXml(doc);
		source.appendChild(rolesElement);
		for (UserRole role : (List<UserRole>) Hiber.session().createQuery(
				"from UserRole role order by role.code").list()) {
			rolesElement.appendChild(role.toXml(doc));
		}
		return doc;
	}

	public UriPathElement getUriId() {
		return URI_ID;
	}

	public Urlable getChild(UriPathElement uriId) throws HttpException {
			return UserRole.getUserRole(Long.parseLong(uriId.getString()));
	}

	public Element toXml(Document doc) throws HttpException {
		return doc.createElement("user-roles");
	}
}