from net.sf.chellow.monad import Hiber, XmlTree
from net.sf.chellow.billing import Supplier

supplier_id = inv.getLong('supplier-id')
supplier = organization.getSupplier(supplier_id)
accounts_element = doc.createElement('accounts')
source.appendChild(accounts_element)
accounts_element.appendChild(supplier.toXml(doc))
for account in Hiber.session().createQuery("from Account account where account.provider.id = :supplierId order by account.reference").setLong("supplierId", supplier.getId()).list():
    account_element = account.toXml(doc)
    accounts_element.appendChild(account_element)
accounts_element.appendChild(account.toXml(doc));
source.appendChild(organization.toXml(doc))