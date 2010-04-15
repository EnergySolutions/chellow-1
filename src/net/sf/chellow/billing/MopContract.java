/*******************************************************************************
 * 
 *  Copyright (c) 2005, 2009 Wessex Water Services Limited
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

package net.sf.chellow.billing;

import java.util.List;

import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.Invocation;
import net.sf.chellow.monad.MonadUtils;
import net.sf.chellow.monad.Urlable;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.monad.XmlTree;
import net.sf.chellow.monad.types.MonadDate;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.physical.HhStartDate;
import net.sf.chellow.physical.MarketRole;
import net.sf.chellow.physical.Participant;
import net.sf.chellow.physical.SupplyGeneration;
import net.sf.chellow.ui.Chellow;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MopContract extends Contract {
	static public MopContract insertMopContract(Participant participant,
			String name, HhStartDate startDate, HhStartDate finishDate,
			String chargeScript, String rateScript) throws HttpException {
		MopContract existing = findMopContract(name);
		if (existing != null) {
			throw new UserException(
					"There's already a HHDC contract with the name " + name);
		}
		MopContract contract = new MopContract(participant, name, startDate,
				finishDate, chargeScript, rateScript);
		Hiber.session().save(contract);
		Hiber.flush();
		return contract;
	}

	public static MopContract findMopContract(String name) throws HttpException {
		return (MopContract) Hiber.session().createQuery(
				"from MopContract contract where contract.name = :name")
				.setString("name", name).uniqueResult();
	}

	public static MopContract findMopContract(Long id) throws HttpException {
		return (MopContract) Hiber.session().get(MopContract.class, id);
	}

	public static MopContract getMopService(Long id) throws HttpException {
		MopContract contract = (MopContract) Hiber.session().get(
				MopContract.class, id);
		if (contract == null) {
			throw new UserException(
					"There isn't a meter operator contract with that id.");
		}
		return contract;
	}

	private Provider mop;

	public MopContract() {
	}

	public MopContract(Participant participant, String name,
			HhStartDate startDate, HhStartDate finishDate, String chargeScript,
			String rateScript) throws HttpException {
		super(name, startDate, finishDate, chargeScript, rateScript);
		intrinsicUpdate(participant, name, chargeScript);
	}

	private void intrinsicUpdate(Participant participant, String name,
			String chargeScript) throws HttpException {
		super.internalUpdate(name, chargeScript);
		setParty(Provider.getProvider(participant, MarketRole
				.getMarketRole(MarketRole.MOP)));
	}

	public Provider getParty() {
		return mop;
	}

	void setParty(Provider mop) {
		this.mop = mop;
	}

	public boolean equals(Object obj) {
		boolean isEqual = false;
		if (obj instanceof MopContract) {
			MopContract contract = (MopContract) obj;
			isEqual = contract.getId().equals(getId());
		}
		return isEqual;
	}

	public MonadUri getUri() throws HttpException {
		return Chellow.MOP_CONTRACTS_INSTANCE.getUri().resolve(getUriId())
				.append("/");
	}

	@SuppressWarnings("unchecked")
	public void update(Participant participant, String name, String chargeScript)
			throws HttpException {
		intrinsicUpdate(participant, name, chargeScript);
		for (SupplyGeneration generation : (List<SupplyGeneration>) Hiber
				.session()
				.createQuery(
						"from SupplyGeneration generation where generation.mopContract = :mopContract and generation.startDate.date < :startDate or (generation.finishDate.date is not null and (:finishDate is not null or generation.finishDate.date > :finishDate))")
				.setEntity("mopContract", this).setTimestamp("startDate",
						getStartDate().getDate()).setTimestamp(
						"finishDate",
						getFinishDate() == null ? null : getFinishDate()
								.getDate()).list()) {
			throw new UserException(
					"The supply '"
							+ generation.getSupply().getId()
							+ "' has a generation with this contract that covers a time outside this contract.");
		}
	}

	public void httpPost(Invocation inv) throws HttpException {
		Long participantId = inv.getLong("participant-id");
		String name = inv.getString("name");
		String chargeScript = inv.getString("charge-script");
		if (!inv.isValid()) {
			throw new UserException(document());
		}
		chargeScript = chargeScript.replace("\r", "").replace("\t", "    ");
		try {
			update(Participant.getParticipant(participantId), name,
					chargeScript);
		} catch (UserException e) {
			Document doc = document();
			Element source = doc.getDocumentElement();
			source.setAttribute("charge-script", chargeScript);
			e.setDocument(doc);
			throw e;
		}
		Hiber.commit();
		Document doc = document();
		Element source = doc.getDocumentElement();
		source.setAttribute("charge-script", chargeScript);
		inv.sendOk(doc);
	}

	@SuppressWarnings("unchecked")
	protected Document document() throws HttpException {
		Document doc = MonadUtils.newSourceDocument();
		Element source = doc.getDocumentElement();
		source.appendChild(toXml(doc, new XmlTree("party", new XmlTree(
				"participant"))));
		for (Party party : (List<Party>) Hiber
				.session()
				.createQuery(
						"from Party party where party.role.code = :roleCode order by party.participant.code")
				.setCharacter("roleCode", MarketRole.MOP).list()) {
			source.appendChild(party.toXml(doc, new XmlTree("participant")));
		}
		source.appendChild(new MonadDate().toXml(doc));
		source.appendChild(MonadDate.getMonthsXml(doc));
		source.appendChild(MonadDate.getDaysXml(doc));
		return doc;
	}

	public void httpGet(Invocation inv) throws HttpException {
		inv.sendOk(document());
	}

	public void httpDelete(Invocation inv) throws HttpException {
		// TODO Auto-generated method stub

	}

	public String toString() {
		return super.toString() + " " + mop;
	}

	public Urlable getChild(UriPathElement uriId) throws HttpException {
		if (RateScripts.URI_ID.equals(uriId)) {
			return new RateScripts(this);
		} else if (Batches.URI_ID.equals(uriId)) {
			return new Batches(this);
		} else {
			return null;
		}
	}

	public Element toXml(Document doc) throws HttpException {
		Element element = super.toXml(doc, "mop-contract");
		return element;
	}

	@Override
	public String missingBillSnagDescription() {
		return "Missing MOP bill.";
	}
}
